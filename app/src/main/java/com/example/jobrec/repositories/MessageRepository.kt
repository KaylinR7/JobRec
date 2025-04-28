package com.example.jobrec.repositories

import com.example.jobrec.models.Message
import com.example.jobrec.models.InterviewDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun sendMessage(message: Message): String {
        val messageId = UUID.randomUUID().toString()
        val newMessage = message.copy(id = messageId)
        
        db.collection("messages")
            .document(messageId)
            .set(newMessage)
            .await()
            
        // Update conversation last message
        db.collection("conversations")
            .document(message.conversationId)
            .update(
                mapOf(
                    "lastMessage" to message.content,
                    "lastMessageTime" to message.createdAt,
                    "lastMessageSender" to message.senderId
                )
            )
            .await()
            
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
} 