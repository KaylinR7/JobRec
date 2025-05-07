package com.example.jobrec

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CompanyApplicationDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobTitleText: TextView
    private lateinit var applicantNameText: TextView
    private lateinit var applicantEmailText: TextView
    private lateinit var applicantPhoneText: TextView
    private lateinit var statusText: TextView
    private lateinit var appliedDateText: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var viewResumeButton: Button
    private var applicationId: String? = null
    private var resumeUrl: String? = null
    private var applicantId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_application_details)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Application Review"
        }

        // Get the application ID from intent
        applicationId = intent.getStringExtra("applicationId")

        // Initialize views
        jobTitleText = findViewById(R.id.jobTitleText)
        applicantNameText = findViewById(R.id.applicantNameText)
        applicantEmailText = findViewById(R.id.applicantEmailText)
        applicantPhoneText = findViewById(R.id.applicantPhoneText)
        statusText = findViewById(R.id.statusText)
        appliedDateText = findViewById(R.id.appliedDateText)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)
        viewResumeButton = findViewById(R.id.viewResumeButton)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        loadApplicationDetails()
        setupButtons()
    }

    private fun setupButtons() {
        acceptButton.setOnClickListener {
            updateApplicationStatus("accepted")
        }

        rejectButton.setOnClickListener {
            updateApplicationStatus("rejected")
        }

        viewResumeButton.setOnClickListener {
            if (!resumeUrl.isNullOrEmpty()) {
                if (resumeUrl!!.startsWith("http")) {
                    // If it's a direct URL (PDF from storage)
                    openResume(resumeUrl!!)
                } else {
                    // If it's a Firestore document ID (CV content)
                    loadCvContent(resumeUrl!!)
                }
            } else {
                Toast.makeText(this, "No resume available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCvContent(cvId: String) {
        db.collection("cvs")
            .document(cvId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val cvContent = document.getString("content")
                    if (!cvContent.isNullOrEmpty()) {
                        showCvContent(cvContent)
                    } else {
                        Toast.makeText(this, "No resume content available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCvContent(content: String) {
        val intent = Intent(this, ViewCvActivity::class.java).apply {
            putExtra("cvContent", content)
        }
        startActivity(intent)
    }

    private fun openResume(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer app found. Please install a PDF viewer.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadApplicationDetails() {
        applicationId?.let { id ->
            db.collection("applications").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Update UI with application details
                        jobTitleText.text = document.getString("jobTitle")
                        applicantNameText.text = document.getString("applicantName")
                        applicantEmailText.text = document.getString("applicantEmail") ?: "Not provided"
                        applicantPhoneText.text = document.getString("applicantPhone") ?: "Not provided"
                        
                        val status = document.getString("status") ?: "pending"
                        statusText.text = status.capitalize()
                        statusText.setTextColor(getStatusColor(status))
                        
                        // Get resume URL and applicant ID
                        resumeUrl = document.getString("resumeUrl")
                        applicantId = document.getString("userId")
                        
                        // Format the date
                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        appliedDateText.text = timestamp?.let { dateFormat.format(it) }

                        // Update button states based on current status
                        val isActionable = status == "pending" || status == "reviewed"
                        acceptButton.isEnabled = isActionable
                        rejectButton.isEnabled = isActionable
                        
                        // Always enable the view resume button
                        viewResumeButton.isEnabled = true
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading application: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateApplicationStatus(newStatus: String) {
        applicationId?.let { id ->
            db.collection("applications").document(id)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Application $newStatus", Toast.LENGTH_SHORT).show()
                    loadApplicationDetails() // Reload to update UI
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating application: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> resources.getColor(R.color.status_pending, theme)
            "accepted" -> resources.getColor(R.color.status_accepted, theme)
            "rejected" -> resources.getColor(R.color.status_rejected, theme)
            else -> resources.getColor(R.color.status_pending, theme)
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