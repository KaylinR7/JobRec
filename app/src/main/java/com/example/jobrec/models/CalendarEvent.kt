package com.example.jobrec.models

import com.google.firebase.Timestamp

data class CalendarEvent(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: Timestamp = Timestamp.now(),
    val time: String = "",
    val duration: Int = 60, // Duration in minutes
    val meetingType: String = "online", // "online" or "in-person"
    val location: String? = null, // For in-person meetings
    val meetingLink: String? = null, // For online meetings
    val notes: String = "",
    val isInterview: Boolean = false, // True if this is an interview event
    val jobId: String? = null, // Associated job ID if it's an interview
    val companyId: String? = null, // Associated company ID if it's an interview
    val status: String = "scheduled", // "pending", "scheduled", "completed", "cancelled"
    val invitationMessageId: String? = null, // Reference to the invitation message
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
