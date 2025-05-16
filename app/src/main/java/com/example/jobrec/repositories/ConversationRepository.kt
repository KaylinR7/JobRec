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
        android.util.Log.d("ConversationRepo", "Creating conversation for application: $applicationId")
        android.util.Log.d("ConversationRepo", "Company ID: $companyId, Company Name: $companyName")
        android.util.Log.d("ConversationRepo", "Candidate ID: $candidateId, Candidate Name: $candidateName")

        // Check if conversation already exists for this application
        val existingConversation = getConversationByApplicationId(applicationId)
        if (existingConversation != null) {
            android.util.Log.d("ConversationRepo", "Conversation already exists with ID: ${existingConversation.id}")
            return existingConversation.id
        }

        // Create new conversation
        val conversationId = UUID.randomUUID().toString()
        android.util.Log.d("ConversationRepo", "Creating new conversation with ID: $conversationId")

        // Ensure we have a valid company name
        var validCompanyName = companyName
        if (validCompanyName.isBlank()) {
            // Try to get company name from the company document
            try {
                val companyDoc = db.collection("companies")
                    .document(companyId)
                    .get()
                    .await()

                if (companyDoc.exists()) {
                    validCompanyName = companyDoc.getString("companyName") ?: "Company"
                    android.util.Log.d("ConversationRepo", "Retrieved company name from document: $validCompanyName")
                } else {
                    validCompanyName = "Company"
                    android.util.Log.d("ConversationRepo", "Using default company name: $validCompanyName")
                }
            } catch (e: Exception) {
                validCompanyName = "Company"
                android.util.Log.d("ConversationRepo", "Error getting company name, using default: $validCompanyName")
            }
        }

        val conversation = Conversation(
            id = conversationId,
            applicationId = applicationId,
            jobId = jobId,
            jobTitle = jobTitle,
            candidateId = candidateId,
            candidateName = candidateName,
            companyId = companyId,
            companyName = validCompanyName
        )

        try {
            db.collection("conversations")
                .document(conversationId)
                .set(conversation)
                .await()

            android.util.Log.d("ConversationRepo", "Conversation created successfully")

            // Verify the conversation was created
            val createdConversation = getConversationById(conversationId)
            if (createdConversation != null) {
                android.util.Log.d("ConversationRepo", "Verified conversation exists with companyId=${createdConversation.companyId}")
            } else {
                android.util.Log.e("ConversationRepo", "ERROR: Failed to verify conversation creation")
            }

            return conversationId
        } catch (e: Exception) {
            android.util.Log.e("ConversationRepo", "ERROR creating conversation: ${e.message}")
            throw e
        }
    }

    suspend fun getConversationByApplicationId(applicationId: String): Conversation? {
        android.util.Log.d("ConversationRepo", "Looking for conversation with applicationId: $applicationId")

        val result = db.collection("conversations")
            .whereEqualTo("applicationId", applicationId)
            .get()
            .await()

        if (!result.isEmpty) {
            val conversation = result.documents[0].toObject(Conversation::class.java)
            android.util.Log.d("ConversationRepo", "Found conversation: ${conversation?.id}, companyId=${conversation?.companyId}")
            return conversation
        } else {
            android.util.Log.d("ConversationRepo", "No conversation found for applicationId: $applicationId")
            return null
        }
    }

    suspend fun getConversationById(conversationId: String): Conversation? {
        android.util.Log.d("ConversationRepo", "Getting conversation by ID: $conversationId")

        val document = db.collection("conversations")
            .document(conversationId)
            .get()
            .await()

        if (document.exists()) {
            val conversation = document.toObject(Conversation::class.java)
            android.util.Log.d("ConversationRepo", "Found conversation: companyId=${conversation?.companyId}, candidateId=${conversation?.candidateId}")
            return conversation
        } else {
            android.util.Log.d("ConversationRepo", "No conversation found with ID: $conversationId")
            return null
        }
    }

    suspend fun getUserConversations(userId: String): List<Conversation> {
        // Add logging
        android.util.Log.d("ConversationRepo", "Getting conversations for user: $userId")

        // Get the current user's email
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email

        if (userEmail == null) {
            android.util.Log.d("ConversationRepo", "No email available for current user, returning empty list")
            return emptyList()
        }

        // Check if this is a company user by email
        val companyByEmailSnapshot = db.collection("companies")
            .whereEqualTo("email", userEmail)
            .get()
            .await()

        val isCompanyUser = !companyByEmailSnapshot.isEmpty
        android.util.Log.d("ConversationRepo", "Is company user (by email): $isCompanyUser")

        if (isCompanyUser) {
            // This is a company user, get company conversations
            val companyDoc = companyByEmailSnapshot.documents[0]
            val companyId = companyDoc.id
            val companyName = companyDoc.getString("companyName") ?: "unknown"

            android.util.Log.d("ConversationRepo", "Company user found: id=$companyId, name=$companyName")

            // Update the company document with userId if it doesn't exist
            if (companyDoc.getString("userId") == null) {
                db.collection("companies")
                    .document(companyId)
                    .update("userId", userId)
                    .await()
                android.util.Log.d("ConversationRepo", "Updated company document with userId: $userId")
            }

            // Get conversations where companyId matches the company document ID
            val companyConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()

            android.util.Log.d("ConversationRepo", "Found ${companyConversationsSnapshot.size()} company conversations by companyId")
            val companyConversations = companyConversationsSnapshot.toObjects(Conversation::class.java)

            // Also get conversations where companyId matches the user ID (for backward compatibility)
            val userIdConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("companyId", userId)
                .get()
                .await()

            android.util.Log.d("ConversationRepo", "Found ${userIdConversationsSnapshot.size()} company conversations by userId")
            val userIdConversations = userIdConversationsSnapshot.toObjects(Conversation::class.java)

            // Combine and sort
            val allCompanyConversations = (companyConversations + userIdConversations).distinctBy { it.id }
            android.util.Log.d("ConversationRepo", "Total company conversations: ${allCompanyConversations.size}")

            // Log each conversation for debugging
            allCompanyConversations.forEach {
                android.util.Log.d("ConversationRepo", "Company conversation: ${it.id}, with candidate: ${it.candidateName}")
            }

            return allCompanyConversations.sortedByDescending { it.updatedAt.seconds }
        } else {
            // This is a student user, get candidate conversations
            val candidateConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("candidateId", userId)
                .get()
                .await()

            android.util.Log.d("ConversationRepo", "Found ${candidateConversationsSnapshot.size()} candidate conversations")
            val candidateConversations = candidateConversationsSnapshot.toObjects(Conversation::class.java)

            // For each conversation, ensure we have the correct company name
            val updatedConversations = candidateConversations.map { conversation ->
                // If company name is missing or default, try to get it from the companies collection
                if (conversation.companyName.isBlank() || conversation.companyName == "Company" || conversation.companyName == "unknown") {
                    try {
                        // First try by document ID
                        val companyDoc = db.collection("companies")
                            .document(conversation.companyId)
                            .get()
                            .await()

                        if (companyDoc.exists()) {
                            val companyName = companyDoc.getString("companyName")
                            if (!companyName.isNullOrEmpty()) {
                                android.util.Log.d("ConversationRepo", "Updated company name for conversation ${conversation.id}: $companyName")
                                conversation.copy(companyName = companyName)
                            } else {
                                conversation
                            }
                        } else {
                            // Try by companyId field
                            val companyDocs = db.collection("companies")
                                .whereEqualTo("companyId", conversation.companyId)
                                .get()
                                .await()

                            if (!companyDocs.isEmpty) {
                                val companyName = companyDocs.documents[0].getString("companyName")
                                if (!companyName.isNullOrEmpty()) {
                                    android.util.Log.d("ConversationRepo", "Updated company name for conversation ${conversation.id}: $companyName")
                                    conversation.copy(companyName = companyName)
                                } else {
                                    conversation
                                }
                            } else {
                                conversation
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationRepo", "Error getting company name: ${e.message}")
                        conversation
                    }
                } else {
                    conversation
                }
            }

            // Log each conversation for debugging
            updatedConversations.forEach {
                android.util.Log.d("ConversationRepo", "Candidate conversation: ${it.id}, with company: ${it.companyName}")
            }

            return updatedConversations.sortedByDescending { it.updatedAt.seconds }
        }
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
