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
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
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
        
        // Start listening for new jobs
        jobsListener = db.collection("jobs")
            .whereEqualTo("status", "active")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    // Only notify for new documents
                    if (change.type.name == "ADDED") {
                        val job = change.document.toObject(com.example.jobrec.Job::class.java)
                        
                        // Check if this job was posted after the last notification
                        if (job.postedDate.toDate().after(lastNotificationTime)) {
                            // Send notification for new job
                            showJobNotification(job)
                            
                            // Update last notification time
                            saveLastNotificationTime(job.postedDate.toDate())
                            
                            // Save notification to database
                            saveNotification(
                                title = "New Job Posted",
                                message = "${job.title} at ${job.companyName}",
                                type = TYPE_NEW_JOB,
                                jobId = job.id
                            )
                        }
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
        
        // Create the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_work)
            .setContentTitle("New Job Posted")
            .setContentText("${job.title} at ${job.companyName}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
        
        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(job.id.hashCode(), builder.build())
        }
    }
    
    /**
     * Save a notification to the database
     */
    private fun saveNotification(title: String, message: String, type: String, jobId: String? = null) {
        val currentUser = auth.currentUser ?: return
        
        val notification = hashMapOf(
            "userId" to currentUser.uid,
            "title" to title,
            "message" to message,
            "type" to type,
            "jobId" to jobId,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "read" to false
        )
        
        db.collection("notifications")
            .add(notification)
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
