package com.example.jobrec

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class EditJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobId: String
    private lateinit var companyId: String

    // UI elements
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var jobTypeInput: AutoCompleteTextView
    private lateinit var locationInput: TextInputEditText
    private lateinit var salaryInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var statusInput: AutoCompleteTextView
    private lateinit var updateJobButton: Button
    private lateinit var deleteJobButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_job)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get job ID and company ID from intent
        jobId = intent.getStringExtra("jobId") ?: ""
        companyId = intent.getStringExtra("companyId") ?: ""
        
        if (jobId.isEmpty() || companyId.isEmpty()) {
            Toast.makeText(this, "Error: Job ID or Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI elements
        initializeViews()
        setupDropdowns()
        setupButtons()
        loadJobData()
    }

    private fun initializeViews() {
        jobTitleInput = findViewById(R.id.jobTitleInput)
        jobTypeInput = findViewById(R.id.jobTypeInput)
        locationInput = findViewById(R.id.locationInput)
        salaryInput = findViewById(R.id.salaryInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        requirementsInput = findViewById(R.id.requirementsInput)
        statusInput = findViewById(R.id.statusInput)
        updateJobButton = findViewById(R.id.updateJobButton)
        deleteJobButton = findViewById(R.id.deleteJobButton)
    }

    private fun setupDropdowns() {
        // Setup job type dropdown
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val jobTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        jobTypeInput.setAdapter(jobTypeAdapter)

        // Setup status dropdown
        val statusTypes = arrayOf("active", "closed", "draft")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusTypes)
        statusInput.setAdapter(statusAdapter)
    }

    private fun setupButtons() {
        updateJobButton.setOnClickListener {
            if (validateInputs()) {
                updateJob()
            }
        }

        deleteJobButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadJobData() {
        db.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val data = document.data
                    
                    // Check if the job belongs to the company
                    if (data?.get("companyId") != companyId) {
                        Toast.makeText(this, "Error: You don't have permission to edit this job", Toast.LENGTH_SHORT).show()
                        finish()
                        return@addOnSuccessListener
                    }
                    
                    // Fill the form with job data
                    jobTitleInput.setText(data["title"] as? String ?: "")
                    jobTypeInput.setText(data["type"] as? String ?: "")
                    locationInput.setText(data["location"] as? String ?: "")
                    salaryInput.setText(data["salary"] as? String ?: "")
                    descriptionInput.setText(data["description"] as? String ?: "")
                    requirementsInput.setText(data["requirements"] as? String ?: "")
                    statusInput.setText(data["status"] as? String ?: "Active")
                } else {
                    Toast.makeText(this, "Error: Job not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading job: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (jobTitleInput.text.isNullOrBlank()) {
            jobTitleInput.error = "Job title is required"
            isValid = false
        }

        if (jobTypeInput.text.isNullOrBlank()) {
            jobTypeInput.error = "Job type is required"
            isValid = false
        }

        if (locationInput.text.isNullOrBlank()) {
            locationInput.error = "Location is required"
            isValid = false
        }

        if (salaryInput.text.isNullOrBlank()) {
            salaryInput.error = "Salary is required"
            isValid = false
        }

        if (descriptionInput.text.isNullOrBlank()) {
            descriptionInput.error = "Job description is required"
            isValid = false
        }

        if (requirementsInput.text.isNullOrBlank()) {
            requirementsInput.error = "Requirements are required"
            isValid = false
        }

        if (statusInput.text.isNullOrBlank()) {
            statusInput.error = "Status is required"
            isValid = false
        }

        return isValid
    }

    private fun updateJob() {
        val jobUpdates = hashMapOf<String, Any>(
            "title" to jobTitleInput.text.toString(),
            "type" to jobTypeInput.text.toString(),
            "location" to locationInput.text.toString(),
            "salary" to salaryInput.text.toString(),
            "description" to descriptionInput.text.toString(),
            "requirements" to requirementsInput.text.toString(),
            "status" to statusInput.text.toString()
        )

        db.collection("jobs")
            .document(jobId)
            .update(jobUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Job updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating job: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteJob()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteJob() {
        db.collection("jobs")
            .document(jobId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Job deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting job: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
} 