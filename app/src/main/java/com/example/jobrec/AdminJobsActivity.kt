package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminJobsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminJobsAdapter
    private lateinit var searchEditText: EditText
    private lateinit var progressIndicator: CircularProgressIndicator
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_jobs)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Manage Jobs"
        }

        recyclerView = findViewById(R.id.recyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        progressIndicator = findViewById(R.id.progressIndicator)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminJobsAdapter(emptyList(),
            onViewClick = { job -> viewJob(job) },
            onEditClick = { job -> editJob(job) },
            onDeleteClick = { job -> deleteJob(job.id) }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.addJobFab)?.setOnClickListener {
            addJob()
        }

        setupSearch()
        loadJobs()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadJobs()
                } else {
                    searchJobs(query)
                }
            }
        })
    }

    private fun loadJobs() {
        progressIndicator.visibility = View.VISIBLE
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { document ->
                    try {
                        document.toObject(Job::class.java).copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                adapter.updateJobs(jobs)
                progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading jobs: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressIndicator.visibility = View.GONE
            }
    }

    private fun searchJobs(query: String) {
        progressIndicator.visibility = View.VISIBLE
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { document ->
                    try {
                        val job = document.toObject(Job::class.java).copy(id = document.id)
                        if (job.title.contains(query, ignoreCase = true) ||
                            job.description.contains(query, ignoreCase = true) ||
                            job.companyName.contains(query, ignoreCase = true)) {
                            job
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                adapter.updateJobs(jobs)
                progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error searching jobs: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressIndicator.visibility = View.GONE
            }
    }

    private fun deleteJob(jobId: String) {
        db.collection("jobs").document(jobId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Job deleted", Toast.LENGTH_SHORT).show()
                loadJobs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addJob() {
        // TODO: Show dialog or activity to add a new job
        Toast.makeText(this, "Add Job (not implemented)", Toast.LENGTH_SHORT).show()
    }

    private fun viewJob(job: Job) {
        val dialog = AdminJobDetailsDialog.newInstance(job)
        dialog.show(supportFragmentManager, "JobDetails")
    }

    private fun editJob(job: Job) {
        // TODO: Show dialog or activity to edit job details
        Toast.makeText(this, "Edit Job: ${job.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}