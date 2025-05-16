package com.example.jobrec

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent

class ApplicationsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var applicationsAdapter: ApplicationAdapter
    private val applications = mutableListOf<Application>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applications)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: run {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup toolbar with black back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Applications"

        // Set white navigation icon for red toolbar
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)

        // Initialize views
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Setup RecyclerView
        applicationsAdapter = ApplicationAdapter(applications) { application ->
            showApplicationDetails(application)
        }
        applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ApplicationsActivity)
            adapter = applicationsAdapter
        }

        // Setup bottom navigation
        setupBottomNavigation()

        // Load applications
        loadApplications()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val status = when (item.itemId) {
                R.id.navigation_pending -> "pending"
                R.id.navigation_reviewed -> "reviewed"
                R.id.navigation_accepted -> "accepted"
                R.id.navigation_rejected -> "rejected"
                else -> null
            }
            loadApplications(status)
            true
        }
    }

    private fun loadApplications(status: String? = null) {
        var query = db.collection("applications")
            .whereEqualTo("companyId", companyId)

        if (status != null) {
            query = query.whereEqualTo("status", status)
        }

        query.get()
            .addOnSuccessListener { documents ->
                applications.clear()
                val newApplications = documents.map { doc ->
                    Application(
                        id = doc.id,
                        jobId = doc.getString("jobId") ?: "",
                        jobTitle = doc.getString("jobTitle") ?: "",
                        applicantName = doc.getString("applicantName") ?: "",
                        applicantEmail = doc.getString("applicantEmail") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                    )
                }.sortedByDescending { it.timestamp }

                applications.addAll(newApplications)
                applicationsAdapter.notifyDataSetChanged()

                // Update empty state
                emptyView.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
                applicationsRecyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e("ApplicationsActivity", "Error loading applications", e)
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showApplicationDetails(application: Application) {
        val intent = Intent(this, CompanyApplicationDetailsActivity::class.java).apply {
            putExtra("applicationId", application.id)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    data class Application(
        val id: String,
        val jobId: String = "",
        val jobTitle: String,
        val applicantName: String,
        val applicantEmail: String,
        val status: String,
        val timestamp: java.util.Date
    )
}