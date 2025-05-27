package com.example.jobrec.services
import android.util.Log
import com.example.jobrec.services.FCMService
import com.example.jobrec.models.Conversation
import com.example.jobrec.Job
import com.example.jobrec.models.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class NotificationManager {
    private val TAG = "NotificationManager"
    private val db = FirebaseFirestore.getInstance()
    private val FCM_SERVER_KEY = "YOUR_FCM_SERVER_KEY"
    companion object {
        const val TOPIC_ALL_JOBS = "all_jobs"
        const val TOPIC_JOB_CATEGORY_PREFIX = "job_category_"
        const val TOPIC_JOB_SPECIALIZATION_PREFIX = "job_specialization_"
    }
    suspend fun subscribeToTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            android.util.Log.d(TAG, "Subscribed to topic: $topic")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error subscribing to topic: $topic", e)
            throw e
        }
    }
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            android.util.Log.d(TAG, "Unsubscribed from topic: $topic")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error unsubscribing from topic: $topic", e)
            throw e
        }
    }
    suspend fun sendNewJobNotification(job: Job) {
        withContext(Dispatchers.IO) {
            try {
                val topics = mutableListOf<String>()
                topics.add(TOPIC_ALL_JOBS)
                if (job.jobField.isNotEmpty()) {
                    val categoryTopic = TOPIC_JOB_CATEGORY_PREFIX + job.jobField.lowercase().replace(" ", "_")
                    topics.add(categoryTopic)
                }
                if (job.specialization.isNotEmpty()) {
                    val specializationTopic = TOPIC_JOB_SPECIALIZATION_PREFIX + job.specialization.lowercase().replace(" ", "_")
                    topics.add(specializationTopic)
                }
                for (topic in topics) {
                    val message = JSONObject().apply {
                        put("to", "/topics/$topic")
                        put("data", JSONObject().apply {
                            put("type", "job")
                            put("jobId", job.id)
                            put("title", "New Job: ${job.title}")
                            put("body", "${job.title} at ${job.companyName}")
                        })
                        put("notification", JSONObject().apply {
                            put("title", "New Job: ${job.title}")
                            put("body", "${job.title} at ${job.companyName}")
                            put("sound", "default")
                            put("priority", "high")
                            put("android_channel_id", FCMService.CHANNEL_ID_JOBS)
                            put("tag", "job_notification")
                        })
                    }
                    sendFcmMessage(message.toString())
                }
                android.util.Log.d(TAG, "Job notification sent to ${topics.size} topics")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending job notification", e)
            }
        }
    }
    suspend fun sendNewMessageNotification(message: Message, conversation: Conversation) {
        withContext(Dispatchers.IO) {
            try {
                val receiverToken = getReceiverToken(message.receiverId)
                if (receiverToken.isNotEmpty()) {
                    val fcmMessage = JSONObject().apply {
                        put("to", receiverToken)
                        put("data", JSONObject().apply {
                            put("type", "message")
                            put("conversationId", conversation.id)
                            put("title", "New message from ${if (message.senderId == conversation.companyId) conversation.companyName else conversation.candidateName}")
                            put("body", message.content)
                        })
                        put("notification", JSONObject().apply {
                            put("title", "New message from ${if (message.senderId == conversation.companyId) conversation.companyName else conversation.candidateName}")
                            put("body", message.content)
                            put("sound", "default")
                            put("priority", "high")
                            put("android_channel_id", FCMService.CHANNEL_ID_MESSAGES)
                            put("tag", "message_notification")
                        })
                    }
                    sendFcmMessage(fcmMessage.toString())
                    android.util.Log.d(TAG, "Message notification sent to user: ${message.receiverId}")
                } else {
                    android.util.Log.d(TAG, "Receiver token not found for user: ${message.receiverId}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending message notification", e)
            }
        }
    }
    private suspend fun getReceiverToken(userId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists() && userDoc.contains("fcmToken")) {
                    return@withContext userDoc.getString("fcmToken") ?: ""
                }
                val companyQuery = db.collection("companies")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                if (!companyQuery.isEmpty) {
                    val companyDoc = companyQuery.documents[0]
                    if (companyDoc.contains("fcmToken")) {
                        return@withContext companyDoc.getString("fcmToken") ?: ""
                    }
                }
                val directCompanyDoc = db.collection("companies").document(userId).get().await()
                if (directCompanyDoc.exists() && directCompanyDoc.contains("fcmToken")) {
                    return@withContext directCompanyDoc.getString("fcmToken") ?: ""
                }
                return@withContext ""
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getting receiver token", e)
                return@withContext ""
            }
        }
    }
    private suspend fun sendFcmMessage(messageJson: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://fcm.googleapis.com/fcm/send")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "key=$FCM_SERVER_KEY")
                }
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(messageJson)
                writer.flush()
                writer.close()
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    android.util.Log.d(TAG, "FCM message sent successfully")
                } else {
                    android.util.Log.e(TAG, "FCM error: ${conn.responseMessage}, code: $responseCode")
                }
                conn.disconnect()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending FCM message", e)
            }
        }
    }
}
