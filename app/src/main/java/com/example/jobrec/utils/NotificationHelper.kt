package com.example.jobrec.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jobrec.JobDetailsActivity
import com.example.jobrec.NotificationsActivity
import com.example.jobrec.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Helper class for handling notifications
 */
class NotificationHelper(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobsListener: ListenerRegistration? = null

    companion object {
        private const val CHANNEL_ID = "job_notifications"
        private const val NOTIFICATION_GROUP = "com.example.jobrec.JOB_NOTIFICATIONS"

        // Notification types
        const val TYPE_NEW_JOB = "new_job"
        const val TYPE_APPLICATION_STATUS = "application_status"
        const val TYPE_NEW_MESSAGE = "new_message"
    }

    /**
     * Initialize notification channels for Android O and above
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Job Notifications"
            val descriptionText = "Notifications for new jobs and application updates"
            // Use high importance to ensure notifications are shown in foreground
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Enable lights and vibration
                enableLights(true)
                enableVibration(true)
                // Set lockscreen visibility
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                // Enable badge
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start listening for new jobs
     */
    fun startJobNotificationsListener() {
        val currentUser = auth.currentUser ?: return

        // Stop any existing listener
        jobsListener?.remove()

        // Get the timestamp of the last notification
        val lastNotificationTime = getLastNotificationTime()

        // Start listening for new jobs - even simpler query without composite index
        jobsListener = db.collection("jobs")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Filter jobs by timestamp in memory
                    val newJobs = snapshot.documents
                        .mapNotNull { it.toObject(com.example.jobrec.Job::class.java) }
                        .filter { job ->
                            // Only process jobs posted after the last notification time
                            job.postedDate.toDate().after(lastNotificationTime)
                        }

                    // Process new jobs
                    newJobs.forEach { job ->
                        // Send notification for new job
                        showJobNotification(job)

                        // Save notification to database
                        saveNotification(
                            title = "New Job Posted",
                            message = "${job.title} at ${job.companyName}",
                            type = TYPE_NEW_JOB,
                            jobId = job.id
                        )
                    }

                    // Update last notification time to the most recent job
                    val mostRecentJob = newJobs.maxByOrNull { it.postedDate.seconds }

                    if (mostRecentJob != null) {
                        saveLastNotificationTime(mostRecentJob.postedDate.toDate())
                    }
                }
            }
    }

    /**
     * Show a notification for a new job
     */
    private fun showJobNotification(job: com.example.jobrec.Job) {
        // Create an intent to open the job details
        val intent = Intent(context, JobDetailsActivity::class.java).apply {
            putExtra("jobId", job.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            job.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification with high priority to ensure it shows in foreground
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_work)
            .setContentTitle("New Job Posted")
            .setContentText("${job.title} at ${job.companyName}")
            // Set high priority to ensure it shows as a heads-up notification
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            // Add these flags to ensure the notification is shown in foreground
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Add category for job notifications
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(job.id.hashCode(), builder.build())
        }
    }

    /**
     * Save a notification to the database
     */
    private fun saveNotification(title: String, message: String, type: String, jobId: String? = null) {
        val notificationRepository = com.example.jobrec.repositories.NotificationRepository(context)

        // Use IO dispatcher for database operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationRepository.saveNotification(title, message, type, jobId)
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    /**
     * Get the timestamp of the last notification
     */
    private fun getLastNotificationTime(): Date {
        val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val lastNotificationTime = sharedPreferences.getLong("lastNotificationTime", 0)
        return if (lastNotificationTime > 0) Date(lastNotificationTime) else Date(0)
    }

    /**
     * Save the timestamp of the last notification
     */
    private fun saveLastNotificationTime(date: Date) {
        val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastNotificationTime", date.time).apply()
    }

    /**
     * Stop listening for new jobs
     */
    fun stopJobNotificationsListener() {
        jobsListener?.remove()
    }
}
