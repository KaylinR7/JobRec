package com.example.jobrec.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationManager private constructor() {
    
    companion object {
        private const val TAG = "NotificationManager"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        
        @Volatile
        private var INSTANCE: NotificationManager? = null
        
        fun getInstance(): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager().also { INSTANCE = it }
            }
        }
    }
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Initialize FCM and request notification permissions
     */
    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(context)
        }
        
        // Get FCM token and save to user profile
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            
            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")
            saveFCMTokenToUser(token)
        }
    }
    
    /**
     * Request notification permission for Android 13+
     */
    private fun requestNotificationPermission(context: Context) {
        if (context is Activity) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    /**
     * Save FCM token to user's Firestore document
     */
    private fun saveFCMTokenToUser(token: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Try updating user document first
        db.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved to user document")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to save FCM token to user document, trying company", e)
                // If user update fails, try company document
                db.collection("companies").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token saved to company document")
                    }
                    .addOnFailureListener { companyError ->
                        Log.e(TAG, "Failed to save FCM token to company document", companyError)
                    }
            }
    }
    
    /**
     * Send notification for new job application
     */
    suspend fun sendJobApplicationNotification(
        companyId: String,
        applicantName: String,
        jobTitle: String,
        applicationId: String
    ) {
        try {
            val companyDoc = db.collection("companies").document(companyId).get().await()
            val fcmToken = companyDoc.getString("fcmToken") ?: return
            
            val notificationData = mapOf(
                "type" to "job_application",
                "title" to "New Job Application",
                "body" to "$applicantName applied for $jobTitle",
                "applicationId" to applicationId,
                "jobTitle" to jobTitle,
                "applicantName" to applicantName
            )
            
            // Send via HTTP v1 API (implementation will be added)
            sendNotificationViaHTTPv1(fcmToken, notificationData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending job application notification", e)
        }
    }
    
    /**
     * Send notification for application status update
     */
    suspend fun sendApplicationStatusNotification(
        applicantId: String,
        jobTitle: String,
        companyName: String,
        newStatus: String,
        applicationId: String
    ) {
        try {
            val userDoc = db.collection("users").document(applicantId).get().await()
            val fcmToken = userDoc.getString("fcmToken") ?: return
            
            val statusMessage = when (newStatus.lowercase()) {
                "reviewed" -> "Your application for $jobTitle at $companyName has been reviewed"
                "accepted", "hired" -> "Congratulations! Your application for $jobTitle at $companyName has been accepted"
                "rejected" -> "Your application for $jobTitle at $companyName was not successful this time"
                "shortlisted" -> "Great news! You've been shortlisted for $jobTitle at $companyName"
                "interviewing" -> "You've been invited for an interview for $jobTitle at $companyName"
                else -> "Your application status for $jobTitle at $companyName has been updated to $newStatus"
            }
            
            val notificationData = mapOf(
                "type" to "application_status",
                "title" to "Application Update",
                "body" to statusMessage,
                "applicationId" to applicationId,
                "jobTitle" to jobTitle,
                "companyName" to companyName,
                "status" to newStatus
            )
            
            sendNotificationViaHTTPv1(fcmToken, notificationData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending application status notification", e)
        }
    }
    
    /**
     * Send notification for new job posting (to matching students)
     */
    suspend fun sendNewJobNotification(
        jobId: String,
        jobTitle: String,
        companyName: String,
        jobField: String,
        location: String
    ) {
        try {
            // Get students with matching skills/interests
            val studentsSnapshot = db.collection("users")
                .whereEqualTo("role", "student")
                .whereArrayContains("interests", jobField)
                .get()
                .await()
            
            val notificationData = mapOf(
                "type" to "new_job",
                "title" to "New Job Match",
                "body" to "New $jobTitle position at $companyName in $location",
                "jobId" to jobId,
                "jobTitle" to jobTitle,
                "companyName" to companyName,
                "location" to location
            )
            
            for (studentDoc in studentsSnapshot.documents) {
                val fcmToken = studentDoc.getString("fcmToken")
                if (fcmToken != null) {
                    sendNotificationViaHTTPv1(fcmToken, notificationData)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending new job notifications", e)
        }
    }
    
    /**
     * Send notification for meeting invitation
     */
    suspend fun sendMeetingInvitationNotification(
        recipientId: String,
        senderName: String,
        jobTitle: String,
        meetingDate: String,
        meetingTime: String,
        messageId: String
    ) {
        try {
            val userDoc = db.collection("users").document(recipientId).get().await()
            val fcmToken = userDoc.getString("fcmToken") ?: return
            
            val notificationData = mapOf(
                "type" to "meeting_invitation",
                "title" to "Meeting Invitation",
                "body" to "$senderName invited you for an interview regarding $jobTitle on $meetingDate at $meetingTime",
                "messageId" to messageId,
                "jobTitle" to jobTitle,
                "senderName" to senderName,
                "meetingDate" to meetingDate,
                "meetingTime" to meetingTime
            )
            
            sendNotificationViaHTTPv1(fcmToken, notificationData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending meeting invitation notification", e)
        }
    }
    
    /**
     * Send notification via Firebase HTTP v1 API
     * This is a placeholder - actual implementation will use HTTP client
     */
    private suspend fun sendNotificationViaHTTPv1(
        fcmToken: String,
        data: Map<String, String>
    ) {
        // This will be implemented with the HTTP v1 API service
        Log.d(TAG, "Sending notification to token: ${fcmToken.take(10)}... with data: $data")
        
        // For now, we'll use the legacy method through Firebase Admin SDK
        // In production, you would implement the HTTP v1 API call here
    }
    
    /**
     * Subscribe user to topic based on their role and interests
     */
    fun subscribeToTopics(userRole: String, interests: List<String> = emptyList()) {
        val messaging = FirebaseMessaging.getInstance()
        
        // Subscribe to role-based topic
        messaging.subscribeToTopic("${userRole}_notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to ${userRole}_notifications topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to ${userRole}_notifications topic", task.exception)
                }
            }
        
        // Subscribe to interest-based topics for students
        if (userRole == "student") {
            interests.forEach { interest ->
                messaging.subscribeToTopic("jobs_${interest.lowercase().replace(" ", "_")}")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to jobs_${interest} topic")
                        }
                    }
            }
        }
    }
    
    /**
     * Unsubscribe from all topics (useful for logout)
     */
    fun unsubscribeFromAllTopics(userRole: String, interests: List<String> = emptyList()) {
        val messaging = FirebaseMessaging.getInstance()
        
        messaging.unsubscribeFromTopic("${userRole}_notifications")
        
        if (userRole == "student") {
            interests.forEach { interest ->
                messaging.unsubscribeFromTopic("jobs_${interest.lowercase().replace(" ", "_")}")
            }
        }
    }
}
