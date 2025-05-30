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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CareerWorxFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "FCMService"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(
                title = it.title ?: "CareerWorx",
                body = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val notificationType = data["type"] ?: return
        val title = data["title"] ?: "CareerWorx"
        val body = data["body"] ?: ""
        
        when (notificationType) {
            "job_application" -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = getString(R.string.company_notifications_channel_id),
                    data = data
                )
            }
            "application_status" -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = getString(R.string.application_notifications_channel_id),
                    data = data
                )
            }
            "new_job" -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = getString(R.string.job_notifications_channel_id),
                    data = data
                )
            }
            "meeting_invitation" -> {
                showNotification(
                    title = title,
                    body = body,
                    channelId = getString(R.string.meeting_notifications_channel_id),
                    data = data
                )
            }
            else -> {
                showNotification(title = title, body = body, data = data)
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String = getString(R.string.default_notification_channel_id),
        data: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add extra data for navigation
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Default channel
            val defaultChannel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.default_notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Job notifications channel
            val jobChannel = NotificationChannel(
                getString(R.string.job_notifications_channel_id),
                getString(R.string.job_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.job_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Application notifications channel
            val applicationChannel = NotificationChannel(
                getString(R.string.application_notifications_channel_id),
                getString(R.string.application_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.application_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Meeting notifications channel
            val meetingChannel = NotificationChannel(
                getString(R.string.meeting_notifications_channel_id),
                getString(R.string.meeting_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.meeting_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Company notifications channel
            val companyChannel = NotificationChannel(
                getString(R.string.company_notifications_channel_id),
                getString(R.string.company_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.company_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, jobChannel, applicationChannel, meetingChannel, companyChannel)
            )
        }
    }

    private fun sendRegistrationToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        // Update user document with FCM token
        db.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token", e)
                // Try updating company document if user update fails
                db.collection("companies").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token updated for company successfully")
                    }
                    .addOnFailureListener { companyError ->
                        Log.e(TAG, "Failed to update FCM token for company", companyError)
                    }
            }
    }
}
