package com.example.jobrec

data class User(
    val idNumber: String = "",
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val summary: String = "",
    val skills: List<String> = emptyList(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val profileImageUrl: String? = null
) 