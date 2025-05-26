package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplicationsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var applicationsAdapter: MyApplicationsAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    private val applications = mutableListOf<ApplicationsActivity.Application>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_applications)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Explicitly set white navigation icon
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)

        db = FirebaseFirestore.getInstance()

        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupRecyclerView()
        setupBottomNavigation()
        loadApplications() // Load all applications by default
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        applicationsAdapter = MyApplicationsAdapter(applications) { application ->
            showApplicationDetails(application)
        }

        applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
            adapter = applicationsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val status = when (item.itemId) {
                R.id.navigation_all -> null
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

    private fun showApplicationDetails(application: ApplicationsActivity.Application) {
        val intent = Intent(this, StudentApplicationDetailsActivity::class.java).apply {
            putExtra("applicationId", application.id)
        }
        startActivity(intent)
    }

    private fun loadApplications(status: String? = null) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var query = db.collection("applications")
            .whereEqualTo("userId", userId)

        if (status != null) {
            query = query.whereEqualTo("status", status)
        }

        query.get()
            .addOnSuccessListener { documents ->
                applications.clear()
                val newApplications = documents.map { document ->
                    ApplicationsActivity.Application(
                        id = document.id,
                        jobId = document.getString("jobId") ?: "",
                        jobTitle = document.getString("jobTitle") ?: "",
                        applicantName = document.getString("applicantName") ?: "",
                        applicantEmail = document.getString("applicantEmail") ?: "",
                        status = document.getString("status") ?: "pending",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                    )
                }.sortedByDescending { it.timestamp }

                applications.addAll(newApplications)
                applicationsAdapter.notifyDataSetChanged()

                // Update empty state
                emptyView.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
                applicationsRecyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, "Error loading applications: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}