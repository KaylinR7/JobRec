package com.example.jobrec

import android.os.Bundle
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

        // Initialize views
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)

        // Setup RecyclerView
        applicationsAdapter = ApplicationsAdapter { application ->
            // Handle application click
            // TODO: Show application details
        }
        applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
            adapter = applicationsAdapter
        }

        // Setup tabs
        setupTabs()
    }

    private fun setupTabs() {
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
    }

    private fun loadApplications(statusIndex: Int) {
        val userId = auth.currentUser?.uid ?: return
        val status = ApplicationStatus.values()[statusIndex]
        
        db.collection("applications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", status.name)
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.mapNotNull { document ->
                    try {
                        document.toObject(JobApplication::class.java).copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                applicationsAdapter.updateApplications(applications)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 