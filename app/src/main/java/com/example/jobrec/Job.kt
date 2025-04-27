package com.example.jobrec

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp

data class Job(
    val id: String = "",
    val title: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val location: String = "",
    val salary: String = "",
    val type: String = "",
    val description: String = "",
    val requirements: String = "",
    val postedDate: Timestamp = Timestamp.now()
) {
    @Exclude
    fun getRequirementsList(): List<String> {
        return when (requirements) {
            is String -> listOf(requirements as String)
            is List<*> -> (requirements as List<*>).filterIsInstance<String>()
            else -> emptyList()
        }
    }

    @Exclude
    fun getPostedDateMillis(): Long {
        return postedDate.toDate().time
    }
} 