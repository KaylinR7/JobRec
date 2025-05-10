package com.example.jobrec.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jobrec.R
import com.example.jobrec.SplashActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "JobRec", it.body ?: "You have a new notification")
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "JobRec"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            val type = remoteMessage.data["type"]
            val id = remoteMessage.data["id"]
            
            sendNotification(title, message, type, id)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // If you want to send messages to this application instance or
        // manage this app's subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Implement this method to send token to your app server
        // This is where you would typically send the token to your server
        Log.d(TAG, "Sending FCM token to server: $token")
    }

    private fun sendNotification(title: String, messageBody: String, type: String? = null, id: String? = null) {
        val intent = Intent(this, SplashActivity::class.java)
        
        // Add data to intent if available
        if (type != null && id != null) {
            intent.putExtra("notification_type", type)
            intent.putExtra("notification_id", id)
            
            // Create a broadcast intent for the notification click
            val broadcastIntent = Intent("com.example.jobrec.NOTIFICATION_CLICKED")
            broadcastIntent.putExtra("notification_type", type)
            broadcastIntent.putExtra("notification_id", id)
            sendBroadcast(broadcastIntent)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "jobrec_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(resources.getColor(R.color.primary, theme))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JobRec Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
