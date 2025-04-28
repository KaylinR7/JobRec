package com.example.jobrec

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserApplicationsActivity : AppCompatActivity() {
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var applicationsAdapter: UserApplicationsAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_applications)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)

        // Set up RecyclerView
        applicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        applicationsAdapter = UserApplicationsAdapter { application ->
            showApplicationDetails(application)
        }
        applicationsRecyclerView.adapter = applicationsAdapter

        // Set up tab layout
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loadApplications(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Add tabs
        ApplicationStatus.values().forEach { status ->
            tabLayout.addTab(tabLayout.newTab().setText(status.name))
        }

        // Load initial applications
        loadApplications(0)
    }

    private fun loadApplications(statusIndex: Int) {
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

    private fun showApplicationDetails(application: JobApplication) {
        // Create a dialog to show application details
        val dialog = UserApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }
} 