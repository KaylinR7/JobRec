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

        // First, check if there are any conversations with numeric companyId and fix them
        val allConversationsCheck = db.collection("conversations")
            .get()
            .await()

        for (doc in allConversationsCheck.documents) {
            val companyId = doc.getString("companyId") ?: continue

            // If companyId is numeric, update it to current user ID
            if (companyId.matches(Regex("\\d+"))) {
                android.util.Log.d("ConversationRepo", "Auto-fixing: Found numeric companyId $companyId in conversation ${doc.id}")

                // Check if this user is a company
                val isCompany = db.collection("companies")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .size() > 0

                if (isCompany) {
                    android.util.Log.d("ConversationRepo", "Auto-fixing: User is a company, updating conversation ${doc.id}")

                    // Update the conversation with current user ID
                    db.collection("conversations")
                        .document(doc.id)
                        .update("companyId", userId)
                        .await()

                    android.util.Log.d("ConversationRepo", "Auto-fixing: Successfully updated conversation ${doc.id}")
                }
            }
        }

        // Get conversations where user is the candidate (without sorting)
        val candidateConversationsSnapshot = db.collection("conversations")
            .whereEqualTo("candidateId", userId)
            .get()
            .await()

        android.util.Log.d("ConversationRepo", "Found ${candidateConversationsSnapshot.size()} candidate conversations")

        val candidateConversations = candidateConversationsSnapshot.toObjects(Conversation::class.java)

        // Get conversations where user is the company (without sorting)
        android.util.Log.d("ConversationRepo", "Querying for company conversations with companyId = $userId")

        val companyConversationsSnapshot = db.collection("conversations")
            .whereEqualTo("companyId", userId)
            .get()
            .await()

        android.util.Log.d("ConversationRepo", "Found ${companyConversationsSnapshot.size()} company conversations")

        // Check if we need to look for company conversations with a different ID
        var additionalCompanyConversations = emptyList<Conversation>()
        if (companyConversationsSnapshot.isEmpty) {
            // Try to find the company's custom ID
            val companyDoc = db.collection("companies")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (!companyDoc.isEmpty) {
                val companyId = companyDoc.documents[0].getString("companyId")
                if (companyId != null) {
                    android.util.Log.d("ConversationRepo", "Found company with custom ID: $companyId, trying to find conversations")

                    val additionalSnapshot = db.collection("conversations")
                        .whereEqualTo("companyId", companyId)
                        .get()
                        .await()

                    android.util.Log.d("ConversationRepo", "Found ${additionalSnapshot.size()} additional company conversations with custom ID")
                    additionalCompanyConversations = additionalSnapshot.toObjects(Conversation::class.java)
                }
            }

            // Also check if this user is a candidate in any conversations
            val candidateConversationsCheck = db.collection("conversations")
                .whereEqualTo("candidateId", userId)
                .get()
                .await()

            if (!candidateConversationsCheck.isEmpty) {
                android.util.Log.d("ConversationRepo", "User is a candidate in ${candidateConversationsCheck.size()} conversations")

                // For each conversation, check if we need to update the company ID
                for (doc in candidateConversationsCheck.documents) {
                    val companyId = doc.getString("companyId") ?: continue

                    // If companyId is numeric, it might be a legacy ID
                    if (companyId.matches(Regex("\\d+"))) {
                        android.util.Log.d("ConversationRepo", "Found numeric company ID: $companyId in conversation ${doc.id}")

                        // Try to find any company and use its userId
                        val anyCompanyDoc = db.collection("companies")
                            .limit(1)
                            .get()
                            .await()

                        if (!anyCompanyDoc.isEmpty) {
                            val firstCompany = anyCompanyDoc.documents[0]
                            val companyUserId = firstCompany.getString("userId")
                            if (companyUserId != null) {
                                android.util.Log.d("ConversationRepo", "Updating conversation ${doc.id} with company userId: $companyUserId")

                                // Update the conversation
                                db.collection("conversations")
                                    .document(doc.id)
                                    .update("companyId", companyUserId)
                                    .await()

                                android.util.Log.d("ConversationRepo", "Successfully updated conversation ${doc.id}")
                            }
                        }
                    }
                }
            }
        }

        // Log all conversations in the database for debugging
        val allConversationsSnapshot = db.collection("conversations")
            .get()
            .await()

        android.util.Log.d("ConversationRepo", "Total conversations in database: ${allConversationsSnapshot.size()}")
        allConversationsSnapshot.forEach { doc ->
            val companyId = doc.getString("companyId") ?: "null"
            val candidateId = doc.getString("candidateId") ?: "null"
            android.util.Log.d("ConversationRepo", "Conversation ${doc.id}: companyId=$companyId, candidateId=$candidateId")
        }

        val companyConversations = companyConversationsSnapshot.toObjects(Conversation::class.java) + additionalCompanyConversations

        // Log each conversation for debugging
        candidateConversations.forEach {
            android.util.Log.d("ConversationRepo", "Candidate conversation: ${it.id}, with company: ${it.companyName}")
        }

        companyConversations.forEach {
            android.util.Log.d("ConversationRepo", "Company conversation: ${it.id}, with candidate: ${it.candidateName}")
        }

        // Combine and sort in memory
        val allConversations = (candidateConversations + companyConversations)
        android.util.Log.d("ConversationRepo", "Total conversations: ${allConversations.size}")

        return allConversations.sortedByDescending { it.updatedAt.seconds }
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
