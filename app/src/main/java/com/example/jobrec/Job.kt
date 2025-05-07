package com.example.jobrec

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp
import java.util.*

data class Job(
    val id: String = "",
    val title: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val location: String = "",
    val salary: String = "",
    val type: String = "",
    val jobType: String = "",
    val description: String = "",
    val requirements: String = "",
    val postedDate: Timestamp = Timestamp.now(),
    val status: String = "active"
) {
    @Exclude
    fun getRequirementsList(): List<String> {
        return requirements.split("\n").filter { it.isNotEmpty() }
    }

    @Exclude
    fun getPostedDateMillis(): Long {
        return postedDate.toDate().time
    }
} 