package com.example.jobrec

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivityMyApplicationsBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldPath

class MyApplicationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyApplicationsBinding
    private lateinit var applicationsAdapter: ApplicationsAdapter
    private val applications = mutableListOf<Application>()
    private val statuses = listOf("All", "Pending", "Shortlisted", "Rejected")
    private val TAG = "MyApplicationsActivity"
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupTabLayout()
        loadApplications(0) // Load all applications initially
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ApplicationsAdapter(applications) { application ->
            // Handle application item click
            showApplicationDetails(application)
        }
        binding.applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
            adapter = applicationsAdapter
        }
    }

    private fun setupTabLayout() {
        statuses.forEach { status ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(status))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    loadApplications(it.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadApplications(statusIndex: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "No current user found")
            Toast.makeText(this, "Please log in to view your applications", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Loading applications for user: ${currentUser.uid}")
        Log.d(TAG, "Status filter: ${if (statusIndex > 0) statuses[statusIndex] else "All"}")

        // Remove any existing listener
        listenerRegistration?.remove()

        val db = FirebaseFirestore.getInstance()
        var query = if (statusIndex > 0) {
            // When filtering by status, use the index with status first
            db.collection("applications")
                .whereEqualTo("status", statuses[statusIndex].lowercase())
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("appliedDate", Query.Direction.DESCENDING)
        } else {
            // When showing all applications, use the index with userId first
            db.collection("applications")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("appliedDate", Query.Direction.DESCENDING)
        }

        Log.d(TAG, "Setting up listener for query: ${query.toString()}")

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listener failed with error", error)
                Log.e(TAG, "Error details: ${error.message}")
                Log.e(TAG, "Error cause: ${error.cause}")
                Toast.makeText(this, "Error loading applications: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Log.d(TAG, "Received ${snapshot.documents.size} documents")
                applications.clear()
                for (document in snapshot.documents) {
                    try {
                        val application = document.toObject(Application::class.java)
                        application?.let {
                            it.id = document.id
                            Log.d(TAG, "Loaded application: id=${it.id}, userId=${it.userId}, status=${it.status}")
                            applications.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document ${document.id} to Application object", e)
                    }
                }
                applicationsAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
        binding.applicationsRecyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showApplicationDetails(application: Application) {
        val dialog = ApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 