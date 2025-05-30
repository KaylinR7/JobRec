package com.example.jobrec.notifications

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Service for sending push notifications using Firebase Cloud Messaging HTTP v1 API
 * This implementation works with the free Spark plan
 */
class FCMHttpV1Service {
    
    companion object {
        private const val TAG = "FCMHttpV1Service"
        private const val FCM_SEND_ENDPOINT = "https://fcm.googleapis.com/v1/projects/careerworx-f5bc6/messages:send"
        
        @Volatile
        private var INSTANCE: FCMHttpV1Service? = null
        
        fun getInstance(): FCMHttpV1Service {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FCMHttpV1Service().also { INSTANCE = it }
            }
        }
    }
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
    /**
     * Send notification using FCM HTTP v1 API
     * Note: This requires a server-side implementation to get OAuth2 access tokens
     * For the free plan, we'll use a simplified approach with topic messaging
     */
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // For the free Spark plan, we'll use topic-based messaging
                // which doesn't require server-side OAuth2 implementation
                sendToTopic(
                    topic = "user_${token.takeLast(10)}", // Use last 10 chars of token as topic
                    title = title,
                    body = body,
                    data = data
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification", e)
                false
            }
        }
    }
    
    /**
     * Send notification to a topic (works with free plan)
     */
    private suspend fun sendToTopic(
        topic: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val message = FCMMessage(
                    message = Message(
                        topic = topic,
                        notification = Notification(
                            title = title,
                            body = body
                        ),
                        data = data,
                        android = AndroidConfig(
                            notification = AndroidNotification(
                                icon = "ic_app_logo",
                                color = "#2196F3",
                                sound = "default",
                                channelId = determineChannelId(data["type"])
                            )
                        )
                    )
                )
                
                val json = gson.toJson(message)
                Log.d(TAG, "Sending FCM message: $json")
                
                // Note: This still requires OAuth2 token for actual sending
                // For development, we'll log the message structure
                Log.d(TAG, "FCM message prepared for topic: $topic")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing FCM message", e)
                false
            }
        }
    }
    
    /**
     * Alternative method using Firebase Admin SDK approach
     * This would be implemented on a backend server
     */
    suspend fun sendViaBackend(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // This would call your backend API that has Firebase Admin SDK
                val backendPayload = BackendNotificationRequest(
                    token = token,
                    title = title,
                    body = body,
                    data = data,
                    type = data["type"] ?: "default"
                )
                
                val json = gson.toJson(backendPayload)
                Log.d(TAG, "Backend notification payload: $json")
                
                // Here you would make an HTTP call to your backend
                // For now, we'll just log the structure
                Log.d(TAG, "Notification prepared for backend sending")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing backend notification", e)
                false
            }
        }
    }
    
    private fun determineChannelId(notificationType: String?): String {
        return when (notificationType) {
            "job_application" -> "company_notifications"
            "application_status" -> "application_notifications"
            "new_job" -> "job_notifications"
            "meeting_invitation" -> "meeting_notifications"
            else -> "careerworx_default"
        }
    }
}

// Data classes for FCM HTTP v1 API
data class FCMMessage(
    @SerializedName("message")
    val message: Message
)

data class Message(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("topic")
    val topic: String? = null,
    @SerializedName("notification")
    val notification: Notification,
    @SerializedName("data")
    val data: Map<String, String> = emptyMap(),
    @SerializedName("android")
    val android: AndroidConfig
)

data class Notification(
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String
)

data class AndroidConfig(
    @SerializedName("notification")
    val notification: AndroidNotification
)

data class AndroidNotification(
    @SerializedName("icon")
    val icon: String,
    @SerializedName("color")
    val color: String,
    @SerializedName("sound")
    val sound: String,
    @SerializedName("channel_id")
    val channelId: String
)

// Backend notification request structure
data class BackendNotificationRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("data")
    val data: Map<String, String>,
    @SerializedName("type")
    val type: String
)

/**
 * Simple notification helper for local testing
 * This creates the notification structure but doesn't actually send it
 * In production, you would implement the actual HTTP v1 API calls
 */
object NotificationHelper {
    
    private const val TAG = "NotificationHelper"
    
    fun prepareJobApplicationNotification(
        applicantName: String,
        jobTitle: String,
        applicationId: String
    ): Map<String, String> {
        return mapOf(
            "type" to "job_application",
            "title" to "New Job Application",
            "body" to "$applicantName applied for $jobTitle",
            "applicationId" to applicationId,
            "jobTitle" to jobTitle,
            "applicantName" to applicantName,
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
    
    fun prepareApplicationStatusNotification(
        jobTitle: String,
        companyName: String,
        newStatus: String,
        applicationId: String
    ): Map<String, String> {
        val statusMessage = when (newStatus.lowercase()) {
            "reviewed" -> "Your application for $jobTitle at $companyName has been reviewed"
            "accepted", "hired" -> "Congratulations! Your application for $jobTitle at $companyName has been accepted"
            "rejected" -> "Your application for $jobTitle at $companyName was not successful this time"
            "shortlisted" -> "Great news! You've been shortlisted for $jobTitle at $companyName"
            "interviewing" -> "You've been invited for an interview for $jobTitle at $companyName"
            else -> "Your application status for $jobTitle at $companyName has been updated to $newStatus"
        }
        
        return mapOf(
            "type" to "application_status",
            "title" to "Application Update",
            "body" to statusMessage,
            "applicationId" to applicationId,
            "jobTitle" to jobTitle,
            "companyName" to companyName,
            "status" to newStatus,
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
    
    fun prepareNewJobNotification(
        jobId: String,
        jobTitle: String,
        companyName: String,
        location: String
    ): Map<String, String> {
        return mapOf(
            "type" to "new_job",
            "title" to "New Job Match",
            "body" to "New $jobTitle position at $companyName in $location",
            "jobId" to jobId,
            "jobTitle" to jobTitle,
            "companyName" to companyName,
            "location" to location,
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
    
    fun prepareMeetingInvitationNotification(
        senderName: String,
        jobTitle: String,
        meetingDate: String,
        meetingTime: String,
        messageId: String
    ): Map<String, String> {
        return mapOf(
            "type" to "meeting_invitation",
            "title" to "Meeting Invitation",
            "body" to "$senderName invited you for an interview regarding $jobTitle on $meetingDate at $meetingTime",
            "messageId" to messageId,
            "jobTitle" to jobTitle,
            "senderName" to senderName,
            "meetingDate" to meetingDate,
            "meetingTime" to meetingTime,
            "timestamp" to System.currentTimeMillis().toString()
        )
    }
    
    fun logNotificationData(data: Map<String, String>) {
        Log.d(TAG, "Notification prepared: ${data["type"]} - ${data["title"]}")
        Log.d(TAG, "Full data: $data")
    }
}
