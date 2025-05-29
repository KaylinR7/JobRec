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
        companyName: String,
        candidateInfo: String? = null
    ): String {
        android.util.Log.d("ConversationRepo", "Creating conversation for application: $applicationId")
        android.util.Log.d("ConversationRepo", "Company ID: $companyId, Company Name: $companyName")
        android.util.Log.d("ConversationRepo", "Candidate ID: $candidateId, Candidate Name: $candidateName")
        val existingConversation = getConversationByApplicationId(applicationId)
        if (existingConversation != null) {
            android.util.Log.d("ConversationRepo", "Conversation already exists with ID: ${existingConversation.id}")
            return existingConversation.id
        }
        val conversationId = UUID.randomUUID().toString()
        android.util.Log.d("ConversationRepo", "Creating new conversation with ID: $conversationId")
        var validCompanyName = companyName
        if (validCompanyName.isBlank()) {
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
        var candidateDetails = candidateInfo
        if (candidateDetails.isNullOrBlank()) {
            try {
                val userDoc = db.collection("users")
                    .document(candidateId)
                    .get()
                    .await()
                if (userDoc.exists()) {
                    val skills = userDoc.get("skills") as? List<String>
                    val education = userDoc.get("education") as? List<Map<String, Any>>
                    val experience = userDoc.get("experience") as? List<Map<String, Any>>
                    val infoBuilder = StringBuilder()
                    val highestEducation = education?.maxByOrNull {
                        (it["degree"] as? String)?.length ?: 0
                    }
                    highestEducation?.let {
                        val degree = it["degree"] as? String
                        val institution = it["institution"] as? String
                        if (!degree.isNullOrBlank()) {
                            infoBuilder.append(degree)
                            if (!institution.isNullOrBlank()) {
                                infoBuilder.append(" at $institution")
                            }
                        }
                    }
                    if (!skills.isNullOrEmpty() && skills.isNotEmpty()) {
                        if (infoBuilder.isNotEmpty()) infoBuilder.append(" • ")
                        infoBuilder.append("Skills: ${skills.take(3).joinToString(", ")}")
                        if (skills.size > 3) infoBuilder.append("...")
                    }
                    val totalExperience = experience?.sumOf { exp ->
                        val startDate = (exp["startDate"] as? com.google.firebase.Timestamp)?.toDate()
                        val endDate = (exp["endDate"] as? com.google.firebase.Timestamp)?.toDate()
                        if (startDate != null && endDate != null) {
                            val diff = endDate.time - startDate.time
                            (diff / (1000L * 60 * 60 * 24 * 365)).toInt()
                        } else {
                            0
                        }
                    } ?: 0
                    if (totalExperience > 0) {
                        if (infoBuilder.isNotEmpty()) infoBuilder.append(" • ")
                        infoBuilder.append("$totalExperience years experience")
                    }
                    candidateDetails = infoBuilder.toString()
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationRepo", "Error getting candidate info: ${e.message}")
            }
        }
        var validCandidateName = candidateName
        if (validCandidateName.isBlank() || validCandidateName == "!!!!!" || validCandidateName == "Student") {
            try {
                val userDoc = db.collection("users")
                    .document(candidateId)
                    .get()
                    .await()
                if (userDoc.exists()) {
                    val name = userDoc.getString("name") ?: ""
                    val surname = userDoc.getString("surname") ?: ""
                    android.util.Log.d("ConversationRepo", "Retrieved candidate data for new conversation - name: '$name', surname: '$surname'")
                    validCandidateName = when {
                        name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                        name.isNotEmpty() -> name
                        surname.isNotEmpty() -> surname
                        else -> "Student"
                    }
                    android.util.Log.d("ConversationRepo", "Using candidate name for new conversation: $validCandidateName")
                } else {
                    validCandidateName = "Student"
                    android.util.Log.d("ConversationRepo", "User document not found, using default candidate name: $validCandidateName")
                }
            } catch (e: Exception) {
                validCandidateName = "Student"
                android.util.Log.e("ConversationRepo", "Error getting candidate name for new conversation, using default: $validCandidateName", e)
            }
        }
        val conversation = Conversation(
            id = conversationId,
            applicationId = applicationId,
            jobId = jobId,
            jobTitle = jobTitle,
            candidateId = candidateId,
            candidateName = validCandidateName,
            candidateInfo = candidateDetails,
            companyId = companyId,
            companyName = validCompanyName
        )
        try {
            db.collection("conversations")
                .document(conversationId)
                .set(conversation)
                .await()
            android.util.Log.d("ConversationRepo", "Conversation created successfully")
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
        android.util.Log.d("ConversationRepo", "Getting conversations for user: $userId")
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email
        if (userEmail == null) {
            android.util.Log.d("ConversationRepo", "No email available for current user, returning empty list")
            return emptyList()
        }
        val companyByEmailSnapshot = db.collection("companies")
            .whereEqualTo("email", userEmail)
            .get()
            .await()
        val isCompanyUser = !companyByEmailSnapshot.isEmpty
        android.util.Log.d("ConversationRepo", "Is company user (by email): $isCompanyUser")
        if (isCompanyUser) {
            val companyDoc = companyByEmailSnapshot.documents[0]
            val companyId = companyDoc.id
            val companyName = companyDoc.getString("companyName") ?: "unknown"
            android.util.Log.d("ConversationRepo", "Company user found: id=$companyId, name=$companyName")
            if (companyDoc.getString("userId") == null) {
                db.collection("companies")
                    .document(companyId)
                    .update("userId", userId)
                    .await()
                android.util.Log.d("ConversationRepo", "Updated company document with userId: $userId")
            }
            val companyConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
            android.util.Log.d("ConversationRepo", "Found ${companyConversationsSnapshot.size()} company conversations by companyId")
            val companyConversations = companyConversationsSnapshot.toObjects(Conversation::class.java)
            val userIdConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("companyId", userId)
                .get()
                .await()
            android.util.Log.d("ConversationRepo", "Found ${userIdConversationsSnapshot.size()} company conversations by userId")
            val userIdConversations = userIdConversationsSnapshot.toObjects(Conversation::class.java)
            val allCompanyConversations = (companyConversations + userIdConversations).distinctBy { it.id }
            android.util.Log.d("ConversationRepo", "Total company conversations: ${allCompanyConversations.size}")
            val updatedCompanyConversations = allCompanyConversations.map { conversation ->
                val needsNameUpdate = conversation.candidateName.isBlank() ||
                                     conversation.candidateName == "!!!!!" ||
                                     conversation.candidateName == "Company" ||
                                     conversation.candidateName == "Student" ||
                                     conversation.candidateName == "nasty juice"
                if (needsNameUpdate) {
                    android.util.Log.d("ConversationRepo", "Conversation ${conversation.id} has invalid candidate name: '${conversation.candidateName}', attempting to fix")
                }
                try {
                    val userDoc = db.collection("users")
                        .document(conversation.candidateId)
                        .get()
                        .await()
                    if (userDoc.exists()) {
                        val name = userDoc.getString("name") ?: ""
                        val surname = userDoc.getString("surname") ?: ""
                        android.util.Log.d("ConversationRepo", "Retrieved candidate data - name: '$name', surname: '$surname'")
                        val fullName = when {
                            name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
                            name.isNotEmpty() -> name
                            surname.isNotEmpty() -> surname
                            else -> ""
                        }
                        if (fullName.isNotEmpty()) {
                            android.util.Log.d("ConversationRepo", "Using name from user document: $fullName")
                            if (needsNameUpdate || fullName != conversation.candidateName) {
                                try {
                                    android.util.Log.d("ConversationRepo", "Updating conversation ${conversation.id} candidateName from '${conversation.candidateName}' to '$fullName'")
                                    db.collection("conversations")
                                        .document(conversation.id)
                                        .update("candidateName", fullName)
                                        .await()
                                    android.util.Log.d("ConversationRepo", "Updated candidateName in Firestore: $fullName")
                                } catch (e: Exception) {
                                    android.util.Log.e("ConversationRepo", "Error updating candidateName in Firestore", e)
                                }
                            }
                            return@map conversation.copy(candidateName = fullName)
                        }
                    }
                    val appSnapshot = db.collection("applications")
                        .whereEqualTo("userId", conversation.candidateId)
                        .get()
                        .await()
                    if (!appSnapshot.isEmpty) {
                        val appDoc = appSnapshot.documents[0]
                        val applicantName = appDoc.getString("applicantName")
                        if (!applicantName.isNullOrEmpty() && applicantName != "!!!!!" && applicantName != "Company") {
                            android.util.Log.d("ConversationRepo", "Using applicant name from application: $applicantName")
                            try {
                                db.collection("conversations")
                                    .document(conversation.id)
                                    .update("candidateName", applicantName)
                                    .await()
                                android.util.Log.d("ConversationRepo", "Updated candidateName in Firestore from application: $applicantName")
                            } catch (e: Exception) {
                                android.util.Log.e("ConversationRepo", "Error updating candidateName from application", e)
                            }
                            return@map conversation.copy(candidateName = applicantName)
                        }
                    }
                    val defaultName = "Student"
                    if (needsNameUpdate) {
                        try {
                            db.collection("conversations")
                                .document(conversation.id)
                                .update("candidateName", defaultName)
                                .await()
                            android.util.Log.d("ConversationRepo", "Updated candidateName in Firestore with default: $defaultName")
                        } catch (e: Exception) {
                            android.util.Log.e("ConversationRepo", "Error updating candidateName with default", e)
                        }
                    }
                    return@map conversation.copy(candidateName = defaultName)
                } catch (e: Exception) {
                    android.util.Log.e("ConversationRepo", "Error getting candidate name: ${e.message}")
                    if (needsNameUpdate) {
                        val defaultName = "Student"
                        try {
                            db.collection("conversations")
                                .document(conversation.id)
                                .update("candidateName", defaultName)
                                .await()
                        } catch (ex: Exception) {
                            android.util.Log.e("ConversationRepo", "Error updating candidateName with default after error: ${ex.message}")
                        }
                        return@map conversation.copy(candidateName = defaultName)
                    }
                    return@map conversation
                }
            }
            updatedCompanyConversations.forEach {
                android.util.Log.d("ConversationRepo", "Company conversation: ${it.id}, with candidate: ${it.candidateName}")
            }
            return updatedCompanyConversations.sortedByDescending { it.updatedAt.seconds }
        } else {
            val candidateConversationsSnapshot = db.collection("conversations")
                .whereEqualTo("candidateId", userId)
                .get()
                .await()
            android.util.Log.d("ConversationRepo", "Found ${candidateConversationsSnapshot.size()} candidate conversations")
            val candidateConversations = candidateConversationsSnapshot.toObjects(Conversation::class.java)
            val updatedConversations = candidateConversations.map { conversation ->
                if (conversation.companyName.isBlank() || conversation.companyName == "Company" || conversation.companyName == "unknown") {
                    try {
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

        // Get conversation details to populate job and company information
        val conversation = getConversationById(conversationId)

        // Get sender name
        val senderName = try {
            val senderDoc = db.collection("companies").document(senderId).get().await()
            if (senderDoc.exists()) {
                senderDoc.getString("companyName") ?: "Company"
            } else {
                val userDoc = db.collection("users").document(senderId).get().await()
                userDoc.getString("name") ?: "User"
            }
        } catch (e: Exception) {
            android.util.Log.e("ConversationRepo", "Error getting sender name", e)
            "Company"
        }

        val interviewDetails = InterviewDetails(
            date = date,
            time = time,
            duration = duration,
            type = type,
            location = location,
            meetingLink = meetingLink,
            jobTitle = conversation?.jobTitle,
            companyName = conversation?.companyName,
            jobId = conversation?.jobId,
            companyId = conversation?.companyId
        )
        val message = Message(
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = "Meeting invitation",
            type = "meeting_invite",
            interviewDetails = interviewDetails,
            senderName = senderName
        )
        val messageId = messageRepository.sendMessage(message)
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
        val unreadMessages = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
            .documents
        for (message in unreadMessages) {
            db.collection("messages")
                .document(message.id)
                .update("isRead", true)
                .await()
        }
        db.collection("conversations")
            .document(conversationId)
            .update("unreadCount", 0)
            .await()
    }
}
