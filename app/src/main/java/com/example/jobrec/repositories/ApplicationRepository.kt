package com.example.jobrec.repositories

import com.example.jobrec.models.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ApplicationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun submitApplication(application: Application): String {
        val applicationId = UUID.randomUUID().toString()
        val newApplication = application.copy(id = applicationId)
        
        db.collection("applications")
            .document(applicationId)
            .set(newApplication)
            .await()
            
        return applicationId
    }

    suspend fun updateApplicationStatus(applicationId: String, status: String) {
        db.collection("applications")
            .document(applicationId)
            .update(
                mapOf(
                    "status" to status,
                    "lastUpdated" to com.google.firebase.Timestamp.now()
                )
            )
            .await()
    }

    suspend fun addInterview(applicationId: String, interview: Application.Interview) {
        val interviewId = UUID.randomUUID().toString()
        val newInterview = interview.copy(id = interviewId)
        
        db.collection("applications")
            .document(applicationId)
            .update(
                "interviews",
                com.google.firebase.firestore.FieldValue.arrayUnion(newInterview)
            )
            .await()
    }

    suspend fun addApplicationDocument(applicationId: String, document: Application.ApplicationDocument) {
        val documentId = UUID.randomUUID().toString()
        val newDocument = document.copy(id = documentId)
        
        db.collection("applications")
            .document(applicationId)
            .update(
                "documents",
                com.google.firebase.firestore.FieldValue.arrayUnion(newDocument)
            )
            .await()
    }

    suspend fun submitInterviewFeedback(applicationId: String, interviewId: String, feedback: Application.InterviewFeedback) {
        db.collection("applications")
            .document(applicationId)
            .get()
            .await()
            .let { document ->
                val application = document.toObject(Application::class.java)
                val updatedInterviews = application?.interviews?.map { interview ->
                    if (interview.id == interviewId) {
                        interview.copy(feedback = feedback)
                    } else {
                        interview
                    }
                }
                
                document.reference.update("interviews", updatedInterviews).await()
            }
    }

    suspend fun submitApplicationFeedback(applicationId: String, feedback: Application.ApplicationFeedback) {
        db.collection("applications")
            .document(applicationId)
            .update("feedback", feedback)
            .await()
    }

    suspend fun getCandidateApplications(candidateId: String): List<Application> {
        return db.collection("applications")
            .whereEqualTo("candidateId", candidateId)
            .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Application::class.java)
    }

    suspend fun getCompanyApplications(companyId: String): List<Application> {
        return db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Application::class.java)
    }
} 