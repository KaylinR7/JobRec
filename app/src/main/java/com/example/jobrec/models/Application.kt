package com.example.jobrec.models

import com.google.firebase.Timestamp

data class Application(
    val id: String = "",
    val jobId: String = "",
    val candidateId: String = "",
    val companyId: String = "",
    val status: String = "applied", // applied, reviewed, shortlisted, interviewed, offered, rejected
    val appliedAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val documents: List<ApplicationDocument> = emptyList(),
    val interviews: List<Interview> = emptyList(),
    val feedback: ApplicationFeedback? = null,
    var jobTitle: String? = null,
    var candidateName: String? = null,
    var companyName: String? = null,
    val timestamp: Timestamp = Timestamp.now() // For backward compatibility
) {
    data class ApplicationDocument(
        val id: String = "",
        val type: String = "", // resume, cover_letter, portfolio, other
        val name: String = "",
        val url: String = "",
        val uploadedAt: Timestamp = Timestamp.now()
    )

    data class Interview(
        val id: String = "",
        val date: Timestamp,
        val time: String,
        val duration: Int, // in minutes
        val type: String, // online, in-person
        val location: String? = null,
        val meetingLink: String? = null,
        val status: String = "scheduled", // scheduled, completed, cancelled
        val feedback: InterviewFeedback? = null
    )

    data class InterviewFeedback(
        val rating: Int = 0,
        val comments: String = "",
        val strengths: List<String> = emptyList(),
        val areasForImprovement: List<String> = emptyList(),
        val submittedAt: Timestamp = Timestamp.now()
    )

    data class ApplicationFeedback(
        val overallRating: Int = 0,
        val comments: String = "",
        val strengths: List<String> = emptyList(),
        val areasForImprovement: List<String> = emptyList(),
        val submittedAt: Timestamp = Timestamp.now()
    )
}