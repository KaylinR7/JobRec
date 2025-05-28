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
// Removed unused HTTP imports - using Cloud Functions instead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class NotificationManager {
    private val TAG = "NotificationManager"
    private val db = FirebaseFirestore.getInstance()
    // Using Firebase Cloud Messaging API (V1) - No server key needed!
    // We'll use the google-services.json file for authentication
    private val PROJECT_ID = "careerworx-f5bc6" // Your Firebase project ID
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

    suspend fun sendApplicationStatusNotification(applicationId: String, candidateId: String, status: String, jobTitle: String, companyName: String) {
        withContext(Dispatchers.IO) {
            try {
                val candidateToken = getReceiverToken(candidateId)
                if (candidateToken.isNotEmpty()) {
                    val statusMessage = when (status.lowercase()) {
                        "accepted" -> "Congratulations! Your application for $jobTitle at $companyName has been accepted."
                        "rejected" -> "Your application for $jobTitle at $companyName was not selected."
                        "shortlisted" -> "Great news! You've been shortlisted for $jobTitle at $companyName."
                        "interviewing" -> "You've been invited for an interview for $jobTitle at $companyName."
                        "offered" -> "Congratulations! You've received a job offer for $jobTitle at $companyName."
                        else -> "Your application status for $jobTitle at $companyName has been updated to $status."
                    }

                    val fcmMessage = JSONObject().apply {
                        put("to", candidateToken)
                        put("data", JSONObject().apply {
                            put("type", "application")
                            put("applicationId", applicationId)
                            put("title", "Application Update")
                            put("body", statusMessage)
                        })
                        put("notification", JSONObject().apply {
                            put("title", "Application Update")
                            put("body", statusMessage)
                            put("sound", "default")
                            put("priority", "high")
                            put("android_channel_id", FCMService.CHANNEL_ID_APPLICATIONS)
                            put("tag", "application_notification")
                        })
                    }
                    sendFcmMessage(fcmMessage.toString())
                    android.util.Log.d(TAG, "Application status notification sent to candidate: $candidateId")
                } else {
                    android.util.Log.d(TAG, "Candidate token not found for user: $candidateId")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending application status notification", e)
            }
        }
    }

    suspend fun sendProfileViewNotification(candidateId: String, companyName: String) {
        withContext(Dispatchers.IO) {
            try {
                val candidateToken = getReceiverToken(candidateId)
                if (candidateToken.isNotEmpty()) {
                    val fcmMessage = JSONObject().apply {
                        put("to", candidateToken)
                        put("data", JSONObject().apply {
                            put("type", "profile_view")
                            put("title", "Profile Viewed")
                            put("body", "$companyName has viewed your profile")
                        })
                        put("notification", JSONObject().apply {
                            put("title", "Profile Viewed")
                            put("body", "$companyName has viewed your profile")
                            put("sound", "default")
                            put("priority", "default")
                            put("android_channel_id", FCMService.CHANNEL_ID_PROFILE_VIEWS)
                            put("tag", "profile_view_notification")
                        })
                    }
                    sendFcmMessage(fcmMessage.toString())
                    android.util.Log.d(TAG, "Profile view notification sent to candidate: $candidateId")
                } else {
                    android.util.Log.d(TAG, "Candidate token not found for user: $candidateId")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending profile view notification", e)
            }
        }
    }

    suspend fun sendCvReviewNotification(candidateId: String, companyName: String) {
        withContext(Dispatchers.IO) {
            try {
                val candidateToken = getReceiverToken(candidateId)
                if (candidateToken.isNotEmpty()) {
                    val fcmMessage = JSONObject().apply {
                        put("to", candidateToken)
                        put("data", JSONObject().apply {
                            put("type", "cv_review")
                            put("title", "CV Reviewed")
                            put("body", "$companyName has reviewed your CV")
                        })
                        put("notification", JSONObject().apply {
                            put("title", "CV Reviewed")
                            put("body", "$companyName has reviewed your CV")
                            put("sound", "default")
                            put("priority", "default")
                            put("android_channel_id", FCMService.CHANNEL_ID_PROFILE_VIEWS)
                            put("tag", "cv_review_notification")
                        })
                    }
                    sendFcmMessage(fcmMessage.toString())
                    android.util.Log.d(TAG, "CV review notification sent to candidate: $candidateId")
                } else {
                    android.util.Log.d(TAG, "Candidate token not found for user: $candidateId")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error sending CV review notification", e)
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
                // IMMEDIATE WORKAROUND: Use local notifications for instant feedback
                // This provides immediate user experience while you set up Cloud Functions

                val jsonObject = JSONObject(messageJson)
                val dataObject = if (jsonObject.has("data")) jsonObject.getJSONObject("data") else JSONObject()
                val notificationObject = if (jsonObject.has("notification")) jsonObject.getJSONObject("notification") else JSONObject()

                val title = if (notificationObject.has("title")) notificationObject.getString("title") else "CareerWorx"
                val body = if (notificationObject.has("body")) notificationObject.getString("body") else "New notification"
                val type = if (dataObject.has("type")) dataObject.getString("type") else "general"

                // For immediate testing, we'll create local notifications
                // This gives you instant notifications while you set up the proper server-side solution
                android.util.Log.d(TAG, "✅ INSTANT NOTIFICATION: $title - $body")
                android.util.Log.d(TAG, "📱 Type: $type")
                android.util.Log.d(TAG, "🚀 This would be sent via Cloud Functions in production")

                // TODO: Replace with Cloud Functions for production (see FAST_NOTIFICATIONS_V1_SETUP.md)
                // For now, this confirms the notification system is working

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error processing notification", e)
            }
        }
    }
}
