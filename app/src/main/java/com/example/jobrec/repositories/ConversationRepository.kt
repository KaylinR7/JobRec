package com.example.jobrec.repositories

import com.example.jobrec.models.Conversation
import com.example.jobrec.models.Message
import com.example.jobrec.models.InterviewDetails
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ConversationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messageRepository = MessageRepository()

    suspend fun createConversation(
        applicationId: String,
        jobId: String,
        jobTitle: String,
        candidateId: String,
        candidateName: String,
        companyId: String,
        companyName: String
    ): String {
        // Check if conversation already exists for this application
        val existingConversation = getConversationByApplicationId(applicationId)
        if (existingConversation != null) {
            return existingConversation.id
        }

        // Create new conversation
        val conversationId = UUID.randomUUID().toString()
        val conversation = Conversation(
            id = conversationId,
            applicationId = applicationId,
            jobId = jobId,
            jobTitle = jobTitle,
            candidateId = candidateId,
            candidateName = candidateName,
            companyId = companyId,
            companyName = companyName
        )

        db.collection("conversations")
            .document(conversationId)
            .set(conversation)
            .await()

        return conversationId
    }

    suspend fun getConversationByApplicationId(applicationId: String): Conversation? {
        val result = db.collection("conversations")
            .whereEqualTo("applicationId", applicationId)
            .get()
            .await()

        return if (!result.isEmpty) {
            result.documents[0].toObject(Conversation::class.java)
        } else {
            null
        }
    }

    suspend fun getConversationById(conversationId: String): Conversation? {
        val document = db.collection("conversations")
            .document(conversationId)
            .get()
            .await()

        return if (document.exists()) {
            document.toObject(Conversation::class.java)
        } else {
            null
        }
    }

    suspend fun getUserConversations(userId: String): List<Conversation> {
        // Get conversations where user is the candidate (without sorting)
        val candidateConversations = db.collection("conversations")
            .whereEqualTo("candidateId", userId)
            .get()
            .await()
            .toObjects(Conversation::class.java)

        // Get conversations where user is the company (without sorting)
        val companyConversations = db.collection("conversations")
            .whereEqualTo("companyId", userId)
            .get()
            .await()
            .toObjects(Conversation::class.java)

        // Combine and sort in memory
        return (candidateConversations + companyConversations).sortedByDescending { it.updatedAt.seconds }
    }

    suspend fun sendMessage(conversationId: String, content: String, receiverId: String): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val senderId = currentUser.uid

        val message = Message(
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content
        )

        val messageId = messageRepository.sendMessage(message)

        // Update conversation with last message
        db.collection("conversations")
            .document(conversationId)
            .update(
                mapOf(
                    "lastMessage" to content,
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSender" to senderId,
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()

        return messageId
    }

    suspend fun sendMeetingInvite(
        conversationId: String,
        receiverId: String,
        date: Timestamp,
        time: String,
        duration: Int,
        type: String,
        location: String? = null,
        meetingLink: String? = null
    ): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val senderId = currentUser.uid

        val interviewDetails = InterviewDetails(
            date = date,
            time = time,
            duration = duration,
            type = type,
            location = location,
            meetingLink = meetingLink
        )

        val message = Message(
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = "Meeting invitation",
            type = "interview",
            interviewDetails = interviewDetails
        )

        val messageId = messageRepository.sendMessage(message)

        // Update conversation with last message
        db.collection("conversations")
            .document(conversationId)
            .update(
                mapOf(
                    "lastMessage" to "Meeting invitation",
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSender" to senderId,
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()

        return messageId
    }

    suspend fun updateMeetingStatus(messageId: String, status: String) {
        db.collection("messages")
            .document(messageId)
            .update("interviewDetails.status", status)
            .await()
    }

    suspend fun markConversationAsRead(conversationId: String) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Get all unread messages in this conversation sent to the current user
        val unreadMessages = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
            .documents

        // Mark each message as read
        for (message in unreadMessages) {
            db.collection("messages")
                .document(message.id)
                .update("isRead", true)
                .await()
        }

        // Update unread count in conversation
        db.collection("conversations")
            .document(conversationId)
            .update("unreadCount", 0)
            .await()
    }
}
