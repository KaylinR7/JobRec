package com.example.jobrec.chatbot
import com.google.firebase.Timestamp
import java.util.UUID
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String = "",
    val isFromUser: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)
