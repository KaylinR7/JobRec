package com.example.jobrec
import android.app.Application
import android.util.Log
import com.example.jobrec.notifications.NotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
class DUTCareerHubApp : Application() {
    private val TAG = "DUTCareerHubApp"
    override fun onCreate() {
        super.onCreate()
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

        // Initialize notification system
        try {
            Log.d(TAG, "Initializing notification system...")
            NotificationManager.getInstance().initialize(this)
            Log.d(TAG, "Notification system initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing notification system", e)
        }
    }
}
