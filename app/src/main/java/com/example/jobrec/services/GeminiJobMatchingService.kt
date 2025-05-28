package com.example.jobrec.services

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.jobrec.BuildConfig

class GeminiJobMatchingService {

    companion object {
        private const val TAG = "GeminiJobMatching"
        // Move API key to BuildConfig for security
        private val API_KEY = BuildConfig.GEMINI_API_KEY
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 3
            topP = 0.9f
            maxOutputTokens = 200
        }
    )

    suspend fun calculateJobMatch(
        userSkills: List<String>,
        userExperience: String,
        userEducation: String,
        userField: String,
        userSubField: String,
        userYearsOfExperience: String,
        userExpectedSalary: String,
        userProvince: String,
        userCity: String,
        jobTitle: String,
        jobDescription: String,
        jobRequirements: String,
        jobField: String,
        jobSpecialization: String,
        jobExperienceLevel: String,
        jobSalary: String,
        jobProvince: String,
        jobCity: String
    ): Int = withContext(Dispatchers.IO) {
        try {
            val prompt = buildEnhancedPrompt(
                userSkills, userExperience, userEducation, userField, userSubField, userYearsOfExperience, userExpectedSalary,
                userProvince, userCity, jobTitle, jobDescription, jobRequirements, jobField, jobSpecialization,
                jobExperienceLevel, jobSalary, jobProvince, jobCity
            )

            Log.d(TAG, "Sending enhanced prompt to Gemini")

            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: "0"

            Log.d(TAG, "Gemini response: $responseText")

            // Extract percentage from response
            val percentage = extractPercentage(responseText)
            Log.d(TAG, "Extracted percentage: $percentage")

            // Validate the percentage is reasonable
            if (percentage in 0..100) {
                percentage
            } else {
                Log.w(TAG, "Invalid percentage from Gemini: $percentage, using fallback")
                calculateEnhancedFallbackMatch(userSkills, userField, userSubField, userYearsOfExperience, userExpectedSalary,
                    userProvince, userCity, jobDescription, jobRequirements, jobField, jobSpecialization, jobExperienceLevel,
                    jobSalary, jobProvince, jobCity)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating job match with Gemini", e)
            // Enhanced fallback matching
            calculateEnhancedFallbackMatch(userSkills, userField, userSubField, userYearsOfExperience, userExpectedSalary,
                userProvince, userCity, jobDescription, jobRequirements, jobField, jobSpecialization, jobExperienceLevel,
                jobSalary, jobProvince, jobCity)
        }
    }

    private fun buildEnhancedPrompt(
        userSkills: List<String>,
        userExperience: String,
        userEducation: String,
        userField: String,
        userSubField: String,
        userYearsOfExperience: String,
        userExpectedSalary: String,
        userProvince: String,
        userCity: String,
        jobTitle: String,
        jobDescription: String,
        jobRequirements: String,
        jobField: String,
        jobSpecialization: String,
        jobExperienceLevel: String,
        jobSalary: String,
        jobProvince: String,
        jobCity: String
    ): String {
        return """
            You are an expert career counselor and job matching specialist. Analyze the compatibility between a candidate and a job posting.

            CANDIDATE PROFILE:
            • Field of Interest: $userField
            • Specialization: $userSubField
            • Years of Experience: $userYearsOfExperience
            • Expected Salary: $userExpectedSalary
            • Location: $userCity, $userProvince
            • Skills: ${userSkills.joinToString(", ")}
            • Work Experience: $userExperience
            • Education: $userEducation

            JOB POSTING:
            • Job Title: $jobTitle
            • Job Field: $jobField
            • Specialization Required: $jobSpecialization
            • Experience Level Required: $jobExperienceLevel
            • Salary Offered: $jobSalary
            • Job Location: $jobCity, $jobProvince
            • Job Description: $jobDescription
            • Requirements: $jobRequirements

            EVALUATION CRITERIA (in order of importance):
            1. Field/Specialization Match (30%): How well does the candidate's field and specialization align with the job?
            2. Skills Compatibility (25%): Do the candidate's skills match the job requirements? Consider related/transferable skills.
            3. Experience Level Match (20%): Does the candidate's experience level match what's required?
            4. Location Compatibility (15%): How well does the candidate's location match the job location? Consider:
               - Same city = Perfect match (100%)
               - Same province, different city = Good match (85%)
               - Different province = Poor match (30%)
               - Remote work = Perfect match (100%)
            5. Education Relevance (5%): Is the candidate's education relevant to the position?
            6. Salary Alignment (5%): Does the job salary meet or exceed the candidate's expectations?

            SCORING GUIDELINES (BE STRICT):
            • 90-100%: Perfect match - candidate exceeds requirements in all areas
            • 80-89%: Excellent match - candidate meets all key requirements with strong alignment
            • 70-79%: Good match - candidate meets most requirements with minor gaps
            • 60-69%: Fair match - candidate has relevant background but notable gaps
            • 50-59%: Poor match - candidate has some relevant skills but major gaps
            • 0-49%: Very poor match - candidate not suitable for this role

            STRICT REQUIREMENTS:
            - Field mismatch should heavily penalize the score (max 60% if fields don't align)
            - Missing critical skills should significantly reduce the score
            - Experience level mismatch should be heavily weighted
            - Only award high scores (80+%) when there's strong alignment across all criteria

            Return ONLY a single number between 0-100 representing the overall match percentage. No explanation needed.
        """.trimIndent()
    }

    private fun extractPercentage(response: String): Int {
        return try {
            // Try to find a number in the response
            val numbers = Regex("\\d+").findAll(response)
            val percentage = numbers.firstOrNull()?.value?.toIntOrNull() ?: 0

            // Ensure it's within valid range
            when {
                percentage > 100 -> 100
                percentage < 0 -> 0
                else -> percentage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting percentage from: $response", e)
            0
        }
    }

    private fun calculateEnhancedFallbackMatch(
        userSkills: List<String>,
        userField: String,
        userSubField: String,
        userYearsOfExperience: String,
        userExpectedSalary: String,
        userProvince: String,
        userCity: String,
        jobDescription: String,
        jobRequirements: String,
        jobField: String,
        jobSpecialization: String,
        jobExperienceLevel: String,
        jobSalary: String,
        jobProvince: String,
        jobCity: String
    ): Int {
        return try {
            // 1. Field/Specialization Match (30% weight)
            val fieldMatch = calculateFieldMatch(userField, userSubField, jobField, jobSpecialization)

            // 2. Skills Match (25% weight)
            val skillsMatch = calculateSkillsMatch(userSkills, jobDescription, jobRequirements, userField)

            // 3. Experience Match (20% weight)
            val experienceMatch = calculateExperienceMatch(userYearsOfExperience, jobExperienceLevel)

            // 4. Location Match (15% weight)
            val locationMatch = calculateLocationMatch(userProvince, userCity, jobProvince, jobCity)

            // 5. Education relevance (5% weight) - simplified for fallback
            val educationMatch = if (fieldMatch > 70) 80 else 60

            // 6. Salary match (5% weight)
            val salaryMatch = calculateSalaryMatch(userExpectedSalary, jobSalary)

            // Weighted calculation with location included
            val finalScore = (fieldMatch * 0.30 + skillsMatch * 0.25 + experienceMatch * 0.20 +
                            locationMatch * 0.15 + educationMatch * 0.05 + salaryMatch * 0.05).toInt()

            Log.d(TAG, "Enhanced fallback match - Field: $fieldMatch%, Skills: $skillsMatch%, " +
                      "Experience: $experienceMatch%, Location: $locationMatch%, Final: $finalScore%")

            finalScore.coerceIn(0, 100)

        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced fallback calculation", e)
            50 // Conservative fallback
        }
    }

    private fun calculateLocationMatch(userProvince: String, userCity: String, jobProvince: String, jobCity: String): Int {
        if (userProvince.isEmpty() && userCity.isEmpty()) return 80 // Neutral if no location specified

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

    private fun calculateFieldMatch(userField: String, userSubField: String, jobField: String, jobSpecialization: String): Int {
        if (userField.isEmpty() || jobField.isEmpty()) return 50

        val userFieldLower = userField.lowercase().trim()
        val userSubFieldLower = userSubField.lowercase().trim()
        val jobFieldLower = jobField.lowercase().trim()
        val jobSpecializationLower = jobSpecialization.lowercase().trim()

        return when {
            // Exact field and specialization match
            userFieldLower == jobFieldLower && userSubFieldLower == jobSpecializationLower -> 90
            // Exact field match, related specialization
            userFieldLower == jobFieldLower && (userSubFieldLower.contains(jobSpecializationLower) ||
                jobSpecializationLower.contains(userSubFieldLower)) -> 80
            // Exact field match, different specialization
            userFieldLower == jobFieldLower -> 65
            // Related fields (e.g., "Software Engineering" and "Information Technology")
            areRelatedFields(userFieldLower, jobFieldLower) -> 55
            // Transferable fields
            areTransferableFields(userFieldLower, jobFieldLower) -> 35
            else -> 15
        }
    }

    private fun areRelatedFields(field1: String, field2: String): Boolean {
        val relatedFieldGroups = listOf(
            listOf("information technology", "computer science", "software engineering", "software development"),
            listOf("engineering", "mechanical engineering", "electrical engineering", "civil engineering"),
            listOf("finance", "accounting", "economics", "business"),
            listOf("marketing", "sales", "business development", "communications"),
            listOf("healthcare", "nursing", "medicine", "medical"),
            listOf("education", "teaching", "training", "academic")
        )

        return relatedFieldGroups.any { group ->
            group.any { field1.contains(it) } && group.any { field2.contains(it) }
        }
    }

    private fun areTransferableFields(field1: String, field2: String): Boolean {
        val transferableSkillsFields = listOf(
            "management", "project management", "business", "administration",
            "customer service", "sales", "communications", "research"
        )

        return transferableSkillsFields.any { skill ->
            (field1.contains(skill) || field2.contains(skill))
        }
    }

    private fun calculateSkillsMatch(userSkills: List<String>, jobDescription: String,
                                   jobRequirements: String, userField: String): Int {
        if (userSkills.isEmpty()) return 20

        val jobText = "$jobDescription $jobRequirements".lowercase()
        var matchCount = 0
        var partialMatchCount = 0

        for (skill in userSkills) {
            val skillLower = skill.lowercase().trim()
            if (skillLower.isEmpty()) continue

            when {
                // Exact skill match
                jobText.contains(skillLower) -> matchCount++
                // Partial or related skill match
                hasRelatedSkill(skillLower, jobText, userField) -> partialMatchCount++
            }
        }

        val totalSkills = userSkills.size
        val exactMatchPercentage = (matchCount.toDouble() / totalSkills * 100).toInt()
        val partialMatchPercentage = (partialMatchCount.toDouble() / totalSkills * 50).toInt()

        val combinedMatch = exactMatchPercentage + partialMatchPercentage

        // Additional strictness: require minimum number of actual skill matches for high scores
        val minimumSkillsForHighScore = when {
            totalSkills >= 5 -> 3  // Need at least 3 matching skills if user has 5+ skills
            totalSkills >= 3 -> 2  // Need at least 2 matching skills if user has 3+ skills
            else -> 1              // Need at least 1 matching skill
        }

        val actualMatches = matchCount + (partialMatchCount / 2) // Partial matches count as half

        return when {
            combinedMatch >= 90 && actualMatches >= minimumSkillsForHighScore -> 85
            combinedMatch >= 70 && actualMatches >= (minimumSkillsForHighScore * 0.8).toInt() -> 75
            combinedMatch >= 50 && actualMatches >= (minimumSkillsForHighScore * 0.6).toInt() -> 60
            combinedMatch >= 30 -> 45
            combinedMatch > 0 -> 30
            else -> 15
        }.coerceAtMost(100)
    }

    private fun hasRelatedSkill(userSkill: String, jobText: String, userField: String): Boolean {
        // Define skill synonyms and related terms
        val skillSynonyms = mapOf(
            "javascript" to listOf("js", "node.js", "react", "angular", "vue"),
            "python" to listOf("django", "flask", "pandas", "numpy"),
            "java" to listOf("spring", "hibernate", "maven", "gradle"),
            "sql" to listOf("mysql", "postgresql", "database", "oracle"),
            "project management" to listOf("scrum", "agile", "kanban", "pmp"),
            "communication" to listOf("presentation", "public speaking", "writing"),
            "leadership" to listOf("management", "team lead", "supervision")
        )

        return skillSynonyms[userSkill]?.any { synonym ->
            jobText.contains(synonym)
        } ?: false
    }

    private fun calculateExperienceMatch(userYearsOfExperience: String, jobExperienceLevel: String): Int {
        if (userYearsOfExperience.isEmpty() || jobExperienceLevel.isEmpty()) return 70

        val userYears = extractYearsFromString(userYearsOfExperience)
        val requiredYears = extractYearsFromString(jobExperienceLevel)

        return when {
            userYears >= requiredYears -> 90
            userYears >= (requiredYears * 0.9) -> 80  // Very close to requirement
            userYears >= (requiredYears * 0.7) -> 65  // Somewhat close
            userYears >= (requiredYears * 0.5) -> 45  // Half the required experience
            userYears > 0 -> 25                       // Some experience but not enough
            else -> 10                                // No relevant experience
        }
    }

    private fun extractYearsFromString(experienceString: String): Int {
        val lowerString = experienceString.lowercase()
        return when {
            lowerString.contains("no experience") || lowerString.contains("entry") -> 0
            lowerString.contains("less than 1") || lowerString.contains("0-1") -> 0
            lowerString.contains("1-2") -> 1
            lowerString.contains("2-3") -> 2
            lowerString.contains("3-5") -> 3
            lowerString.contains("5-7") -> 5
            lowerString.contains("7-10") -> 7
            lowerString.contains("10+") || lowerString.contains("senior") -> 10
            else -> {
                // Try to extract number from string
                val regex = Regex("(\\d+)")
                regex.find(lowerString)?.value?.toIntOrNull() ?: 2
            }
        }
    }

    private fun calculateOverallFit(userField: String, userSubField: String, jobDescription: String): Int {
        val description = jobDescription.lowercase()
        val field = userField.lowercase()
        val subField = userSubField.lowercase()

        return when {
            description.contains(field) && description.contains(subField) -> 90
            description.contains(field) -> 80
            description.contains(subField) -> 70
            else -> 60
        }
    }

    private fun calculateSalaryMatch(userExpectedSalary: String, jobSalary: String): Int {
        if (userExpectedSalary.isEmpty() || jobSalary.isEmpty()) return 85

        try {
            val userSalaryRange = parseSalaryRange(userExpectedSalary)
            val jobSalaryRange = parseSalaryRange(jobSalary)

            if (userSalaryRange == null || jobSalaryRange == null) return 85

            val userMin = userSalaryRange.first
            val userMax = userSalaryRange.second
            val jobMin = jobSalaryRange.first
            val jobMax = jobSalaryRange.second

            return when {
                // Job salary overlaps with user expectation
                (jobMax >= userMin && jobMin <= userMax) -> 95
                // Job salary is higher than user expectation
                jobMin >= userMax -> 100
                // Job salary is lower but close
                jobMax >= (userMin * 0.9) -> 80
                // Job salary is significantly lower
                else -> 60
            }
        } catch (e: Exception) {
            return 85
        }
    }

    private fun parseSalaryRange(salaryString: String): Pair<Int, Int>? {
        try {
            val cleanSalary = salaryString.replace(Regex("[R,\\s]"), "").lowercase()

            return when {
                cleanSalary.contains("-") -> {
                    val parts = cleanSalary.split("-")
                    if (parts.size == 2) {
                        val min = parts[0].trim().toIntOrNull() ?: return null
                        val max = parts[1].trim().toIntOrNull() ?: return null
                        Pair(min, max)
                    } else null
                }
                cleanSalary.matches(Regex("\\d+")) -> {
                    val salary = cleanSalary.toInt()
                    Pair(salary, salary)
                }
                cleanSalary.endsWith("+") -> {
                    val baseSalary = cleanSalary.dropLast(1).toIntOrNull() ?: return null
                    Pair(baseSalary, baseSalary * 2)
                }
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }
}
