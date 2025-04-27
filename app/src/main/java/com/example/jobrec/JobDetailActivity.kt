package com.example.jobrec

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JobDetailActivity : AppCompatActivity() {
    private lateinit var jobTitle: TextView
    private lateinit var companyName: TextView
    private lateinit var jobLocation: TextView
    private lateinit var jobType: TextView
    private lateinit var jobSalary: TextView
    private lateinit var jobDescription: TextView
    private lateinit var jobRequirements: TextView
    private lateinit var applyButton: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_detail)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Job Details"
        }

        // Initialize views
        jobTitle = findViewById(R.id.jobTitle)
        companyName = findViewById(R.id.companyName)
        jobLocation = findViewById(R.id.jobLocation)
        jobType = findViewById(R.id.jobType)
        jobSalary = findViewById(R.id.jobSalary)
        jobDescription = findViewById(R.id.jobDescription)
        jobRequirements = findViewById(R.id.jobRequirements)
        applyButton = findViewById(R.id.applyButton)

        // Set up back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Get job ID from intent
        val jobId = intent.getStringExtra("jobId")
        if (jobId != null) {
            loadJobDetails(jobId)
        } else {
            Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup apply button
        applyButton.setOnClickListener {
            if (auth.currentUser != null) {
                applyForJob(jobId!!)
            } else {
                Toast.makeText(this, "Please sign in to apply", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to login screen
            }
        }
    }

    private fun loadJobDetails(jobId: String) {
        db.collection("jobs").document(jobId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val job = document.toObject(Job::class.java)
                    job?.let { displayJobDetails(it) }
                } else {
                    Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading job details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayJobDetails(job: Job) {
        jobTitle.text = job.title
        companyName.text = job.companyName
        jobLocation.text = job.location
        jobType.text = job.type
        jobSalary.text = job.salary
        jobDescription.text = job.description
        jobRequirements.text = job.getRequirementsList().joinToString("\n• ", "• ")
    }

    private fun applyForJob(jobId: String) {
        val userId = auth.currentUser?.uid ?: return
        val application = hashMapOf(
            "userId" to userId,
            "jobId" to jobId,
            "status" to "pending",
            "appliedDate" to System.currentTimeMillis()
        )

        db.collection("applications")
            .add(application)
            .addOnSuccessListener {
                Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
                applyButton.isEnabled = false
                applyButton.text = "Applied"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error submitting application", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 