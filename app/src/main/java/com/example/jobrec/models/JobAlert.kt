package com.example.jobrec.models

import com.google.firebase.Timestamp

data class JobAlert(
    val id: String = "",
    val userId: String = "",
    val keywords: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val jobTypes: List<String> = emptyList(), // full-time, part-time, contract, etc.
    val salaryRange: SalaryRange? = null,
    val experienceLevel: String? = null, // entry, mid, senior
    val educationLevel: String? = null, // bachelor, master, phd
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
    val notificationFrequency: String = "daily", // daily, weekly, immediate
    val quietHours: QuietHours? = null
)

data class QuietHours(
    val startHour: Int, // 0-23
    val endHour: Int, // 0-23
    val timezone: String = "UTC"
) 