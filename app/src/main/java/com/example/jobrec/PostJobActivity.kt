package com.example.jobrec

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class PostJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String

    // UI elements
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var jobTypeInput: AutoCompleteTextView
    private lateinit var locationInput: TextInputEditText
    private lateinit var salaryInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var postJobButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_job)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Post a Job"
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isEmpty()) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI elements
        initializeViews()
        setupJobTypeDropdown()
        setupPostJobButton()
    }

    private fun initializeViews() {
        jobTitleInput = findViewById(R.id.jobTitleInput)
        jobTypeInput = findViewById(R.id.jobTypeInput)
        locationInput = findViewById(R.id.locationInput)
        salaryInput = findViewById(R.id.salaryInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        requirementsInput = findViewById(R.id.requirementsInput)
        postJobButton = findViewById(R.id.postJobButton)
    }

    private fun setupJobTypeDropdown() {
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        jobTypeInput.setAdapter(adapter)
    }

    private fun setupPostJobButton() {
        postJobButton.setOnClickListener {
            if (validateInputs()) {
                postJob()
            }
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

        return isValid
    }

    private fun postJob() {
        val job = hashMapOf(
            "title" to jobTitleInput.text.toString(),
            "type" to jobTypeInput.text.toString(),
            "location" to locationInput.text.toString(),
            "salary" to salaryInput.text.toString(),
            "description" to descriptionInput.text.toString(),
            "requirements" to requirementsInput.text.toString(),
            "companyId" to companyId,
            "postedAt" to Date(),
            "status" to "active"
        )

        db.collection("jobs")
            .add(job)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Job posted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error posting job: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 