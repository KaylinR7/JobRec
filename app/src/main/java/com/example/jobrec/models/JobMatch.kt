package com.example.jobrec.models

import com.example.jobrec.Job

data class JobMatch(
    val job: Job,
    val matchPercentage: Int,
    val matchReasoning: String,
    val skillsMatch: Int = 0,
    val experienceMatch: Int = 0,
    val educationMatch: Int = 0,
    val locationMatch: Int = 0
)

data class MatchCriteria(
    val skills: List<String>,
    val experience: List<String>,
    val education: List<String>,
    val summary: String,
    val province: String,
    val city: String,
    val field: String,
    val subField: String,
    val yearsOfExperience: String,
    val expectedSalary: String
)
