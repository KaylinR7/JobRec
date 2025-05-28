package com.example.jobrec

import com.example.jobrec.ai.JobMatchingService
import com.example.jobrec.models.JobMatch
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking

/**
 * Test class for the enhanced job matching algorithm
 */
class JobMatchingTest {

    private val jobMatchingService = JobMatchingService()

    @Test
    fun testPerfectFieldMatch() = runBlocking {
        // Create a user with software engineering background
        val user = createTestUser(
            field = "Information Technology",
            subField = "Software Development",
            skills = listOf("Java", "Spring", "SQL", "JavaScript"),
            yearsOfExperience = "3-5 years"
        )

        // Create a matching job
        val job = createTestJob(
            title = "Software Developer",
            jobField = "Information Technology",
            specialization = "Software Development",
            requirements = "Java, Spring Framework, SQL database experience required",
            experienceLevel = "3-5 years"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should get a high match score (75%+) for perfect field and skill alignment
        assertTrue("Perfect match should score 75%+", result.matchPercentage >= 75)
        println("Perfect match score: ${result.matchPercentage}%")
    }

    @Test
    fun testRelatedFieldMatch() = runBlocking {
        // User with Computer Science background
        val user = createTestUser(
            field = "Computer Science",
            subField = "Software Engineering",
            skills = listOf("Python", "Machine Learning", "Data Analysis"),
            yearsOfExperience = "2-3 years"
        )

        // Job in related IT field
        val job = createTestJob(
            title = "Data Scientist",
            jobField = "Information Technology",
            specialization = "Data Science",
            requirements = "Python programming, machine learning algorithms, data analysis skills",
            experienceLevel = "2-3 years"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should get a good match score (60%+) for related fields and matching skills
        assertTrue("Related field match should score 60%+", result.matchPercentage >= 60)
        println("Related field match score: ${result.matchPercentage}%")
    }

    @Test
    fun testPoorFieldMatch() = runBlocking {
        // User with engineering background
        val user = createTestUser(
            field = "Mechanical Engineering",
            subField = "Manufacturing",
            skills = listOf("CAD", "Manufacturing", "Quality Control"),
            yearsOfExperience = "5-7 years"
        )

        // Job in completely different field
        val job = createTestJob(
            title = "Marketing Manager",
            jobField = "Marketing",
            specialization = "Digital Marketing",
            requirements = "Social media marketing, content creation, SEO knowledge",
            experienceLevel = "3-5 years"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should get a low match score (<40%) for unrelated fields and skills
        assertTrue("Unrelated field match should score <40%", result.matchPercentage < 40)
        println("Poor field match score: ${result.matchPercentage}%")
    }

    @Test
    fun testSkillSynonymMatching() = runBlocking {
        // User with JavaScript skills
        val user = createTestUser(
            field = "Information Technology",
            subField = "Web Development",
            skills = listOf("JavaScript", "HTML", "CSS"),
            yearsOfExperience = "1-2 years"
        )

        // Job requiring related frontend skills
        val job = createTestJob(
            title = "Frontend Developer",
            jobField = "Information Technology",
            specialization = "Web Development",
            requirements = "React, Node.js, frontend development experience",
            experienceLevel = "1-2 years"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should recognize JavaScript relates to React/Node.js and score well
        assertTrue("Skill synonym matching should score 65%+", result.matchPercentage >= 65)
        println("Skill synonym match score: ${result.matchPercentage}%")
    }

    @Test
    fun testPerfectLocationMatch() = runBlocking {
        // User in Johannesburg, Gauteng
        val user = createTestUser(
            field = "Information Technology",
            subField = "Software Development",
            skills = listOf("Java", "Spring"),
            yearsOfExperience = "3-5 years",
            province = "Gauteng",
            city = "Johannesburg"
        )

        // Job in same city
        val job = createTestJob(
            title = "Software Developer",
            jobField = "Information Technology",
            specialization = "Software Development",
            requirements = "Java, Spring Framework",
            experienceLevel = "3-5 years",
            province = "Gauteng",
            city = "Johannesburg"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should get high score for perfect location match
        assertTrue("Perfect location match should score 80%+", result.matchPercentage >= 80)
        println("Perfect location match score: ${result.matchPercentage}%")
    }

    @Test
    fun testSameProvinceLocationMatch() = runBlocking {
        // User in Johannesburg, Gauteng
        val user = createTestUser(
            field = "Information Technology",
            subField = "Software Development",
            skills = listOf("Java", "Spring"),
            yearsOfExperience = "3-5 years",
            province = "Gauteng",
            city = "Johannesburg"
        )

        // Job in different city, same province
        val job = createTestJob(
            title = "Software Developer",
            jobField = "Information Technology",
            specialization = "Software Development",
            requirements = "Java, Spring Framework",
            experienceLevel = "3-5 years",
            province = "Gauteng",
            city = "Pretoria"
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should get good score for same province
        assertTrue("Same province match should score 75%+", result.matchPercentage >= 75)
        println("Same province match score: ${result.matchPercentage}%")
    }

    @Test
    fun testCityProvinceHierarchyMatch() = runBlocking {
        // User specifies city only
        val user = createTestUser(
            field = "Information Technology",
            subField = "Software Development",
            skills = listOf("Java", "Spring"),
            yearsOfExperience = "3-5 years",
            province = "",
            city = "Johannesburg"
        )

        // Job specifies province only
        val job = createTestJob(
            title = "Software Developer",
            jobField = "Information Technology",
            specialization = "Software Development",
            requirements = "Java, Spring Framework",
            experienceLevel = "3-5 years",
            province = "Gauteng",
            city = ""
        )

        val result = jobMatchingService.calculateJobMatch(user, job)

        // Should recognize Johannesburg is in Gauteng
        assertTrue("City-province hierarchy match should score 75%+", result.matchPercentage >= 75)
        println("City-province hierarchy match score: ${result.matchPercentage}%")
    }

    private fun createTestUser(
        field: String,
        subField: String,
        skills: List<String>,
        yearsOfExperience: String,
        province: String = "Gauteng",
        city: String = "Johannesburg"
    ): User {
        return User(
            id = "test-user",
            name = "Test User",
            email = "test@example.com",
            field = field,
            subField = subField,
            skills = skills,
            yearsOfExperience = yearsOfExperience,
            province = province,
            city = city,
            summary = "Test user for job matching",
            experience = emptyList(),
            education = emptyList()
        )
    }

    private fun createTestJob(
        title: String,
        jobField: String,
        specialization: String,
        requirements: String,
        experienceLevel: String,
        province: String = "Gauteng",
        city: String = "Johannesburg"
    ): Job {
        return Job(
            id = "test-job",
            title = title,
            companyName = "Test Company",
            jobField = jobField,
            specialization = specialization,
            requirements = requirements,
            experienceLevel = experienceLevel,
            description = "Test job for matching algorithm",
            city = city,
            province = province,
            status = "active"
        )
    }
}
