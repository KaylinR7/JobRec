package com.example.jobrec

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import com.example.jobrec.databinding.ActivityPostJobBinding

class PostJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var binding: ActivityPostJobBinding

    // UI elements
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var jobTypeInput: AutoCompleteTextView
    private lateinit var locationInput: TextInputEditText
    private lateinit var salaryInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var postButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Post New Job"
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
        setupPostButton()
    }

    private fun initializeViews() {
        jobTitleInput = binding.jobTitleInput
        jobTypeInput = binding.jobTypeInput
        locationInput = binding.locationInput
        salaryInput = binding.salaryInput
        descriptionInput = binding.descriptionInput
        requirementsInput = binding.requirementsInput
        postButton = binding.postButton
    }

    private fun setupJobTypeDropdown() {
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        jobTypeInput.setAdapter(adapter)
    }

    private fun setupPostButton() {
        postButton.setOnClickListener {
            if (validateInputs()) {
                // First get the company's profile information
                db.collection("companies")
                    .document(companyId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val company = document.toObject(Company::class.java)
                            company?.let { postJob(it) }
                        } else {
                            Toast.makeText(this, "Company profile not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error loading company profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
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

    private fun postJob(company: Company) {
        val job = hashMapOf(
            "title" to jobTitleInput.text.toString(),
            "companyId" to companyId,
            "companyName" to company.companyName,
            "location" to locationInput.text.toString(),
            "salary" to salaryInput.text.toString(),
            "type" to jobTypeInput.text.toString(),
            "description" to descriptionInput.text.toString(),
            "requirements" to requirementsInput.text.toString(),
            "postedDate" to com.google.firebase.Timestamp.now(),
            "status" to "active"
        )

        db.collection("jobs")
            .add(job)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Job posted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 