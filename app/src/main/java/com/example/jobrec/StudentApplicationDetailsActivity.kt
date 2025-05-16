package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class StudentApplicationDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobTitleText: TextView
    private lateinit var companyNameText: TextView
    private lateinit var jobLocationText: TextView
    private lateinit var statusChip: Chip
    private lateinit var statusDescriptionText: TextView
    private lateinit var appliedDateText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var chatButton: Button
    private lateinit var viewJobDetailsButton: Button
    private lateinit var jobTypeContainer: LinearLayout
    private lateinit var jobTypeText: TextView
    private lateinit var salaryContainer: LinearLayout
    private lateinit var salaryText: TextView

    private var applicationId: String? = null
    private var jobId: String? = null
    private var companyId: String? = null
    private var companyName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_application_details)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Application Details"
        }

        // Explicitly set white navigation icon
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)

        // Get the application ID from intent
        applicationId = intent.getStringExtra("applicationId")

        // Initialize views
        jobTitleText = findViewById(R.id.jobTitleText)
        companyNameText = findViewById(R.id.companyNameText)
        jobLocationText = findViewById(R.id.jobLocationText)
        statusChip = findViewById(R.id.statusChip)
        statusDescriptionText = findViewById(R.id.statusDescriptionText)
        appliedDateText = findViewById(R.id.appliedDateText)
        lastUpdatedText = findViewById(R.id.lastUpdatedText)
        chatButton = findViewById(R.id.chatButton)
        viewJobDetailsButton = findViewById(R.id.viewJobDetailsButton)
        jobTypeContainer = findViewById(R.id.jobTypeContainer)
        jobTypeText = findViewById(R.id.jobTypeText)
        salaryContainer = findViewById(R.id.salaryContainer)
        salaryText = findViewById(R.id.salaryText)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        loadApplicationDetails()
        setupButtons()
    }

    private fun loadApplicationDetails() {
        applicationId?.let { id ->
            db.collection("applications").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Update UI with application details
                        jobTitleText.text = document.getString("jobTitle")

                        companyName = document.getString("companyName")
                        companyId = document.getString("companyId")

                        // If company name is missing but we have company ID, load it from companies collection
                        if (companyName.isNullOrEmpty() && !companyId.isNullOrEmpty()) {
                            loadCompanyName(companyId!!)
                        } else {
                            companyNameText.text = companyName ?: "Unknown Company"
                        }

                        // Get job details from the job document
                        jobId = document.getString("jobId")
                        companyId = document.getString("companyId")

                        if (!jobId.isNullOrEmpty()) {
                            loadJobDetails(jobId!!)
                        }

                        val status = document.getString("status") ?: "pending"
                        updateStatusUI(status)

                        // Format dates
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                        // Applied date (timestamp)
                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        appliedDateText.text = timestamp?.let { dateFormat.format(it) } ?: "Not available"

                        // Last updated date
                        val lastUpdated = document.getTimestamp("lastUpdated")?.toDate()
                        lastUpdatedText.text = lastUpdated?.let { dateFormat.format(it) } ?: "Not available"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading application: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadJobDetails(jobId: String) {
        db.collection("jobs").document(jobId)
            .get()
            .addOnSuccessListener { jobDoc ->
                if (jobDoc != null && jobDoc.exists()) {
                    // Basic job details
                    jobLocationText.text = jobDoc.getString("location") ?: "Location not specified"

                    // Job type (if available)
                    val jobType = jobDoc.getString("jobType")
                    if (!jobType.isNullOrEmpty()) {
                        jobTypeText.text = jobType
                        jobTypeContainer.visibility = View.VISIBLE
                    } else {
                        jobTypeContainer.visibility = View.GONE
                    }

                    // Salary information (if available)
                    val salaryMin = jobDoc.getLong("salaryMin")
                    val salaryMax = jobDoc.getLong("salaryMax")
                    val salaryCurrency = jobDoc.getString("salaryCurrency") ?: "$"
                    val salaryPeriod = jobDoc.getString("salaryPeriod") ?: "year"

                    if (salaryMin != null && salaryMax != null) {
                        val formattedSalary = "$salaryCurrency${salaryMin.toInt().formatWithCommas()} - $salaryCurrency${salaryMax.toInt().formatWithCommas()} per $salaryPeriod"
                        salaryText.text = formattedSalary
                        salaryContainer.visibility = View.VISIBLE
                    } else {
                        salaryContainer.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading job details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun Int.formatWithCommas(): String {
        return String.format("%,d", this)
    }

    private fun loadCompanyName(companyId: String) {
        db.collection("companies").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("companyName") ?: document.getString("name")
                    if (!name.isNullOrEmpty()) {
                        companyName = name
                        companyNameText.text = name
                    } else {
                        companyNameText.text = "Unknown Company"
                    }
                } else {
                    companyNameText.text = "Unknown Company"
                }
            }
            .addOnFailureListener { e ->
                companyNameText.text = "Unknown Company"
                Log.e("StudentApplicationDetailsActivity", "Error loading company: ${e.message}")
            }
    }

    private fun updateStatusUI(status: String) {
        // Update chip text and background color
        statusChip.text = status.capitalize()
        statusChip.chipBackgroundColor = getColorStateList(getStatusColor(status))

        // Show chat button only if application is accepted
        if (status == "accepted") {
            chatButton.visibility = View.VISIBLE
        } else {
            chatButton.visibility = View.GONE
        }

        // Set status description based on status
        statusDescriptionText.text = getStatusDescription(status)
    }

    private fun getStatusDescription(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Your application is being reviewed by the employer."
            "accepted" -> "Congratulations! Your application has been accepted."
            "rejected" -> "Unfortunately, your application was not selected for this position."
            "shortlisted" -> "You've been shortlisted for this position. The employer may contact you soon."
            "interviewing" -> "You're in the interview process for this position."
            "offered" -> "You've received a job offer for this position."
            else -> "Your application status is: ${status.capitalize()}"
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> R.color.status_pending
            "accepted" -> R.color.status_accepted
            "rejected" -> R.color.status_rejected
            "shortlisted" -> R.color.status_shortlisted
            "interviewing" -> R.color.status_interviewing
            "offered" -> R.color.status_offered
            else -> R.color.status_pending
        }
    }

    private fun setupButtons() {
        // Chat button setup
        chatButton.setOnClickListener {
            if (applicationId != null) {
                // Open chat activity with this application
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("applicationId", applicationId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot start chat: Missing application information", Toast.LENGTH_SHORT).show()
            }
        }

        // View job details button setup
        viewJobDetailsButton.setOnClickListener {
            if (jobId != null) {
                // Open job details activity
                val intent = Intent(this, JobDetailsActivity::class.java).apply {
                    putExtra("jobId", jobId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot view job details: Missing job information", Toast.LENGTH_SHORT).show()
            }
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