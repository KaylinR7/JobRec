package com.example.jobrec.notifications

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service for sending push notifications using Firebase Functions
 * This is the modern, secure way to send FCM notifications without server keys
 */
class FirebaseFunctionsNotificationService {

    companion object {
        private const val TAG = "FirebaseFunctionsNotificationService"

        @Volatile
        private var INSTANCE: FirebaseFunctionsNotificationService? = null

        fun getInstance(): FirebaseFunctionsNotificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseFunctionsNotificationService().also { INSTANCE = it }
            }
        }
    }

    private val functions: FirebaseFunctions = Firebase.functions

    /**
     * Send notification to a specific device token
     */
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending notification via Firebase Functions")
                Log.d(TAG, "Target token: ${token.take(10)}...")
                Log.d(TAG, "Title: $title, Body: $body")

                val requestData = hashMapOf(
                    "token" to token,
                    "title" to title,
                    "body" to body,
                    "notificationData" to data
                )

                val result = functions
                    .getHttpsCallable("sendNotification")
                    .call(requestData)
                    .await()

                val response = result.data as? Map<String, Any>
                val success = response?.get("success") as? Boolean ?: false

                if (success) {
                    Log.d(TAG, "Notification sent successfully via Firebase Functions")
                    val messageId = response?.get("messageId") as? String
                    Log.d(TAG, "Message ID: $messageId")
                } else {
                    Log.e(TAG, "Failed to send notification via Firebase Functions")
                }

                success

            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification via Firebase Functions", e)
                Log.w(TAG, "Make sure Firebase Functions are deployed: firebase deploy --only functions")
                false
            }
        }
    }

    /**
     * Send notification to multiple device tokens
     * Note: Requires Firebase Functions to be deployed first
     */
    suspend fun sendMulticastNotification(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Multicast notification service called for ${tokens.size} devices")
                Log.w(TAG, "Firebase Functions not yet implemented - requires deployment")
                false

            } catch (e: Exception) {
                Log.e(TAG, "Error in multicast notification service", e)
                false
            }
        }
    }

    /**
     * Send notification to a topic
     * Note: Requires Firebase Functions to be deployed first
     */
    suspend fun sendTopicNotification(
        topic: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Topic notification service called for: $topic")
                Log.w(TAG, "Firebase Functions not yet implemented - requires deployment")
                false

            } catch (e: Exception) {
                Log.e(TAG, "Error in topic notification service", e)
                false
            }
        }
    }

    /**
     * Test the Firebase Functions connection
     */
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing Firebase Functions connection...")

                // Try to call a simple function to test connectivity
                val testData = hashMapOf(
                    "token" to "test_token",
                    "title" to "Test",
                    "body" to "Connection test",
                    "notificationData" to emptyMap<String, String>()
                )

                val result = functions.getHttpsCallable("sendNotification").call(testData).await()

                Log.d(TAG, "Firebase Functions connection successful")
                true

            } catch (e: Exception) {
                Log.w(TAG, "Firebase Functions not available: ${e.message}")
                Log.w(TAG, "Make sure functions are deployed: firebase deploy --only functions")
                false
            }
        }
    }
}
