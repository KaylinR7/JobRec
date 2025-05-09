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
    private lateinit var jobFieldInput: AutoCompleteTextView
    private lateinit var jobTypeInput: AutoCompleteTextView
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var locationInput: TextInputEditText
    private lateinit var salaryRangeInput: AutoCompleteTextView
    private lateinit var experienceInput: AutoCompleteTextView
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
        jobFieldInput = binding.jobFieldInput
        jobTypeInput = binding.jobTypeInput
        provinceInput = binding.provinceInput
        locationInput = binding.locationInput
        salaryRangeInput = binding.salaryRangeInput
        experienceInput = binding.experienceInput
        descriptionInput = binding.descriptionInput
        requirementsInput = binding.requirementsInput
        postButton = binding.postButton
    }

    private fun setupJobTypeDropdown() {
        // Job Field options - same as in signup
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
        jobFieldInput.setAdapter(fieldAdapter)

        // Job Type options
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        jobTypeInput.setAdapter(typeAdapter)

        // Province options - same as in signup
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceInput.setAdapter(provinceAdapter)

        // Salary Range options - same as in signup
        val salaryOptions = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryOptions)
        salaryRangeInput.setAdapter(salaryAdapter)

        // Experience Level options - same as in signup
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearsOptions)
        experienceInput.setAdapter(experienceAdapter)
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

        if (jobFieldInput.text.isNullOrBlank()) {
            jobFieldInput.error = "Job field is required"
            isValid = false
        }

        if (jobTypeInput.text.isNullOrBlank()) {
            jobTypeInput.error = "Job type is required"
            isValid = false
        }

        if (provinceInput.text.isNullOrBlank()) {
            provinceInput.error = "Province is required"
            isValid = false
        }

        if (locationInput.text.isNullOrBlank()) {
            locationInput.error = "Specific location is required"
            isValid = false
        }

        if (salaryRangeInput.text.isNullOrBlank()) {
            salaryRangeInput.error = "Salary range is required"
            isValid = false
        }

        if (experienceInput.text.isNullOrBlank()) {
            experienceInput.error = "Experience level is required"
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
            "jobField" to jobFieldInput.text.toString(),
            "province" to provinceInput.text.toString(),
            "location" to locationInput.text.toString(),
            "salary" to salaryRangeInput.text.toString(),
            "type" to jobTypeInput.text.toString(),
            "experienceLevel" to experienceInput.text.toString(),
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