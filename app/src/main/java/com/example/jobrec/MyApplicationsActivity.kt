package com.example.jobrec

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyApplicationsActivity : AppCompatActivity() {
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var applicationsAdapter: ApplicationsAdapter
    private lateinit var tabLayout: TabLayout
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_applications)

        try {
            // Initialize views
            applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
            tabLayout = findViewById(R.id.tabLayout)

            // Setup RecyclerView
            applicationsAdapter = ApplicationsAdapter { application ->
                // Handle application click
                showApplicationDetails(application)
            }
            applicationsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
                adapter = applicationsAdapter
            }

            // Setup tabs
            setupTabs()
        } catch (e: Exception) {
            Log.e("MyApplicationsActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing activity: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupTabs() {
        try {
            // Add tabs for different application statuses
            ApplicationStatus.values().forEach { status ->
                tabLayout.addTab(tabLayout.newTab().setText(status.name))
            }

            // Load applications for the first tab by default
            loadApplications(0)

            // Handle tab selection
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    loadApplications(tab.position)
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        } catch (e: Exception) {
            Log.e("MyApplicationsActivity", "Error in setupTabs", e)
            Toast.makeText(this, "Error setting up tabs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadApplications(statusIndex: Int) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("MyApplicationsActivity", "User not logged in")
                Toast.makeText(this, "Please log in to view applications", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val status = ApplicationStatus.values().getOrNull(statusIndex)
            if (status == null) {
                Log.e("MyApplicationsActivity", "Invalid status index: $statusIndex")
                return
            }
            
            Log.d("MyApplicationsActivity", "Loading applications for user: $userId, status: ${status.name}")
            
            db.collection("applications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", status.name)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        Log.d("MyApplicationsActivity", "Query succeeded with ${documents.size()} documents")
                        val applications = documents.mapNotNull { document ->
                            try {
                                val application = document.toObject(JobApplication::class.java).copy(id = document.id)
                                Log.d("MyApplicationsActivity", "Successfully parsed application: ${application.jobTitle}")
                                application
                            } catch (e: Exception) {
                                Log.e("MyApplicationsActivity", "Error parsing application document", e)
                                null
                            }
                        }
                        
                        // Sort applications in memory by appliedDate
                        val sortedApplications = applications.sortedByDescending { it.appliedDate }
                        Log.d("MyApplicationsActivity", "Processed ${sortedApplications.size} applications")
                        
                        runOnUiThread {
                            applicationsAdapter.updateApplications(sortedApplications)
                        }
                    } catch (e: Exception) {
                        Log.e("MyApplicationsActivity", "Error processing applications", e)
                        runOnUiThread {
                            Toast.makeText(this, "Error processing applications: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyApplicationsActivity", "Error loading applications", e)
                    runOnUiThread {
                        Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Log.e("MyApplicationsActivity", "Error in loadApplications", e)
            Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showApplicationDetails(application: JobApplication) {
        try {
            // TODO: Implement application details dialog
            Toast.makeText(this, "Showing details for: ${application.jobTitle}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MyApplicationsActivity", "Error showing application details", e)
            Toast.makeText(this, "Error showing application details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 