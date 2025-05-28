package com.example.jobrec.services
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jobrec.HomeActivity
import com.example.jobrec.R
import com.example.jobrec.ChatActivity
import com.example.jobrec.JobDetailsActivity
import com.example.jobrec.StudentApplicationDetailsActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
class FCMService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    companion object {
        const val CHANNEL_ID_JOBS = "job_notifications"
        const val CHANNEL_ID_MESSAGES = "message_notifications"
        const val CHANNEL_ID_APPLICATIONS = "application_notifications"
        const val CHANNEL_ID_PROFILE_VIEWS = "profile_view_notifications"
        const val CHANNEL_NAME_JOBS = "Job Notifications"
        const val CHANNEL_NAME_MESSAGES = "Message Notifications"
        const val CHANNEL_NAME_APPLICATIONS = "Application Updates"
        const val CHANNEL_NAME_PROFILE_VIEWS = "Profile Views"
    }
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        saveTokenToFirestore(token)
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val notificationType = remoteMessage.data["type"] ?: "general"
            when (notificationType) {
                "job" -> handleJobNotification(remoteMessage.data)
                "message" -> handleMessageNotification(remoteMessage.data)
                "application" -> handleApplicationNotification(remoteMessage.data)
                "profile_view" -> handleProfileViewNotification(remoteMessage.data)
                "cv_review" -> handleCvReviewNotification(remoteMessage.data)
                else -> handleGeneralNotification(remoteMessage.data)
            }
        }
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val title = it.title ?: "CareerWorx"
            val body = it.body ?: "You have a new notification"
            if (remoteMessage.data.isNotEmpty()) {
                val notificationType = remoteMessage.data["type"] ?: "general"
                when (notificationType) {
                    "job" -> {
                        val jobId = remoteMessage.data["jobId"]
                        if (jobId != null) {
                            val intent = Intent(this, JobDetailsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("jobId", jobId)
                            }
                            sendNotification(title, body, intent, CHANNEL_ID_JOBS)
                        } else {
                            sendNotification(title, body, null, CHANNEL_ID_JOBS)
                        }
                    }
                    "message" -> {
                        val conversationId = remoteMessage.data["conversationId"]
                        if (conversationId != null) {
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("conversationId", conversationId)
                            }
                            sendNotification(title, body, intent, CHANNEL_ID_MESSAGES)
                        } else {
                            sendNotification(title, body, null, CHANNEL_ID_MESSAGES)
                        }
                    }
                    "application" -> {
                        val applicationId = remoteMessage.data["applicationId"]
                        if (applicationId != null) {
                            val intent = Intent(this, StudentApplicationDetailsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("applicationId", applicationId)
                            }
                            sendNotification(title, body, intent, CHANNEL_ID_APPLICATIONS)
                        } else {
                            sendNotification(title, body, null, CHANNEL_ID_APPLICATIONS)
                        }
                    }
                    "profile_view", "cv_review" -> {
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        sendNotification(title, body, intent, CHANNEL_ID_PROFILE_VIEWS)
                    }
                    else -> sendNotification(title, body, null, null)
                }
            } else {
                sendNotification(title, body, null, null)
            }
        }
    }
    private fun handleJobNotification(data: Map<String, String>) {
        val title = data["title"] ?: "New Job Posting"
        val body = data["body"] ?: "A new job has been posted that matches your profile"
        val jobId = data["jobId"]
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("jobId", jobId)
        }
        sendNotification(title, body, intent, CHANNEL_ID_JOBS)
    }
    private fun handleMessageNotification(data: Map<String, String>) {
        val title = data["title"] ?: "New Message"
        val body = data["body"] ?: "You have received a new message"
        val conversationId = data["conversationId"]
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("conversationId", conversationId)
        }
        sendNotification(title, body, intent, CHANNEL_ID_MESSAGES)
    }
    private fun handleApplicationNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Application Update"
        val body = data["body"] ?: "Your application status has been updated"
        val applicationId = data["applicationId"]
        val intent = Intent(this, StudentApplicationDetailsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("applicationId", applicationId)
        }
        sendNotification(title, body, intent, CHANNEL_ID_APPLICATIONS)
    }

    private fun handleProfileViewNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Profile Viewed"
        val body = data["body"] ?: "An employer has viewed your profile"
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        sendNotification(title, body, intent, CHANNEL_ID_PROFILE_VIEWS)
    }

    private fun handleCvReviewNotification(data: Map<String, String>) {
        val title = data["title"] ?: "CV Reviewed"
        val body = data["body"] ?: "An employer has reviewed your CV"
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        sendNotification(title, body, intent, CHANNEL_ID_PROFILE_VIEWS)
    }

    private fun handleGeneralNotification(data: Map<String, String>) {
        val title = data["title"] ?: "CareerWorx"
        val body = data["body"] ?: "You have a new notification"
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        sendNotification(title, body, intent, null)
    }
    private fun sendNotification(title: String, messageBody: String, intent: Intent?, channelId: String?) {
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            val defaultIntent = Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            PendingIntent.getActivity(
                this, 0, defaultIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId ?: CHANNEL_ID_JOBS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobChannel = NotificationChannel(
                CHANNEL_ID_JOBS,
                CHANNEL_NAME_JOBS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new job postings"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
                setSound(defaultSoundUri, null)
            }
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
                setSound(defaultSoundUri, null)
            }
            val applicationChannel = NotificationChannel(
                CHANNEL_ID_APPLICATIONS,
                CHANNEL_NAME_APPLICATIONS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for application status updates"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
                setSound(defaultSoundUri, null)
            }
            val profileViewChannel = NotificationChannel(
                CHANNEL_ID_PROFILE_VIEWS,
                CHANNEL_NAME_PROFILE_VIEWS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for profile views and CV reviews"
                enableLights(true)
                enableVibration(false) // Less intrusive for profile views
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
                setSound(defaultSoundUri, null)
            }
            notificationManager.createNotificationChannel(jobChannel)
            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(applicationChannel)
            notificationManager.createNotificationChannel(profileViewChannel)
        }
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    private fun saveTokenToFirestore(token: String) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token saved for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving FCM token", e)
                    checkAndSaveCompanyToken(userId, token)
                }
        } else {
            Log.d(TAG, "User not logged in, token not saved")
        }
    }
    private fun checkAndSaveCompanyToken(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("companies")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val companyDoc = documents.documents[0]
                    companyDoc.reference.update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM Token saved for company: ${companyDoc.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving FCM token for company", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking if user is company", e)
            }
    }
}
