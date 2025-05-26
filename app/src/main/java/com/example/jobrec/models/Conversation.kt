package com.example.jobrec.models

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val applicationId: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val candidateId: String = "",
    val candidateName: String = "",
    val candidateInfo: String? = null, // Additional candidate profile information
    val companyId: String = "",
    val companyName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp = Timestamp.now(),
    val lastMessageSender: String = "",
    val unreadCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val status: String = "active" // active, archived
)
