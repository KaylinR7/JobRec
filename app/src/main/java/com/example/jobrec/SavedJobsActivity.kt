package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SavedJobsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var jobsAdapter: RecentJobsAdapter
    private lateinit var emptyView: View
    private lateinit var loadingView: View
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SavedJobsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_jobs)

        // Setup toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Saved Jobs"

        // Initialize views
        recyclerView = findViewById(R.id.savedJobsRecyclerView)
        emptyView = findViewById(R.id.emptyStateLayout)
        loadingView = findViewById(R.id.loadingLayout)

        // Setup RecyclerView
        jobsAdapter = RecentJobsAdapter { job ->
            // Handle job item click
            val intent = Intent(this, JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedJobsActivity)
            adapter = jobsAdapter
        }

        // Load saved jobs
        loadSavedJobs()
    }

    private fun loadSavedJobs() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmptyState("Please sign in to view saved jobs")
            return
        }

        showLoading()

        // First, get the saved job IDs for this user
        db.collection("savedJobs")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showEmptyState("You haven't saved any jobs yet")
                    return@addOnSuccessListener
                }

                // Extract job IDs from saved jobs documents
                val jobIds = documents.mapNotNull { it.getString("jobId") }
                
                if (jobIds.isEmpty()) {
                    showEmptyState("You haven't saved any jobs yet")
                    return@addOnSuccessListener
                }

                // Fetch the actual job details
                fetchJobDetails(jobIds)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading saved jobs", e)
                showEmptyState("Error loading saved jobs: ${e.message}")
            }
    }

    private fun fetchJobDetails(jobIds: List<String>) {
        if (jobIds.isEmpty()) {
            showEmptyState("You haven't saved any jobs yet")
            return
        }

        // Firestore doesn't support direct array contains queries with multiple values
        // So we'll fetch jobs one by one and combine the results
        val jobs = mutableListOf<Job>()
        var fetchCount = 0

        for (jobId in jobIds) {
            db.collection("jobs")
                .document(jobId)
                .get()
                .addOnSuccessListener { document ->
                    fetchCount++
                    
                    if (document.exists()) {
                        val job = document.toObject(Job::class.java)
                        job?.let {
                            // Add the document ID to the job object
                            jobs.add(it.copy(id = document.id))
                        }
                    }

                    // If we've fetched all jobs, update the UI
                    if (fetchCount == jobIds.size) {
                        updateUI(jobs)
                    }
                }
                .addOnFailureListener { e ->
                    fetchCount++
                    Log.e(TAG, "Error fetching job details for ID: $jobId", e)
                    
                    // If we've fetched all jobs, update the UI
                    if (fetchCount == jobIds.size) {
                        updateUI(jobs)
                    }
                }
        }
    }

    private fun updateUI(jobs: List<Job>) {
        hideLoading()
        
        if (jobs.isEmpty()) {
            showEmptyState("You haven't saved any jobs yet")
            return
        }

        // Sort jobs by posted date (newest first)
        val sortedJobs = jobs.sortedByDescending { it.postedDate.toDate() }
        
        // Update the adapter
        jobsAdapter.submitList(sortedJobs)
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        hideLoading()
        findViewById<android.widget.TextView>(R.id.emptyStateText).text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingView.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
