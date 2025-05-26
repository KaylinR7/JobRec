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
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        companyId = intent.getStringExtra("companyId") ?: run {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("companies")
                    .whereEqualTo("email", currentUser.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
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
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        initializeViews()
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
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val totalJobs = documents.size()
                val activeJobs = documents.count { it.getString("status") == "active" }
                totalJobsText.text = totalJobs.toString()
                activeJobsText.text = activeJobs.toString()
            }
        loadRecentActivity()
    }
    private fun loadRecentActivity() {
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 