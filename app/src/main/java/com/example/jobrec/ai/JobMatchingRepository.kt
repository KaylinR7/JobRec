package com.example.jobrec.ai

import android.util.Log
import com.example.jobrec.User
import com.example.jobrec.Job
import com.example.jobrec.models.JobMatch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class JobMatchingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val jobMatchingService = JobMatchingService()
    private val TAG = "JobMatchingRepository"

    suspend fun getRecommendedJobsWithMatches(): List<JobMatch> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "No current user found, returning empty list")
                    return@withContext emptyList()
                }

                val jobs = getActiveJobs()
                val jobMatches = mutableListOf<JobMatch>()

                for (job in jobs) {
                    try {
                        val jobMatch = jobMatchingService.calculateJobMatch(currentUser, job)
                        jobMatches.add(jobMatch)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error calculating match for job ${job.id}", e)
                        jobMatches.add(
                            JobMatch(
                                job = job.copy(matchPercentage = 50, matchReasoning = "Standard match"),
                                matchPercentage = 50,
                                matchReasoning = "Standard match"
                            )
                        )
                    }
                }

                jobMatches.sortedByDescending { it.matchPercentage }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recommended jobs with matches", e)
                emptyList()
            }
        }
    }

    suspend fun calculateMatchForJob(job: Job): JobMatch? {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "No current user found for job match calculation")
                    return@withContext null
                }

                jobMatchingService.calculateJobMatch(currentUser, job)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating match for specific job", e)
                null
            }
        }
    }

    private suspend fun getCurrentUser(): User? {
        return try {
            val currentUserEmail = auth.currentUser?.email
            if (currentUserEmail == null) {
                Log.w(TAG, "No authenticated user")
                return null
            }

            val userDocuments = db.collection("users")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .await()

            if (userDocuments.isEmpty) {
                Log.w(TAG, "No user document found for email: $currentUserEmail")
                return null
            }

            val userDoc = userDocuments.documents[0]
            val user = userDoc.toObject(User::class.java)
            Log.d(TAG, "Successfully retrieved user profile for matching")
            user
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving current user", e)
            null
        }
    }

    private suspend fun getActiveJobs(): List<Job> {
        return try {
            val jobDocuments = db.collection("jobs")
                .whereEqualTo("status", "active")
                .get()
                .await()

            val jobs = jobDocuments.mapNotNull { doc ->
                try {
                    doc.toObject(Job::class.java).copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to Job object: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Retrieved ${jobs.size} active jobs for matching")
            jobs
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving active jobs", e)
            emptyList()
        }
    }

    suspend fun getJobsWithMatches(limit: Int = 10): List<Job> {
        return withContext(Dispatchers.IO) {
            try {
                val jobMatches = getRecommendedJobsWithMatches()
                jobMatches.take(limit).map { it.job }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting jobs with matches", e)
                emptyList()
            }
        }
    }

    suspend fun refreshUserMatchCache() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing user match cache...")
                val jobMatches = getRecommendedJobsWithMatches()
                Log.d(TAG, "Refreshed ${jobMatches.size} job matches")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing user match cache", e)
            }
        }
    }
}
