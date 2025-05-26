package com.example.jobrec.models
import com.google.firebase.Timestamp
data class JobAlert(
    val id: String = "",
    val userId: String = "",
    val keywords: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val jobTypes: List<String> = emptyList(), 
    val salaryRange: SalaryRange? = null,
    val experienceLevel: String? = null, 
    val educationLevel: String? = null, 
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val lastNotified: Timestamp? = null
)
data class SalaryRange(
    val min: Int,
    val max: Int,
    val currency: String = "USD"
)
data class NotificationPreference(
    val userId: String = "",
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val notificationFrequency: String = "daily", 
    val quietHours: QuietHours? = null
)
data class QuietHours(
    val startHour: Int, 
    val endHour: Int, 
    val timezone: String = "UTC"
) 