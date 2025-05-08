package com.example.jobrec

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class EditJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobId: String
    private lateinit var companyId: String

    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var typeInput: TextInputEditText
    private lateinit var salaryInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_job)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Job"
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Get job ID and company ID from intent
        jobId = intent.getStringExtra("jobId") ?: run {
            Toast.makeText(this, "Error: Job ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        companyId = intent.getStringExtra("companyId") ?: run {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        titleInput = findViewById(R.id.titleInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        requirementsInput = findViewById(R.id.requirementsInput)
        locationInput = findViewById(R.id.locationInput)
        typeInput = findViewById(R.id.typeInput)
        salaryInput = findViewById(R.id.salaryInput)
        saveButton = findViewById(R.id.saveButton)

        // Load job data
        loadJobData()

        // Setup save button
        saveButton.setOnClickListener {
            saveJob()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadJobData() {
        db.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val job = document.toObject(Job::class.java)
                    job?.let {
                        titleInput.setText(it.title)
                        descriptionInput.setText(it.description)
                        requirementsInput.setText(it.requirements)
                        locationInput.setText(it.location)
                        typeInput.setText(it.type)
                        salaryInput.setText(it.salary)
                    }
                } else {
                    Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading job: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun saveJob() {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val requirements = requirementsInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val type = typeInput.text.toString().trim()
        val salary = salaryInput.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || requirements.isEmpty() || 
            location.isEmpty() || type.isEmpty() || salary.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val jobData = mapOf(
            "title" to title,
            "description" to description,
            "requirements" to requirements,
            "location" to location,
            "type" to type,
            "salary" to salary,
            "companyId" to companyId,
            "status" to "active"
        )

        db.collection("jobs")
            .document(jobId)
            .update(jobData)
            .addOnSuccessListener {
                Toast.makeText(this, "Job updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 