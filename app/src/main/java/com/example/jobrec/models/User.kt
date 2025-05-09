package com.example.jobrec.models

data class User(
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val field: String = "",
    val specialization: String = "",
    val certificate: String = "",
    val graduationYear: String = "",
    val experience: String = "",
    val language: String = "",
    val summary: String = "",
    val skills: List<String> = listOf(),
    val linkedin: String = "",
    val github: String = "",
    val portfolio: String = "",
    val role: String = "",
    val languages: List<String> = listOf(),
    val education: List<Education> = listOf(),
    val workExperience: List<Experience> = listOf(),
    val preferredIndustry: String = ""
)

data class Education(
    val degree: String = "",
    val institution: String = "",
    val graduationYear: String = ""
)

data class Experience(
    val title: String = "",
    val company: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = ""
)
