package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployerJobsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmployerJobsAdapter
    private lateinit var companyId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_jobs)

        // Setup toolbar with black back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Manage Jobs"
        }

        // Set white navigation icon for red toolbar
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: run {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.employerJobsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = EmployerJobsAdapter(
            emptyList(),
            onEditClick = { job -> editJob(job) },
            onDeleteClick = { job -> showDeleteConfirmation(job) }
        )
        recyclerView.adapter = adapter

        // Setup FAB
        findViewById<FloatingActionButton>(R.id.addJobFab)?.setOnClickListener {
            val intent = Intent(this, PostJobActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }

        // Load jobs
        loadJobs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadJobs() // Refresh jobs when returning to this activity
    }

    private fun loadJobs() {
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { doc ->
                    doc.toObject(Job::class.java).copy(id = doc.id)
                }
                adapter.updateJobs(jobs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editJob(job: Job) {
        val intent = Intent(this, EditJobActivity::class.java)
        intent.putExtra("jobId", job.id)
        intent.putExtra("companyId", companyId)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(job: Job) {
        AlertDialog.Builder(this)
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job posting?")
            .setPositiveButton("Delete") { _, _ ->
                deleteJob(job)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteJob(job: Job) {
        db.collection("jobs")
            .document(job.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Job deleted successfully", Toast.LENGTH_SHORT).show()
                loadJobs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}