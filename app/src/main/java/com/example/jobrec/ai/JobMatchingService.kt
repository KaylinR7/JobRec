package com.example.jobrec.ai

import android.util.Log
import com.example.jobrec.BuildConfig
import com.example.jobrec.chatbot.HuggingFaceService
import com.example.jobrec.Job
import com.example.jobrec.models.JobMatch
import com.example.jobrec.models.MatchCriteria
import com.example.jobrec.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobMatchingService {
    private val huggingFaceService = HuggingFaceService()
    private val TAG = "JobMatchingService"

    private val huggingFaceToken: String
        get() = BuildConfig.HUGGING_FACE_TOKEN

    suspend fun calculateJobMatch(user: User, job: Job): JobMatch {
        return withContext(Dispatchers.IO) {
            try {
                val matchCriteria = extractMatchCriteria(user)
                val aiMatchResult = getAIMatchAnalysis(matchCriteria, job)
                val ruleBasedMatch = calculateRuleBasedMatch(user, job)

                val finalPercentage = (aiMatchResult.first + ruleBasedMatch) / 2
                val reasoning = aiMatchResult.second

                JobMatch(
                    job = job.copy(
                        matchPercentage = finalPercentage,
                        matchReasoning = reasoning
                    ),
                    matchPercentage = finalPercentage,
                    matchReasoning = reasoning,
                    skillsMatch = calculateSkillsMatch(user.skills, job.requirements),
                    experienceMatch = calculateExperienceMatch(user.experience, job.experienceLevel),
                    educationMatch = calculateEducationMatch(user.education, job.jobField),
                    locationMatch = calculateLocationMatch(user.province, job.province)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating job match", e)
                val fallbackMatch = calculateRuleBasedMatch(user, job)
                JobMatch(
                    job = job.copy(
                        matchPercentage = fallbackMatch,
                        matchReasoning = "Match calculated using profile analysis"
                    ),
                    matchPercentage = fallbackMatch,
                    matchReasoning = "Match calculated using profile analysis"
                )
            }
        }
    }

    private fun extractMatchCriteria(user: User): MatchCriteria {
        return MatchCriteria(
            skills = user.skills,
            experience = user.experience.map { "${it.position} at ${it.company} (${it.startDate} - ${it.endDate})" },
            education = user.education.map { "${it.degree} in ${it.fieldOfStudy} from ${it.institution}" },
            summary = user.summary,
            location = user.province
        )
    }

    private suspend fun getAIMatchAnalysis(criteria: MatchCriteria, job: Job): Pair<Int, String> {
        return try {
            val prompt = buildMatchPrompt(criteria, job)
            val response = huggingFaceService.generateResponse(prompt, huggingFaceToken)
            parseAIResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed, using fallback", e)
            Pair(50, "Unable to perform detailed AI analysis")
        }
    }

    private fun buildMatchPrompt(criteria: MatchCriteria, job: Job): String {
        return """
            Analyze job compatibility and provide a match percentage (0-100) and brief reasoning.

            Candidate Profile:
            - Skills: ${criteria.skills.joinToString(", ")}
            - Experience: ${criteria.experience.joinToString("; ")}
            - Education: ${criteria.education.joinToString("; ")}
            - Summary: ${criteria.summary}
            - Location: ${criteria.location}

            Job Requirements:
            - Title: ${job.title}
            - Field: ${job.jobField}
            - Specialization: ${job.specialization}
            - Experience Level: ${job.experienceLevel}
            - Requirements: ${job.requirements}
            - Location: ${job.province}

            Respond with: "MATCH: [percentage]% - [brief reasoning]"
        """.trimIndent()
    }

    private fun parseAIResponse(response: String): Pair<Int, String> {
        return try {
            val matchRegex = """MATCH:\s*(\d+)%\s*-\s*(.+)""".toRegex()
            val matchResult = matchRegex.find(response)

            if (matchResult != null) {
                val percentage = matchResult.groupValues[1].toInt().coerceIn(0, 100)
                val reasoning = matchResult.groupValues[2].trim()
                Pair(percentage, reasoning)
            } else {
                val percentageRegex = """(\d+)%""".toRegex()
                val percentageMatch = percentageRegex.find(response)
                val percentage = percentageMatch?.groupValues?.get(1)?.toInt()?.coerceIn(0, 100) ?: 50
                Pair(percentage, "AI analysis completed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            Pair(50, "Analysis completed with standard matching")
        }
    }

    private fun calculateRuleBasedMatch(user: User, job: Job): Int {
        val skillsScore = calculateSkillsMatch(user.skills, job.requirements)
        val experienceScore = calculateExperienceMatch(user.experience, job.experienceLevel)
        val educationScore = calculateEducationMatch(user.education, job.jobField)
        val locationScore = calculateLocationMatch(user.province, job.province)

        // Weighted calculation - skills are most important
        val finalScore = ((skillsScore * 0.7) + (experienceScore * 0.15) + (educationScore * 0.1) + (locationScore * 0.05)).toInt()

        return finalScore.coerceAtMost(100)
    }

    private fun calculateSkillsMatch(userSkills: List<String>, jobRequirements: String): Int {
        if (userSkills.isEmpty()) return 20
        if (jobRequirements.isEmpty()) return 40

        val requirements = jobRequirements.lowercase()
        var matchCount = 0
        var totalSkills = userSkills.size

        // Simple direct matching
        for (skill in userSkills) {
            val skillLower = skill.lowercase().trim()
            if (skillLower.isEmpty()) continue

            // Check for direct mentions of the skill
            if (requirements.contains(skillLower)) {
                matchCount++
            }
        }

        // Calculate percentage based on how many skills matched
        val matchPercentage = if (totalSkills > 0) {
            (matchCount.toDouble() / totalSkills * 100).toInt()
        } else 20

        // Return score based on match percentage
        return when {
            matchPercentage >= 50 -> 85 + (matchPercentage - 50) / 5 // 85-95 range
            matchPercentage >= 25 -> 60 + matchPercentage // 60-85 range
            matchPercentage > 0 -> 40 + matchPercentage // 40-65 range
            else -> 25 // No matches
        }.coerceAtMost(100)
    }

    private fun extractJobTitle(requirements: String): String {
        // Try to extract job context from requirements
        val commonTitles = listOf("developer", "engineer", "designer", "manager", "analyst", "tester", "consultant")
        return commonTitles.find { requirements.contains(it) } ?: ""
    }

    private fun isCompleteMismatch(userSkills: List<String>, jobRequirements: String): Boolean {
        val userIsTechnical = userSkills.any { isTechnicalSkill(it.lowercase()) }
        val jobIsTechnical = isTechnicalJob(jobRequirements)

        // If user is clearly technical but job is clearly non-technical (or vice versa)
        return (userIsTechnical && !jobIsTechnical && !jobRequirements.contains("technical")) ||
               (!userIsTechnical && jobIsTechnical)
    }

    private fun isTechnicalSkill(skill: String): Boolean {
        val technicalSkills = listOf("programming", "coding", "development", "software", "web", "mobile", "database", "api", "framework", "library")
        return technicalSkills.any { skill.contains(it) }
    }

    private fun isTechnicalJob(requirements: String): Boolean {
        val technicalKeywords = listOf("programming", "coding", "development", "software", "web", "mobile", "database", "api", "technical")
        return technicalKeywords.any { requirements.contains(it) }
    }

    private fun isRelatedSkill(skill: String, requirements: String): Boolean {
        val relatedSkills = mapOf(
            "java" to listOf("spring", "android", "kotlin", "programming"),
            "kotlin" to listOf("java", "android", "programming"),
            "python" to listOf("django", "flask", "data", "programming"),
            "javascript" to listOf("react", "node", "web", "frontend", "backend"),
            "react" to listOf("javascript", "frontend", "web"),
            "sql" to listOf("database", "mysql", "postgresql", "data"),
            "html" to listOf("css", "web", "frontend"),
            "css" to listOf("html", "web", "frontend", "design")
        )

        return relatedSkills[skill]?.any { related ->
            requirements.contains(related)
        } ?: false
    }

    private fun calculateExperienceMatch(userExperience: List<com.example.jobrec.Experience>, jobExperienceLevel: String): Int {
        val totalYears = userExperience.sumOf { parseExperienceYears(it.startDate, it.endDate) }
        val experienceLevelLower = jobExperienceLevel.lowercase()

        return when {
            // Entry level
            experienceLevelLower.contains("entry") || experienceLevelLower.contains("junior") -> {
                if (totalYears <= 2) 90 else 75
            }
            // Mid level
            experienceLevelLower.contains("mid") || experienceLevelLower.contains("intermediate") -> {
                if (totalYears in 2..5) 90 else if (totalYears < 2) 60 else 80
            }
            // Senior level
            experienceLevelLower.contains("senior") || experienceLevelLower.contains("lead") -> {
                if (totalYears >= 5) 90 else if (totalYears >= 2) 70 else 50
            }
            // No specific level
            else -> 75
        }
    }

    private fun parseExperienceYears(startDate: String, endDate: String): Int {
        return try {
            val startYear = startDate.substringAfterLast("/").toIntOrNull() ?:
                           startDate.substringAfterLast("-").toIntOrNull() ?:
                           java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

            val endYear = if (endDate.lowercase().contains("present") || endDate.lowercase().contains("current") || endDate.isEmpty()) {
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            } else {
                endDate.substringAfterLast("/").toIntOrNull() ?:
                endDate.substringAfterLast("-").toIntOrNull() ?:
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            }

            kotlin.math.max(1, endYear - startYear)
        } catch (e: Exception) {
            1
        }
    }

    private fun calculateEducationMatch(userEducation: List<com.example.jobrec.Education>, jobField: String): Int {
        if (userEducation.isEmpty()) return 50
        if (jobField.isEmpty()) return 70

        val jobFieldLower = jobField.lowercase()

        for (education in userEducation) {
            val fieldOfStudy = education.fieldOfStudy.lowercase()

            // Simple field matching
            if (fieldOfStudy.contains(jobFieldLower) || jobFieldLower.contains(fieldOfStudy)) {
                return 90 // Good match
            }
        }

        return 60 // Has education but not directly related
    }

    private fun isRelatedField(userField: String, jobField: String): Boolean {
        val relatedFields = mapOf(
            "computer science" to listOf("software", "programming", "technology", "it", "development"),
            "information technology" to listOf("software", "programming", "computer", "development"),
            "software engineering" to listOf("programming", "development", "technology", "computer"),
            "business" to listOf("management", "marketing", "finance", "administration"),
            "engineering" to listOf("technical", "development", "technology"),
            "design" to listOf("ui", "ux", "graphic", "creative", "visual")
        )

        return relatedFields.entries.any { (field, related) ->
            (userField.contains(field) && related.any { jobField.contains(it) }) ||
            (jobField.contains(field) && related.any { userField.contains(it) })
        }
    }

    private fun isTechnicalField(field: String): Boolean {
        val technicalKeywords = listOf("software", "programming", "development", "technology", "it", "computer", "engineering")
        return technicalKeywords.any { field.contains(it) }
    }

    private fun isTechnicalDegree(degree: String): Boolean {
        val technicalDegrees = listOf("computer", "software", "engineering", "technology", "information", "mathematics", "science")
        return technicalDegrees.any { degree.contains(it) }
    }

    private fun calculateLocationMatch(userLocation: String, jobLocation: String): Int {
        if (userLocation.isEmpty() || jobLocation.isEmpty()) return 80

        val userLoc = userLocation.lowercase().trim()
        val jobLoc = jobLocation.lowercase().trim()

        return when {
            userLoc == jobLoc -> 100
            jobLoc.contains("remote") -> 95
            userLoc.contains(jobLoc) || jobLoc.contains(userLoc) -> 85
            else -> 70
        }
    }
}
