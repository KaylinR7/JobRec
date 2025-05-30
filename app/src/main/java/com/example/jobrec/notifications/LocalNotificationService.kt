package com.example.jobrec.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jobrec.R
import com.example.jobrec.SplashActivity

/**
 * Local notification service for testing push notifications
 * This simulates what would happen when FCM notifications are received
 */
class LocalNotificationService private constructor() {
    
    companion object {
        private const val TAG = "LocalNotificationService"
        
        @Volatile
        private var INSTANCE: LocalNotificationService? = null
        
        fun getInstance(): LocalNotificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalNotificationService().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Show a local notification for testing purposes
     */
    fun showTestNotification(
        context: Context,
        title: String,
        body: String,
        notificationType: String = "default",
        data: Map<String, String> = emptyMap()
    ) {
        val channelId = getChannelIdForType(context, notificationType)
        val notificationId = System.currentTimeMillis().toInt()
        
        // Create intent for when notification is tapped
        val intent = Intent(context, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add extra data for navigation
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            putExtra("notification_type", notificationType)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        
        // Add action buttons based on notification type
        when (notificationType) {
            "job_application" -> {
                val viewIntent = Intent(context, SplashActivity::class.java).apply {
                    putExtra("action", "view_applications")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId + 1, 
                    viewIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_application,
                    "View Applications",
                    viewPendingIntent
                )
            }
            "application_status" -> {
                val viewIntent = Intent(context, SplashActivity::class.java).apply {
                    putExtra("action", "view_my_applications")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId + 1, 
                    viewIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_application,
                    "View Status",
                    viewPendingIntent
                )
            }
            "new_job" -> {
                val viewIntent = Intent(context, SplashActivity::class.java).apply {
                    putExtra("action", "view_jobs")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId + 1, 
                    viewIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_jobs,
                    "View Jobs",
                    viewPendingIntent
                )
            }
            "meeting_invitation" -> {
                val viewIntent = Intent(context, SplashActivity::class.java).apply {
                    putExtra("action", "view_calendar")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context, 
                    notificationId + 1, 
                    viewIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_calendar,
                    "View Calendar",
                    viewPendingIntent
                )
            }
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Log.d(TAG, "Local test notification shown: $title - $body")
    }
    
    private fun getChannelIdForType(context: Context, notificationType: String): String {
        return when (notificationType) {
            "job_application" -> context.getString(R.string.company_notifications_channel_id)
            "application_status" -> context.getString(R.string.application_notifications_channel_id)
            "new_job" -> context.getString(R.string.job_notifications_channel_id)
            "meeting_invitation" -> context.getString(R.string.meeting_notifications_channel_id)
            else -> context.getString(R.string.default_notification_channel_id)
        }
    }
    
    /**
     * Test different notification types
     */
    fun testAllNotificationTypes(context: Context) {
        // Test job application notification (for companies)
        showTestNotification(
            context = context,
            title = "New Job Application",
            body = "John Doe applied for Software Developer position",
            notificationType = "job_application",
            data = mapOf(
                "applicationId" to "test_app_123",
                "jobTitle" to "Software Developer",
                "applicantName" to "John Doe"
            )
        )
        
        // Test application status notification (for students)
        showTestNotification(
            context = context,
            title = "Application Update",
            body = "Your application for Software Developer at TechCorp has been reviewed",
            notificationType = "application_status",
            data = mapOf(
                "applicationId" to "test_app_456",
                "jobTitle" to "Software Developer",
                "companyName" to "TechCorp",
                "status" to "reviewed"
            )
        )
        
        // Test new job notification (for students)
        showTestNotification(
            context = context,
            title = "New Job Match",
            body = "New Android Developer position at InnovateTech in Cape Town",
            notificationType = "new_job",
            data = mapOf(
                "jobId" to "test_job_789",
                "jobTitle" to "Android Developer",
                "companyName" to "InnovateTech",
                "location" to "Cape Town"
            )
        )
        
        // Test meeting invitation notification (for students)
        showTestNotification(
            context = context,
            title = "Meeting Invitation",
            body = "TechCorp invited you for an interview regarding Software Developer on Dec 15, 2024 at 14:00",
            notificationType = "meeting_invitation",
            data = mapOf(
                "messageId" to "test_msg_101",
                "jobTitle" to "Software Developer",
                "senderName" to "TechCorp",
                "meetingDate" to "Dec 15, 2024",
                "meetingTime" to "14:00"
            )
        )
        
        Log.d(TAG, "All test notifications sent")
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        Log.d(TAG, "All notifications cleared")
    }
}
