package com.example.jobrec.repositories

import com.example.jobrec.models.JobAlert
import com.example.jobrec.models.NotificationPreference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class JobAlertRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createJobAlert(jobAlert: JobAlert): String {
        val alertId = UUID.randomUUID().toString()
        val newAlert = jobAlert.copy(
            id = alertId,
            userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        )

        db.collection("jobAlerts")
            .document(alertId)
            .set(newAlert)
            .await()

        return alertId
    }

    suspend fun updateJobAlert(jobAlert: JobAlert) {
        db.collection("jobAlerts")
            .document(jobAlert.id)
            .set(jobAlert)
            .await()
    }

    suspend fun deleteJobAlert(alertId: String) {
        db.collection("jobAlerts")
            .document(alertId)
            .delete()
            .await()
    }

    suspend fun getUserJobAlerts(): List<JobAlert> {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        return db.collection("jobAlerts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .toObjects(JobAlert::class.java)
    }

    suspend fun updateNotificationPreference(preference: NotificationPreference) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        db.collection("notificationPreferences")
            .document(userId)
            .set(preference)
            .await()
    }

    suspend fun getNotificationPreference(): NotificationPreference? {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        return db.collection("notificationPreferences")
            .document(userId)
            .get()
            .await()
            .toObject(NotificationPreference::class.java)
    }

    suspend fun checkForNewJobs(jobAlert: JobAlert): List<String> {
        // Get all active jobs with a simple query
        val allJobs = db.collection("jobs")
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .documents

        // Filter jobs in memory
        return allJobs.filter { jobDoc ->
            var matches = true

            // Check if job is newer than last notification
            val jobCreatedAt = jobDoc.getTimestamp("createdAt")
            val lastNotified = jobAlert.lastNotified ?: jobAlert.createdAt

            if (jobCreatedAt == null || !jobCreatedAt.toDate().after(lastNotified.toDate())) {
                return@filter false
            }

            // Check keywords
            if (jobAlert.keywords.isNotEmpty()) {
                val jobTitle = jobDoc.getString("title") ?: ""
                val jobDescription = jobDoc.getString("description") ?: ""
                val jobKeywords = jobDoc.get("keywords") as? List<String> ?: emptyList()

                // Check if any keyword matches in title, description or keywords
                matches = matches && jobAlert.keywords.any { keyword ->
                    jobTitle.contains(keyword, ignoreCase = true) ||
                    jobDescription.contains(keyword, ignoreCase = true) ||
                    jobKeywords.any { it.contains(keyword, ignoreCase = true) }
                }
            }

            // Check location
            if (matches && jobAlert.locations.isNotEmpty()) {
                val jobLocation = jobDoc.getString("location") ?: ""
                matches = matches && jobAlert.locations.any {
                    jobLocation.contains(it, ignoreCase = true)
                }
            }

            // Check job type
            if (matches && jobAlert.jobTypes.isNotEmpty()) {
                val jobType = jobDoc.getString("type") ?: ""
                matches = matches && jobAlert.jobTypes.contains(jobType)
            }

            matches
        }.map { it.id }
    }
}