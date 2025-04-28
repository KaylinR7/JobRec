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
        // Query jobs that match the alert criteria
        val jobsQuery = db.collection("jobs")
            .whereEqualTo("isActive", true)
            .whereGreaterThan("createdAt", jobAlert.lastNotified ?: jobAlert.createdAt)
            
        // Add filters based on alert criteria
        if (jobAlert.keywords.isNotEmpty()) {
            jobsQuery.whereArrayContainsAny("keywords", jobAlert.keywords)
        }
        if (jobAlert.locations.isNotEmpty()) {
            jobsQuery.whereIn("location", jobAlert.locations)
        }
        if (jobAlert.jobTypes.isNotEmpty()) {
            jobsQuery.whereIn("type", jobAlert.jobTypes)
        }
        
        return jobsQuery.get()
            .await()
            .documents
            .map { it.id }
    }
} 