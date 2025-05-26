package com.example.jobrec.models
import com.google.firebase.Timestamp
data class RecentActivity(
    val id: String = "",
    val companyId: String = "",
    val type: String = "", 
    val title: String = "",
    val description: String = "",
    val relatedId: String = "", 
    val relatedName: String = "", 
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)
