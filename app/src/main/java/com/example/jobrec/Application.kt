package com.example.jobrec

data class Application(
    val id: String = "",
    val jobTitle: String = "",
    val applicantName: String = "",
    val appliedDate: String = "",
    val status: String = "pending"
) 