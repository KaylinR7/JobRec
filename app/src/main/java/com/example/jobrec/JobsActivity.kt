package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class JobsActivity : AppCompatActivity() {
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var jobsAdapter: JobsAdapter
    private val db = FirebaseFirestore.getInstance()
    private var currentQuery: Query = db.collection("jobs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)

        // Initialize views
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        filterChipGroup = findViewById(R.id.filterChipGroup)

        // Setup RecyclerView
        jobsAdapter = JobsAdapter { job ->
            // Handle job item click
            val intent = Intent(this, JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }
        jobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@JobsActivity)
            adapter = jobsAdapter
        }

        // Setup search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterJobs()
            }
        })

        // Setup filter chips
        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            filterJobs()
        }

        // Load initial jobs
        loadJobs()
    }

    private fun loadJobs() {
        currentQuery.get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { it.toObject(Job::class.java) }
                jobsAdapter.submitList(jobs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                jobsAdapter.submitList(emptyList())
            }
    }

    private fun filterJobs() {
        var query: Query = db.collection("jobs")
            .whereEqualTo("status", "active")

        // Apply search filter
        val searchText = searchEditText.text.toString().trim().lowercase()
        
        // Show loading state
        jobsAdapter.submitList(null)
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE

        query.get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { it.toObject(Job::class.java) }
                
                // Apply search filter in memory
                val filteredJobs = if (searchText.isNotEmpty()) {
                    jobs.filter { job ->
                        job.title.lowercase().contains(searchText) ||
                        job.description.lowercase().contains(searchText) ||
                        job.location.lowercase().contains(searchText) ||
                        job.type.lowercase().contains(searchText)
                    }
                } else {
                    jobs
                }

                // Apply type filters
                val selectedChips = filterChipGroup.checkedChipIds
                val finalJobs = if (selectedChips.isNotEmpty()) {
                    val types = selectedChips.map { chipId ->
                        when (chipId) {
                            R.id.filterFullTime -> "Full-time"
                            R.id.filterPartTime -> "Part-time"
                            R.id.filterRemote -> "Remote"
                            else -> null
                        }
                    }.filterNotNull()

                    if (types.isNotEmpty()) {
                        filteredJobs.filter { job -> types.contains(job.type) }
                    } else {
                        filteredJobs
                    }
                } else {
                    filteredJobs
                }

                // Update UI
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                jobsAdapter.submitList(finalJobs)
                
                // Show empty state if no results
                if (finalJobs.isEmpty()) {
                    findViewById<View>(R.id.emptyStateLayout).visibility = View.VISIBLE
                    findViewById<View>(R.id.jobsRecyclerView).visibility = View.GONE
                } else {
                    findViewById<View>(R.id.emptyStateLayout).visibility = View.GONE
                    findViewById<View>(R.id.jobsRecyclerView).visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                Toast.makeText(this, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                jobsAdapter.submitList(emptyList())
                findViewById<View>(R.id.emptyStateLayout).visibility = View.VISIBLE
                findViewById<View>(R.id.jobsRecyclerView).visibility = View.GONE
            }
    }
} 