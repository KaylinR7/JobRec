package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class StudentApplicationDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobTitleText: TextView
    private lateinit var companyNameText: TextView
    private lateinit var jobLocationText: TextView
    private lateinit var statusText: TextView
    private lateinit var appliedDateText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var chatButton: Button
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
            title = "My Application"
        }

        // Get the application ID from intent
        applicationId = intent.getStringExtra("applicationId")

        // Initialize views
        jobTitleText = findViewById(R.id.jobTitleText)
        companyNameText = findViewById(R.id.companyNameText)
        jobLocationText = findViewById(R.id.jobLocationText)
        statusText = findViewById(R.id.statusText)
        appliedDateText = findViewById(R.id.appliedDateText)
        lastUpdatedText = findViewById(R.id.lastUpdatedText)
        chatButton = findViewById(R.id.chatButton)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        loadApplicationDetails()
        setupChatButton()
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
                        companyNameText.text = companyName

                        // Get job location from the job document
                        jobId = document.getString("jobId")
                        companyId = document.getString("companyId")

                        if (!jobId.isNullOrEmpty()) {
                            db.collection("jobs").document(jobId!!)
                                .get()
                                .addOnSuccessListener { jobDoc ->
                                    if (jobDoc != null && jobDoc.exists()) {
                                        jobLocationText.text = jobDoc.getString("location") ?: "Location not specified"
                                    }
                                }
                        }

                        val status = document.getString("status") ?: "pending"
                        statusText.text = status.capitalize()
                        statusText.setTextColor(getStatusColor(status))

                        // Show chat button only if application is accepted
                        if (status == "accepted") {
                            chatButton.visibility = View.VISIBLE
                        } else {
                            chatButton.visibility = View.GONE
                        }

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
                    android.widget.Toast.makeText(this, "Error loading application: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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

    private fun setupChatButton() {
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}