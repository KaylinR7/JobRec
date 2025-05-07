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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.content.Intent

class ApplicationsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var statusChipGroup: ChipGroup
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

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Applications"

        // Initialize views
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        statusChipGroup = findViewById(R.id.statusChipGroup)

        // Setup RecyclerView
        applicationsAdapter = ApplicationAdapter(applications) { application ->
            showApplicationDetails(application)
        }
        applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ApplicationsActivity)
            adapter = applicationsAdapter
        }

        // Setup status filter
        setupStatusFilter()

        // Load applications
        loadApplications()
    }

    private fun setupStatusFilter() {
        statusChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            val status = when (chip?.id) {
                R.id.pendingChip -> "pending"
                R.id.reviewedChip -> "reviewed"
                R.id.acceptedChip -> "accepted"
                R.id.rejectedChip -> "rejected"
                else -> null
            }
            loadApplications(status)
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

    data class Application(
        val id: String,
        val jobTitle: String,
        val applicantName: String,
        val applicantEmail: String,
        val status: String,
        val timestamp: java.util.Date
    )
} 