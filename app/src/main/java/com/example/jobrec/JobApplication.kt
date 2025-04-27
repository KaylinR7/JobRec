package com.example.jobrec

import java.util.Date

data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val userId: String = "",
    val applicantName: String = "",
    val companyId: String = "",
    val appliedDate: Date = Date(),
    val status: String = "Pending",
    val cvUrl: String = "",
    val coverLetter: String = ""
)

enum class ApplicationStatus {
    PENDING,
    REVIEWING,
    SHORTLISTED,
    INTERVIEWING,
    OFFERED,
    REJECTED
} 