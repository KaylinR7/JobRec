package com.example.jobrec

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CompanyAnalyticsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyId: String

    // UI elements
    private lateinit var pendingApplicationsText: TextView
    private lateinit var acceptedApplicationsText: TextView
    private lateinit var rejectedApplicationsText: TextView
    private lateinit var totalApplicationsText: TextView
    private lateinit var activeJobsText: TextView
    private lateinit var totalJobsText: TextView
    private lateinit var recentActivityRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_analytics)

        // Initialize Firestore and Firebase Auth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get company ID from intent or find it using the current user's email
        companyId = intent.getStringExtra("companyId") ?: run {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Query companies collection to find the company with matching email
                db.collection("companies")
                    .whereEqualTo("email", currentUser.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            // Get the registration number from the document
                            val registrationNumber = documents.documents[0].getString("registrationNumber")
                            if (registrationNumber != null) {
                                companyId = registrationNumber
                                loadAnalytics()
                            }
                        }
                    }
                return@run ""
            } else {
                return@run ""
            }
        }

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize UI elements
        initializeViews()

        // Load analytics data if we have the companyId directly
        if (companyId.isNotEmpty()) {
            loadAnalytics()
        }
    }

    private fun initializeViews() {
        pendingApplicationsText = findViewById(R.id.pendingApplicationsText)
        acceptedApplicationsText = findViewById(R.id.acceptedApplicationsText)
        rejectedApplicationsText = findViewById(R.id.rejectedApplicationsText)
        totalApplicationsText = findViewById(R.id.totalApplicationsText)
        activeJobsText = findViewById(R.id.activeJobsText)
        totalJobsText = findViewById(R.id.totalJobsText)
        recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView)

        recentActivityRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadAnalytics() {
        // Load applications statistics
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val totalApplications = documents.size()
                val pendingApplications = documents.count { it.getString("status") == "pending" }
                val acceptedApplications = documents.count { it.getString("status") == "accepted" }
                val rejectedApplications = documents.count { it.getString("status") == "rejected" }

                totalApplicationsText.text = totalApplications.toString()
                pendingApplicationsText.text = pendingApplications.toString()
                acceptedApplicationsText.text = acceptedApplications.toString()
                rejectedApplicationsText.text = rejectedApplications.toString()
            }

        // Load jobs statistics
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val totalJobs = documents.size()
                val activeJobs = documents.count { it.getString("status") == "active" }

                totalJobsText.text = totalJobs.toString()
                activeJobsText.text = activeJobs.toString()
            }

        // Load recent activity
        loadRecentActivity()
    }

    private fun loadRecentActivity() {
        // TODO: Implement recent activity loading
        // This could show recent applications, job postings, etc.
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 