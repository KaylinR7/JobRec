package com.example.jobrec

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicationDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobTitleText: TextView
    private lateinit var companyNameText: TextView
    private lateinit var applicantNameText: TextView
    private lateinit var applicantEmailText: TextView
    private lateinit var applicantPhoneText: TextView
    private lateinit var statusText: TextView
    private lateinit var appliedDateText: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var actionButtonsLayout: LinearLayout
    private var applicationId: String? = null
    private var isStudentView: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_details)

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

        // Get the application ID and view type from intent
        applicationId = intent.getStringExtra("applicationId")
        isStudentView = intent.getBooleanExtra("isStudentView", false)

        // Initialize views
        jobTitleText = findViewById(R.id.jobTitleText)
        companyNameText = findViewById(R.id.companyNameText)
        applicantNameText = findViewById(R.id.applicantNameText)
        applicantEmailText = findViewById(R.id.applicantEmailText)
        applicantPhoneText = findViewById(R.id.applicantPhoneText)
        statusText = findViewById(R.id.statusText)
        appliedDateText = findViewById(R.id.appliedDateText)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout)

        // Hide action buttons for student view
        actionButtonsLayout.visibility = if (isStudentView) View.GONE else View.VISIBLE

        if (!isStudentView) {
            setupActionButtons()
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        loadApplicationDetails()
    }

    private fun setupActionButtons() {
        acceptButton.setOnClickListener {
            updateApplicationStatus("accepted")
        }

        rejectButton.setOnClickListener {
            updateApplicationStatus("rejected")
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
                        companyNameText.text = document.getString("companyName")
                        applicantNameText.text = document.getString("applicantName")
                        applicantEmailText.text = document.getString("applicantEmail")
                        applicantPhoneText.text = document.getString("applicantPhone")

                        val status = document.getString("status") ?: "pending"
                        statusText.text = status.capitalize()
                        statusText.setTextColor(getStatusColor(status))

                        // Format the date
                        val timestamp = document.getTimestamp("timestamp")?.toDate()
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        appliedDateText.text = timestamp?.let { dateFormat.format(it) }

                        // Update button states based on current status
                        if (!isStudentView) {
                            acceptButton.isEnabled = status == "pending" || status == "reviewed"
                            rejectButton.isEnabled = status == "pending" || status == "reviewed"
                        }
                    } else {
                        Toast.makeText(this, "Application not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading application: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun updateApplicationStatus(newStatus: String) {
        applicationId?.let { id ->
            db.collection("applications").document(id)
                .update("status", newStatus)
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