package com.example.jobrec

import com.google.firebase.Timestamp

data class Application(
    var id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val userId: String = "",
    val applicantName: String = "",
    val appliedDate: Timestamp = Timestamp.now(),
    val status: String = "pending",
    val coverLetter: String = "",
    val cvUrl: String = "",
    val notes: String = ""
) 