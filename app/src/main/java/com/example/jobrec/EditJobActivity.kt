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

        // First get the current job data to preserve fields we're not updating
        db.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get existing job data
                    val existingJob = document.toObject(Job::class.java)

                    // Create updated job object with all fields
                    val updatedJob = existingJob?.copy(
                        title = title,
                        description = description,
                        requirements = requirements,
                        location = location,
                        type = type,
                        salary = salary,
                        companyId = companyId,
                        updatedDate = com.google.firebase.Timestamp.now(),
                        status = "active"
                    ) ?: Job(
                        id = jobId,
                        title = title,
                        description = description,
                        requirements = requirements,
                        location = location,
                        type = type,
                        salary = salary,
                        companyId = companyId,
                        status = "active"
                    )

                    // Convert to map for Firestore (excluding id field which is handled separately)
                    val jobData = mapOf(
                        "title" to updatedJob.title,
                        "description" to updatedJob.description,
                        "requirements" to updatedJob.requirements,
                        "location" to updatedJob.location,
                        "type" to updatedJob.type,
                        "salary" to updatedJob.salary,
                        "companyId" to updatedJob.companyId,
                        "companyName" to (existingJob?.companyName ?: ""),
                        "jobType" to (existingJob?.jobType ?: updatedJob.type),
                        "updatedDate" to updatedJob.updatedDate,
                        "postedDate" to (existingJob?.postedDate ?: updatedJob.postedDate),
                        "status" to updatedJob.status,
                        "jobField" to (existingJob?.jobField ?: ""),
                        "specialization" to (existingJob?.specialization ?: ""),
                        "province" to (existingJob?.province ?: ""),
                        "experienceLevel" to (existingJob?.experienceLevel ?: "")
                    )

                    // Use set with merge option to update the document
                    db.collection("jobs")
                        .document(jobId)
                        .set(jobData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Job updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // If document doesn't exist, create a new one
                    val newJob = Job(
                        id = jobId,
                        title = title,
                        description = description,
                        requirements = requirements,
                        location = location,
                        type = type,
                        salary = salary,
                        companyId = companyId,
                        status = "active"
                    )

                    // Convert to map for Firestore
                    val jobData = mapOf(
                        "title" to newJob.title,
                        "description" to newJob.description,
                        "requirements" to newJob.requirements,
                        "location" to newJob.location,
                        "type" to newJob.type,
                        "salary" to newJob.salary,
                        "companyId" to newJob.companyId,
                        "companyName" to newJob.companyName,
                        "jobType" to newJob.jobType,
                        "updatedDate" to newJob.updatedDate,
                        "postedDate" to newJob.postedDate,
                        "status" to newJob.status,
                        "jobField" to newJob.jobField,
                        "specialization" to newJob.specialization,
                        "province" to newJob.province,
                        "experienceLevel" to newJob.experienceLevel
                    )

                    // Create a new document
                    db.collection("jobs")
                        .document(jobId)
                        .set(jobData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Job created successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error creating job: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving job data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}