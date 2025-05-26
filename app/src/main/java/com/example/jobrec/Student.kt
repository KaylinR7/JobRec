package com.example.jobrec
data class Student(
    val name: String = "",
    val surname: String = "",
    val cellNumber: String = "",
    val email: String = "",
    val address: String = "",
    val experiences: MutableList<StudentExperience> = mutableListOf(),  
    val education: MutableList<StudentEducation> = mutableListOf(),  
    val skills: List<String> = listOf(),
    val hobbies: List<String> = listOf()
)
