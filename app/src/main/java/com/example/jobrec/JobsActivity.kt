package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
            val intent = Intent(this, JobDetailActivity::class.java)
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
        currentQuery.get().addOnSuccessListener { documents ->
            val jobs = documents.mapNotNull { it.toObject(Job::class.java) }
            jobsAdapter.submitList(jobs)
        }
    }

    private fun filterJobs() {
        var query: Query = db.collection("jobs")

        // Apply search filter
        val searchText = searchEditText.text.toString()
        if (searchText.isNotEmpty()) {
            query = query.whereGreaterThanOrEqualTo("title", searchText)
                .whereLessThanOrEqualTo("title", searchText + '\uf8ff')
        }

        // Apply type filters
        val selectedChips = filterChipGroup.checkedChipIds
        if (selectedChips.isNotEmpty()) {
            val types = selectedChips.map { chipId ->
                when (chipId) {
                    R.id.filterFullTime -> "Full-time"
                    R.id.filterPartTime -> "Part-time"
                    R.id.filterRemote -> "Remote"
                    else -> null
                }
            }.filterNotNull()

            if (types.isNotEmpty()) {
                query = query.whereIn("type", types)
            }
        }

        currentQuery = query
        loadJobs()
    }
} 