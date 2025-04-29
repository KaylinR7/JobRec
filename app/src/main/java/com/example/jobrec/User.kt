package com.example.jobrec

data class User(
    val id: String = "",
    val idNumber: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val summary: String = "",
    val skills: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val languages: List<Language> = emptyList(),
    val references: List<Reference> = emptyList(),
    val profileImageUrl: String? = null,
    val role: String = "user",
    val achievements: String = "",
    val linkedin: String = "",
    val github: String = "",
    val portfolio: String = ""
)

data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

data class Experience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

data class Language(
    val name: String = "",
    val proficiency: String = "" // Basic, Intermediate, Advanced, Native
)

data class Reference(
    val name: String = "",
    val position: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = ""
) 