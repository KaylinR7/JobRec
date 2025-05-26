package com.example.jobrec.models
import com.google.firebase.Timestamp
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: String = "text", 
    val fileUrl: String? = null,
    val fileName: String? = null,
    val interviewDetails: InterviewDetails? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)
data class InterviewDetails(
    val date: Timestamp = Timestamp.now(),
    val time: String = "",
    val duration: Int = 60, 
    val type: String = "online", 
    val location: String? = null,
    val meetingLink: String? = null,
    val status: String = "pending" 
)