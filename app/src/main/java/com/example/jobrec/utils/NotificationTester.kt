package com.example.jobrec.utils

import android.content.Context
import android.util.Log
import com.example.jobrec.services.NotificationManager
import com.example.jobrec.models.Job
import com.example.jobrec.models.Message
import com.example.jobrec.models.Conversation
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class for testing notifications in development
 * Use this to verify that your FCM setup is working correctly
 */
class NotificationTester(private val context: Context) {
    private val TAG = "NotificationTester"
    private val notificationManager = NotificationManager()

    /**
     * Test job posting notification
     */
    fun testJobNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testJob = Job(
                    id = "test_job_123",
                    title = "Test Software Developer",
                    companyName = "Test Company",
                    jobField = "Technology",
                    specialization = "Software Development",
                    location = "Johannesburg",
                    salary = "R25,000 - R35,000",
                    type = "Full-time",
                    experienceLevel = "Mid-level",
                    description = "This is a test job posting for notification testing",
                    requirements = "Test requirements",
                    postedDate = Timestamp.now(),
                    status = "active"
                )

                notificationManager.sendNewJobNotification(testJob)
                Log.d(TAG, "Test job notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test job notification", e)
            }
        }
    }

    /**
     * Test application status notification
     */
    fun testApplicationStatusNotification(candidateId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationManager.sendApplicationStatusNotification(
                    applicationId = "test_app_123",
                    candidateId = candidateId,
                    status = "accepted",
                    jobTitle = "Test Software Developer",
                    companyName = "Test Company"
                )
                Log.d(TAG, "Test application status notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test application status notification", e)
            }
        }
    }

    /**
     * Test profile view notification
     */
    fun testProfileViewNotification(candidateId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationManager.sendProfileViewNotification(
                    candidateId = candidateId,
                    companyName = "Test Company"
                )
                Log.d(TAG, "Test profile view notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test profile view notification", e)
            }
        }
    }

    /**
     * Test CV review notification
     */
    fun testCvReviewNotification(candidateId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationManager.sendCvReviewNotification(
                    candidateId = candidateId,
                    companyName = "Test Company"
                )
                Log.d(TAG, "Test CV review notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test CV review notification", e)
            }
        }
    }

    /**
     * Test message notification
     */
    fun testMessageNotification(receiverId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testMessage = Message(
                    id = "test_msg_123",
                    conversationId = "test_conv_123",
                    senderId = "test_sender_123",
                    receiverId = receiverId,
                    content = "This is a test message for notification testing",
                    type = "text",
                    createdAt = Timestamp.now()
                )

                val testConversation = Conversation(
                    id = "test_conv_123",
                    candidateId = receiverId,
                    companyId = "test_company_123",
                    candidateName = "Test Candidate",
                    companyName = "Test Company",
                    lastMessage = testMessage.content,
                    lastMessageTime = testMessage.createdAt,
                    lastMessageSender = testMessage.senderId
                )

                notificationManager.sendNewMessageNotification(testMessage, testConversation)
                Log.d(TAG, "Test message notification sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test message notification", e)
            }
        }
    }

    /**
     * Test all notification types for a specific user
     */
    fun testAllNotifications(candidateId: String) {
        Log.d(TAG, "Testing all notifications for candidate: $candidateId")

        // Test with delays to avoid overwhelming
        CoroutineScope(Dispatchers.Main).launch {
            testJobNotification()

            kotlinx.coroutines.delay(2000)
            testApplicationStatusNotification(candidateId)

            kotlinx.coroutines.delay(2000)
            testProfileViewNotification(candidateId)

            kotlinx.coroutines.delay(2000)
            testCvReviewNotification(candidateId)

            kotlinx.coroutines.delay(2000)
            testMessageNotification(candidateId)

            Log.d(TAG, "All test notifications sent")
        }
    }

    /**
     * Quick test method - call this from any activity
     * Example: NotificationTester(this).quickTest("your_user_id")
     */
    fun quickTest(userId: String) {
        Log.d(TAG, "🧪 QUICK NOTIFICATION TEST STARTING...")
        testApplicationStatusNotification(userId)
    }
}
