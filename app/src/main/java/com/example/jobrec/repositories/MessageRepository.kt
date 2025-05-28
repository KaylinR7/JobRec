package com.example.jobrec.repositories
import com.example.jobrec.models.Conversation
import com.example.jobrec.models.Message
import com.example.jobrec.models.InterviewDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
class MessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    suspend fun sendMessage(message: Message): String {
        val messageId = UUID.randomUUID().toString()
        val newMessage = message.copy(id = messageId)
        android.util.Log.d("MessageRepo", "Sending message: conversationId=${message.conversationId}, senderId=${message.senderId}, receiverId=${message.receiverId}")
        db.collection("messages")
            .document(messageId)
            .set(newMessage)
            .await()
        android.util.Log.d("MessageRepo", "Message saved with ID: $messageId")
        android.util.Log.d("MessageRepo", "Updating conversation: ${message.conversationId}")
        val conversationDoc = db.collection("conversations")
            .document(message.conversationId)
            .get()
            .await()
        if (!conversationDoc.exists()) {
            android.util.Log.e("MessageRepo", "ERROR: Conversation ${message.conversationId} does not exist!")
            val allConversations = db.collection("conversations").get().await()
            android.util.Log.d("MessageRepo", "All conversations (${allConversations.size()}):")
            allConversations.forEach { doc ->
                android.util.Log.d("MessageRepo", "  ${doc.id}: companyId=${doc.getString("companyId")}, candidateId=${doc.getString("candidateId")}")
            }
        } else {
            android.util.Log.d("MessageRepo", "Conversation exists, updating last message")
            val companyId = conversationDoc.getString("companyId") ?: "null"
            val candidateId = conversationDoc.getString("candidateId") ?: "null"
            android.util.Log.d("MessageRepo", "Conversation data: companyId=$companyId, candidateId=$candidateId")
            db.collection("conversations")
                .document(message.conversationId)
                .update(
                    mapOf(
                        "lastMessage" to message.content,
                        "lastMessageTime" to message.createdAt,
                        "lastMessageSender" to message.senderId,
                        "updatedAt" to message.createdAt
                    )
                )
                .await()
            android.util.Log.d("MessageRepo", "Conversation updated successfully")


        }
        return messageId
    }
    suspend fun getConversationMessages(conversationId: String): List<Message> {
        return db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .orderBy("createdAt")
            .get()
            .await()
            .toObjects(Message::class.java)
    }
    suspend fun markMessageAsRead(messageId: String) {
        db.collection("messages")
            .document(messageId)
            .update("isRead", true)
            .await()
    }
    suspend fun scheduleInterview(
        interviewDetails: InterviewDetails,
        conversationId: String,
        receiverId: String
    ): String {
        try {
            val message = Message(
                conversationId = conversationId,
                senderId = auth.currentUser?.uid ?: throw Exception("User not authenticated"),
                receiverId = receiverId,
                content = "Interview scheduled for ${interviewDetails.date} at ${interviewDetails.time}",
                type = "interview",
                interviewDetails = interviewDetails
            )
            return sendMessage(message)
        } catch (e: Exception) {
            throw Exception("Failed to schedule interview: ${e.message}")
        }
    }
    suspend fun uploadFile(fileUrl: String, fileName: String, conversationId: String) {
        val message = Message(
            conversationId = conversationId,
            senderId = auth.currentUser?.uid ?: throw Exception("User not authenticated"),
            type = "file",
            fileUrl = fileUrl,
            fileName = fileName
        )
        sendMessage(message)
    }

    private suspend fun getSenderName(senderId: String): String {
        return try {
            // First check if it's a company user
            val companyQuery = db.collection("companies")
                .whereEqualTo("userId", senderId)
                .get()
                .await()

            if (!companyQuery.isEmpty) {
                companyQuery.documents[0].getString("companyName") ?: "Company"
            } else {
                // Check if it's a regular user
                val userDoc = db.collection("users")
                    .document(senderId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val firstName = userDoc.getString("name") ?: ""
                    val lastName = userDoc.getString("surname") ?: ""
                    "$firstName $lastName".trim().ifEmpty { "User" }
                } else {
                    "Unknown User"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Error getting sender name", e)
            "Unknown User"
        }
    }

    private suspend fun getSenderType(senderId: String): String {
        return try {
            // Check if it's a company user
            val companyQuery = db.collection("companies")
                .whereEqualTo("userId", senderId)
                .get()
                .await()

            if (!companyQuery.isEmpty) {
                "company"
            } else {
                "student"
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Error getting sender type", e)
            "unknown"
        }
    }
}