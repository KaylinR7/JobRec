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
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.ArrayAdapter

class JobsActivity : AppCompatActivity() {
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var jobsAdapter: JobsAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fieldFilterInput: MaterialAutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)

        // Initialize views
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        fieldFilterInput = findViewById(R.id.fieldFilterInput)

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

        // Setup field filter
        setupFieldFilter()

        // Load initial jobs
        loadJobs()
    }

    private fun setupFieldFilter() {
        val fieldOptions = arrayOf(
            "Information Technology",
            "Healthcare",
            "Law",
            "Education",
            "Engineering",
            "Business",
            "Finance",
            "Marketing",
            "Sales",
            "Customer Service",
            "Manufacturing",
            "Construction",
            "Transportation",
            "Hospitality",
            "Other"
        )
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldFilterInput.setAdapter(fieldAdapter)

        // Add listener for field filter changes
        fieldFilterInput.setOnItemClickListener { _, _, position, _ ->
            filterJobs()
        }
    }

    private fun loadJobs() {
        // Simple query without complex filters or ordering
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                val allJobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java)
                        job.id = doc.id
                        // Filter active jobs in memory
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } // Sort in memory

                jobsAdapter.submitList(allJobs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                jobsAdapter.submitList(emptyList())
            }
    }



    private fun filterJobs() {
        // Get search filter
        val searchText = searchEditText.text.toString().trim().lowercase()
        val selectedField = fieldFilterInput.text.toString().trim()

        // Show loading state
        jobsAdapter.submitList(null)
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE

        // Simple query without complex filters or ordering
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                // Get all jobs and filter in memory
                val allJobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java)
                        job.id = doc.id
                        // Only include active jobs
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } // Sort in memory

                // Apply field filter in memory
                val fieldFilteredJobs = if (selectedField.isNotEmpty()) {
                    allJobs.filter { job -> job.jobField.equals(selectedField, ignoreCase = true) }
                } else {
                    allJobs
                }

                // Apply search filter in memory
                val searchFilteredJobs = if (searchText.isNotEmpty()) {
                    fieldFilteredJobs.filter { job ->
                        job.title.lowercase().contains(searchText) ||
                                job.description.lowercase().contains(searchText) ||
                                job.location.lowercase().contains(searchText) ||
                                job.type.lowercase().contains(searchText)
                    }
                } else {
                    fieldFilteredJobs
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
                        searchFilteredJobs.filter { job -> types.contains(job.type) }
                    } else {
                        searchFilteredJobs
                    }
                } else {
                    searchFilteredJobs
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