package com.example.jobrec
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.os.Build
import android.util.Log
import com.example.jobrec.notifications.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
class DUTCareerHubApp : Application() {
    private val TAG = "DUTCareerHubApp"

    companion object {
        lateinit var instance: DUTCareerHubApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase App Check
        try {
            Log.d(TAG, "Initializing Firebase App Check...")
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Firebase App Check initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase App Check", e)
        }

        val settings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
        try {
            Log.d(TAG, "Creating Firestore indexes...")
            FirestoreIndexManager.createIndexes()
            Log.d(TAG, "Firestore indexes creation initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Firestore indexes", e)
        }

        // Create notification channels first
        try {
            Log.d(TAG, "Creating notification channels...")
            createNotificationChannels()
            Log.d(TAG, "Notification channels created")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channels", e)
        }

        // Initialize notification system
        try {
            Log.d(TAG, "Initializing notification system...")
            NotificationManager.getInstance().initialize(this)
            Log.d(TAG, "Notification system initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing notification system", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as AndroidNotificationManager

            // Default channel
            val defaultChannel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.default_notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Job notifications channel
            val jobChannel = NotificationChannel(
                getString(R.string.job_notifications_channel_id),
                getString(R.string.job_notifications_channel_name),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.job_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Application notifications channel
            val applicationChannel = NotificationChannel(
                getString(R.string.application_notifications_channel_id),
                getString(R.string.application_notifications_channel_name),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.application_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Meeting notifications channel
            val meetingChannel = NotificationChannel(
                getString(R.string.meeting_notifications_channel_id),
                getString(R.string.meeting_notifications_channel_name),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.meeting_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            // Company notifications channel
            val companyChannel = NotificationChannel(
                getString(R.string.company_notifications_channel_id),
                getString(R.string.company_notifications_channel_name),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.company_notifications_channel_description)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, jobChannel, applicationChannel, meetingChannel, companyChannel)
            )

            Log.d(TAG, "Created notification channels: ${listOf(defaultChannel, jobChannel, applicationChannel, meetingChannel, companyChannel).map { it.id }}")
        }
    }
}
