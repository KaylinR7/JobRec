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
class NotificationHelper(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobsListener: ListenerRegistration? = null
    companion object {
        private const val CHANNEL_ID = "job_notifications"
        private const val NOTIFICATION_GROUP = "com.example.jobrec.JOB_NOTIFICATIONS"
        const val TYPE_NEW_JOB = "new_job"
        const val TYPE_APPLICATION_STATUS = "application_status"
        const val TYPE_NEW_MESSAGE = "new_message"
    }
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Job Notifications"
            val descriptionText = "Notifications for new jobs and application updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun startJobNotificationsListener() {
        val currentUser = auth.currentUser ?: return
        jobsListener?.remove()
        val lastNotificationTime = getLastNotificationTime()
        jobsListener = db.collection("jobs")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val newJobs = snapshot.documents
                        .mapNotNull { it.toObject(com.example.jobrec.Job::class.java) }
                        .filter { job ->
                            job.postedDate.toDate().after(lastNotificationTime)
                        }
                    newJobs.forEach { job ->
                        showJobNotification(job)
                        saveNotification(
                            title = "New Job Posted",
                            message = "${job.title} at ${job.companyName}",
                            type = TYPE_NEW_JOB,
                            jobId = job.id
                        )
                    }
                    val mostRecentJob = newJobs.maxByOrNull { it.postedDate.seconds }
                    if (mostRecentJob != null) {
                        saveLastNotificationTime(mostRecentJob.postedDate.toDate())
                    }
                }
            }
    }
    private fun showJobNotification(job: com.example.jobrec.Job) {
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
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_work)
            .setContentTitle("New Job Posted")
            .setContentText("${job.title} at ${job.companyName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        with(NotificationManagerCompat.from(context)) {
            notify(job.id.hashCode(), builder.build())
        }
    }
    private fun saveNotification(title: String, message: String, type: String, jobId: String? = null) {
        val notificationRepository = com.example.jobrec.repositories.NotificationRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationRepository.saveNotification(title, message, type, jobId)
            } catch (e: Exception) {
            }
        }
    }
    private fun getLastNotificationTime(): Date {
        val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val lastNotificationTime = sharedPreferences.getLong("lastNotificationTime", 0)
        return if (lastNotificationTime > 0) Date(lastNotificationTime) else Date(0)
    }
    private fun saveLastNotificationTime(date: Date) {
        val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastNotificationTime", date.time).apply()
    }
    fun stopJobNotificationsListener() {
        jobsListener?.remove()
    }
}
