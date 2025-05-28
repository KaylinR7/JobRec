package com.example.jobrec.ai

import android.util.Log
import com.example.jobrec.services.GeminiJobMatchingService
import com.example.jobrec.Job
import com.example.jobrec.models.JobMatch
import com.example.jobrec.models.MatchCriteria
import com.example.jobrec.models.LocationData
import com.example.jobrec.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobMatchingService {
    private val geminiService = GeminiJobMatchingService()
    private val TAG = "JobMatchingService"

    suspend fun calculateJobMatch(user: User, job: Job): JobMatch {
        return withContext(Dispatchers.IO) {
            try {
                val matchCriteria = extractMatchCriteria(user)
                val aiMatchResult = getAIMatchAnalysis(matchCriteria, job)
                val ruleBasedMatch = calculateRuleBasedMatch(user, job)

                // Use intelligent score combination instead of simple averaging
                val finalPercentage = combineMatchScores(aiMatchResult.first, ruleBasedMatch, matchCriteria, job)
                val reasoning = generateEnhancedReasoning(finalPercentage, aiMatchResult.second, matchCriteria, job)

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
                    locationMatch = calculateHierarchicalLocationMatch(user.province, user.city, job.province, job.city)
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
            province = user.province,
            city = user.city,
            field = user.field,
            subField = user.subField,
            yearsOfExperience = user.yearsOfExperience,
            expectedSalary = user.expectedSalary
        )
    }

    private suspend fun getAIMatchAnalysis(criteria: MatchCriteria, job: Job): Pair<Int, String> {
        return try {
            val matchPercentage = geminiService.calculateJobMatch(
                userSkills = criteria.skills,
                userExperience = criteria.experience.joinToString(", "),
                userEducation = criteria.education.joinToString(", "),
                userField = extractUserField(criteria),
                userSubField = extractUserSubField(criteria),
                userYearsOfExperience = extractYearsOfExperience(criteria),
                userExpectedSalary = extractExpectedSalary(criteria),
                userProvince = extractUserProvince(criteria),
                userCity = extractUserCity(criteria),
                jobTitle = job.title,
                jobDescription = job.description,
                jobRequirements = job.requirements,
                jobField = job.jobField,
                jobSpecialization = job.specialization,
                jobExperienceLevel = job.experienceLevel,
                jobSalary = job.salary,
                jobProvince = job.province,
                jobCity = job.city
            )

            val reasoning = generateMatchReasoning(matchPercentage, criteria, job)
            Pair(matchPercentage, reasoning)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI analysis failed, using fallback", e)
            Pair(50, "Unable to perform detailed AI analysis")
        }
    }

    private fun extractUserField(criteria: MatchCriteria): String {
        return criteria.field
    }

    private fun extractUserSubField(criteria: MatchCriteria): String {
        return criteria.subField
    }

    private fun extractYearsOfExperience(criteria: MatchCriteria): String {
        return criteria.yearsOfExperience
    }

    private fun extractExpectedSalary(criteria: MatchCriteria): String {
        return criteria.expectedSalary
    }

    private fun extractUserProvince(criteria: MatchCriteria): String {
        return criteria.province
    }

    private fun extractUserCity(criteria: MatchCriteria): String {
        return criteria.city
    }

    private fun combineMatchScores(aiScore: Int, ruleBasedScore: Int, criteria: MatchCriteria, job: Job): Int {
        // Intelligent score combination that doesn't penalize perfect matches
        val scoreDifference = kotlin.math.abs(aiScore - ruleBasedScore)

        return when {
            // If both scores are high (80+), use the higher one
            aiScore >= 80 && ruleBasedScore >= 80 -> maxOf(aiScore, ruleBasedScore)

            // If scores are very close (within 10 points), use the higher one
            scoreDifference <= 10 -> maxOf(aiScore, ruleBasedScore)

            // If one score is much higher than the other, check for perfect field/skill match
            scoreDifference > 20 -> {
                val fieldMatch = calculateFieldSpecializationMatch(criteria.field, criteria.subField, job.jobField, job.specialization)
                val skillsMatch = calculateSkillsMatch(criteria.skills, job.requirements)

                // If field and skills are excellent, trust the higher score
                if (fieldMatch >= 80 && skillsMatch >= 70) {
                    maxOf(aiScore, ruleBasedScore)
                } else {
                    // Otherwise, use weighted average favoring rule-based for consistency
                    ((ruleBasedScore * 0.7) + (aiScore * 0.3)).toInt()
                }
            }

            // Default: weighted average favoring rule-based
            else -> ((ruleBasedScore * 0.6) + (aiScore * 0.4)).toInt()
        }.coerceIn(0, 100)
    }

    private fun generateEnhancedReasoning(percentage: Int, aiReasoning: String, criteria: MatchCriteria, job: Job): String {
        val fieldMatch = calculateFieldSpecializationMatch(criteria.field, criteria.subField, job.jobField, job.specialization)
        val skillsMatch = calculateSkillsMatch(criteria.skills, job.requirements)

        return when {
            percentage >= 85 -> "Excellent match! Your ${criteria.field} background and skills align perfectly with this ${job.jobField} position."
            percentage >= 75 -> "Great match! Strong alignment between your profile and job requirements."
            percentage >= 65 -> "Good match! You meet most requirements with some areas for growth."
            percentage >= 50 -> "Fair match! Some relevant experience but notable gaps in requirements."
            else -> "Limited match. Consider developing additional skills for this type of role."
        }
    }

    private fun generateMatchReasoning(percentage: Int, criteria: MatchCriteria, job: Job): String {
        return when {
            percentage >= 80 -> "Excellent match! Your skills and experience align very well with this position."
            percentage >= 60 -> "Good match. You have relevant skills for this role with some areas for growth."
            percentage >= 40 -> "Moderate match. Some of your skills are relevant, but you may need additional experience."
            else -> "Limited match. This role may require skills or experience you don't currently have."
        }
    }



    private fun calculateRuleBasedMatch(user: User, job: Job): Int {
        // Enhanced rule-based matching with field/specialization as primary factor
        val fieldScore = calculateFieldSpecializationMatch(user.field, user.subField, job.jobField, job.specialization)
        val skillsScore = calculateSkillsMatch(user.skills, job.requirements)
        val experienceScore = calculateExperienceMatch(user.experience, job.experienceLevel)
        val educationScore = calculateEducationMatch(user.education, job.jobField)
        val locationScore = calculateHierarchicalLocationMatch(user.province, user.city, job.province, job.city)
        val salaryScore = calculateSalaryMatch(user.expectedSalary, job.salary)

        // Enhanced weighted calculation with salary consideration
        val finalScore = ((fieldScore * 0.35) + (skillsScore * 0.25) + (experienceScore * 0.15) +
                         (educationScore * 0.10) + (locationScore * 0.10) + (salaryScore * 0.05)).toInt()

        Log.d(TAG, "Rule-based match - Field: $fieldScore%, Skills: $skillsScore%, " +
                  "Experience: $experienceScore%, Education: $educationScore%, Location: $locationScore%, " +
                  "Salary: $salaryScore%, Final: $finalScore%")

        return finalScore.coerceAtMost(100)
    }

    private fun calculateFieldSpecializationMatch(userField: String, userSubField: String,
                                                jobField: String, jobSpecialization: String): Int {
        if (userField.isEmpty() || jobField.isEmpty()) return 50

        val userFieldLower = userField.lowercase().trim()
        val userSubFieldLower = userSubField.lowercase().trim()
        val jobFieldLower = jobField.lowercase().trim()
        val jobSpecializationLower = jobSpecialization.lowercase().trim()

        return when {
            // Perfect match: same field and specialization
            userFieldLower == jobFieldLower && userSubFieldLower == jobSpecializationLower -> 90
            // Good match: same field, related specialization
            userFieldLower == jobFieldLower && (userSubFieldLower.contains(jobSpecializationLower) ||
                jobSpecializationLower.contains(userSubFieldLower)) -> 80
            // Fair match: same field, different specialization
            userFieldLower == jobFieldLower -> 65
            // Related fields (e.g., IT and Computer Science)
            areFieldsRelated(userFieldLower, jobFieldLower) -> 50
            // Transferable skills between fields
            areFieldsTransferable(userFieldLower, jobFieldLower) -> 30
            else -> 15
        }
    }

    private fun areFieldsRelated(field1: String, field2: String): Boolean {
        val relatedGroups = listOf(
            listOf("information technology", "computer science", "software engineering", "software development"),
            listOf("engineering", "mechanical engineering", "electrical engineering", "civil engineering", "industrial engineering"),
            listOf("finance", "accounting", "economics", "business administration"),
            listOf("marketing", "sales", "business development", "communications", "advertising"),
            listOf("healthcare", "nursing", "medicine", "medical", "health sciences"),
            listOf("education", "teaching", "training", "academic", "curriculum development")
        )

        return relatedGroups.any { group ->
            group.any { field1.contains(it) } && group.any { field2.contains(it) }
        }
    }

    private fun areFieldsTransferable(field1: String, field2: String): Boolean {
        val transferableFields = listOf(
            "management", "project management", "business", "administration",
            "customer service", "sales", "communications", "research", "consulting"
        )

        return transferableFields.any { transferable ->
            (field1.contains(transferable) || field2.contains(transferable))
        }
    }

    private fun calculateSkillsMatch(userSkills: List<String>, jobRequirements: String): Int {
        if (userSkills.isEmpty()) return 20
        if (jobRequirements.isEmpty()) return 40

        val requirements = jobRequirements.lowercase()
        var exactMatchCount = 0
        var partialMatchCount = 0
        var totalSkills = userSkills.size

        // Enhanced skill matching with synonyms and related skills
        for (skill in userSkills) {
            val skillLower = skill.lowercase().trim()
            if (skillLower.isEmpty()) continue

            when {
                // Direct exact match
                requirements.contains(skillLower) -> exactMatchCount++
                // Check for related/synonym skills
                hasRelatedSkillInRequirements(skillLower, requirements) -> partialMatchCount++
            }
        }

        // Calculate weighted match percentage
        val exactMatchPercentage = if (totalSkills > 0) {
            (exactMatchCount.toDouble() / totalSkills * 100).toInt()
        } else 0

        val partialMatchPercentage = if (totalSkills > 0) {
            (partialMatchCount.toDouble() / totalSkills * 50).toInt() // Partial matches worth 50%
        } else 0

        val combinedMatchPercentage = exactMatchPercentage + partialMatchPercentage

        // Stricter scoring based on combined match percentage
        return when {
            combinedMatchPercentage >= 90 -> 85  // Need very high skill match
            combinedMatchPercentage >= 70 -> 75  // Good skill match
            combinedMatchPercentage >= 50 -> 60  // Moderate skill match
            combinedMatchPercentage >= 30 -> 45  // Some relevant skills
            combinedMatchPercentage > 0 -> 30    // Few relevant skills
            else -> 15                           // No relevant skills
        }.coerceAtMost(100)
    }

    private fun hasRelatedSkillInRequirements(userSkill: String, requirements: String): Boolean {
        // Enhanced skill synonyms and related terms
        val skillSynonyms = mapOf(
            "javascript" to listOf("js", "node.js", "react", "angular", "vue.js", "typescript", "frontend"),
            "python" to listOf("django", "flask", "pandas", "numpy", "machine learning", "data science"),
            "java" to listOf("spring", "hibernate", "maven", "gradle", "backend", "enterprise"),
            "sql" to listOf("mysql", "postgresql", "database", "oracle", "data analysis", "queries"),
            "html" to listOf("web development", "frontend", "markup", "css"),
            "css" to listOf("styling", "frontend", "web design", "bootstrap", "sass"),
            "project management" to listOf("scrum", "agile", "kanban", "pmp", "planning", "coordination"),
            "communication" to listOf("presentation", "public speaking", "writing", "interpersonal"),
            "leadership" to listOf("management", "team lead", "supervision", "mentoring"),
            "microsoft office" to listOf("excel", "word", "powerpoint", "outlook", "office suite"),
            "data analysis" to listOf("analytics", "reporting", "excel", "tableau", "power bi"),
            "customer service" to listOf("support", "client relations", "help desk", "customer care"),
            "sales" to listOf("business development", "account management", "revenue", "client acquisition")
        )

        return skillSynonyms[userSkill]?.any { synonym ->
            requirements.contains(synonym)
        } ?: false
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

    private fun calculateHierarchicalLocationMatch(userProvince: String, userCity: String, jobProvince: String, jobCity: String): Int {
        if (userProvince.isEmpty() && userCity.isEmpty()) return 80

        val userProv = userProvince.lowercase().trim()
        val userCityLower = userCity.lowercase().trim()
        val jobProv = jobProvince.lowercase().trim()
        val jobCityLower = jobCity.lowercase().trim()

        return when {
            // Remote work gets highest score
            jobCityLower.contains("remote") || jobProv.contains("remote") ||
            jobCityLower == "remote" || jobProv == "remote" -> 100

            // Perfect city match (highest priority)
            userCityLower.isNotEmpty() && jobCityLower.isNotEmpty() &&
            userCityLower == jobCityLower -> 100

            // Same province, different cities but both specified
            userProv.isNotEmpty() && jobProv.isNotEmpty() && userProv == jobProv &&
            userCityLower.isNotEmpty() && jobCityLower.isNotEmpty() -> 85

            // Same province, one or both cities not specified
            userProv.isNotEmpty() && jobProv.isNotEmpty() && userProv == jobProv -> 90

            // User city matches job province (e.g., user: "Johannesburg", job province: "Gauteng")
            userCityLower.isNotEmpty() && jobProv.isNotEmpty() &&
            LocationData.getProvinceForCity(userCity)?.lowercase() == jobProv -> 95

            // Job city matches user province (e.g., user province: "Gauteng", job: "Johannesburg")
            userProv.isNotEmpty() && jobCityLower.isNotEmpty() &&
            LocationData.getProvinceForCity(jobCity)?.lowercase() == userProv -> 95

            // Partial city match (contains)
            userCityLower.isNotEmpty() && jobCityLower.isNotEmpty() &&
            (userCityLower.contains(jobCityLower) || jobCityLower.contains(userCityLower)) -> 80

            // Partial province match
            userProv.isNotEmpty() && jobProv.isNotEmpty() &&
            (userProv.contains(jobProv) || jobProv.contains(userProv)) -> 75

            // Different provinces but same country (South Africa)
            else -> 50
        }
    }

    private fun calculateSalaryMatch(userExpectedSalary: String, jobSalary: String): Int {
        if (userExpectedSalary.isEmpty() || jobSalary.isEmpty()) return 85 // Neutral if no salary info

        try {
            val userSalaryRange = parseSalaryRange(userExpectedSalary)
            val jobSalaryRange = parseSalaryRange(jobSalary)

            if (userSalaryRange == null || jobSalaryRange == null) return 85

            val userMin = userSalaryRange.first
            val userMax = userSalaryRange.second
            val jobMin = jobSalaryRange.first
            val jobMax = jobSalaryRange.second

            return when {
                // Job salary range overlaps with user expectation
                (jobMax >= userMin && jobMin <= userMax) -> {
                    // Calculate overlap percentage
                    val overlapStart = maxOf(userMin, jobMin)
                    val overlapEnd = minOf(userMax, jobMax)
                    val overlapSize = overlapEnd - overlapStart
                    val userRangeSize = userMax - userMin
                    val overlapPercentage = if (userRangeSize > 0) (overlapSize.toDouble() / userRangeSize * 100).toInt() else 100

                    when {
                        overlapPercentage >= 80 -> 100 // Excellent salary match
                        overlapPercentage >= 50 -> 90  // Good salary match
                        overlapPercentage >= 20 -> 80  // Fair salary match
                        else -> 70                     // Some overlap
                    }
                }
                // Job salary is higher than user expectation (good for user)
                jobMin >= userMax -> 95
                // Job salary is lower than user expectation
                jobMax < userMin -> {
                    val gap = userMin - jobMax
                    val userMinSalary = userMin
                    val gapPercentage = if (userMinSalary > 0) (gap.toDouble() / userMinSalary * 100).toInt() else 100

                    when {
                        gapPercentage <= 10 -> 75  // Small gap
                        gapPercentage <= 25 -> 60  // Moderate gap
                        gapPercentage <= 50 -> 40  // Large gap
                        else -> 20                 // Very large gap
                    }
                }
                else -> 70
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating salary match", e)
            return 85 // Neutral if parsing fails
        }
    }

    private fun parseSalaryRange(salaryString: String): Pair<Int, Int>? {
        try {
            val cleanSalary = salaryString.replace(Regex("[R,\\s]"), "").lowercase()

            return when {
                // Range format: "10000-20000" or "10000 - 20000"
                cleanSalary.contains("-") -> {
                    val parts = cleanSalary.split("-")
                    if (parts.size == 2) {
                        val min = parts[0].trim().toIntOrNull() ?: return null
                        val max = parts[1].trim().toIntOrNull() ?: return null
                        Pair(min, max)
                    } else null
                }
                // Single value: "15000"
                cleanSalary.matches(Regex("\\d+")) -> {
                    val salary = cleanSalary.toInt()
                    Pair(salary, salary)
                }
                // Range with "+" (e.g., "50000+")
                cleanSalary.endsWith("+") -> {
                    val baseSalary = cleanSalary.dropLast(1).toIntOrNull() ?: return null
                    Pair(baseSalary, baseSalary * 2) // Assume upper bound is double
                }
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }


}
