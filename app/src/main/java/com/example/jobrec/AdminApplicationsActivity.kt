package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivityAdminApplicationsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminApplicationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminApplicationsBinding
    private lateinit var adapter: AdminApplicationsAdapter
    private val applications = ArrayList<Application>()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminApplicationsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Manage Applications"
        }

        // Setup RecyclerView with empty adapter initially
        setupRecyclerView()

        // Setup search functionality
        setupSearch()

        // Load applications data
        loadApplications()

        // Add a test application to verify the adapter is working
        addTestApplication()
    }

    private fun addTestApplication() {
        // This method is now empty - we no longer add test applications
        Log.d(TAG, "Test applications disabled")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")

        // Create adapter with the applications list
        adapter = AdminApplicationsAdapter(applications) { application ->
            showApplicationDetails(application)
        }

        // Set up RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminApplicationsActivity)
            adapter = this@AdminApplicationsActivity.adapter
        }

        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadApplications()
                } else {
                    searchApplications(query)
                }
            }
        })
    }

    private fun loadApplications() {
        Log.d(TAG, "Loading applications...")
        binding.progressIndicator.visibility = View.VISIBLE

        // Clear existing applications
        applications.clear()

        // Always add the test application first to ensure something is displayed
        addTestApplication()

        // Log that we're querying the applications collection
        Log.d(TAG, "Querying the 'applications' collection in Firestore")

        // Query Firestore for applications without any ordering or filtering
        db.collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} applications in Firestore")

                // Log all document IDs and fields for debugging
                documents.forEach { doc ->
                    Log.d(TAG, "Document ID: ${doc.id}")
                    Log.d(TAG, "All fields: ${doc.data}")
                    // Log every field in the document to find the status field
                    doc.data.forEach { (key, value) ->
                        Log.d(TAG, "Field: $key = $value")
                    }
                }

                // Process each document
                for (document in documents) {
                    try {
                        // Create application manually from document fields to avoid deserialization issues
                        // Get the status and log it
                        val status = getStatusFromDocument(document)
                        Log.d(TAG, "Final application status: $status")

                        val application = Application(
                            id = document.id,
                            jobId = document.getString("jobId") ?: "",
                            jobTitle = document.getString("jobTitle") ?: "Unknown Job",
                            companyName = document.getString("companyName") ?: "Unknown Company",
                            userId = document.getString("userId") ?: "",
                            applicantName = document.getString("applicantName") ?: "Unknown Applicant",
                            applicantEmail = document.getString("applicantEmail") ?: "",
                            applicantPhone = document.getString("applicantPhone") ?: "",
                            status = status,
                            // Use the current timestamp if appliedDate is missing
                            appliedDate = document.getTimestamp("appliedDate") ?: com.google.firebase.Timestamp.now()
                        )

                        applications.add(application)
                        Log.d(TAG, "Added application: ${application.jobTitle} - ${application.applicantName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing document: ${e.message}", e)
                    }
                }

                // Update the UI
                Log.d(TAG, "Total applications loaded: ${applications.size}")
                adapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading applications: ${e.message}", e)
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressIndicator.visibility = View.GONE
            }
    }

    private fun searchApplications(query: String) {
        Log.d(TAG, "Searching applications with query: $query")
        binding.progressIndicator.visibility = View.VISIBLE

        // Clear existing applications
        applications.clear()

        // Always add the test application to ensure something is displayed
        addTestApplication()

        // Log that we're searching the applications collection
        Log.d(TAG, "Searching the 'applications' collection in Firestore with query: $query")

        // Query Firestore for all applications without ordering
        db.collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} applications to filter")

                // Process and filter each document
                for (document in documents) {
                    try {
                        // Create application manually from document fields
                        // Get the status and log it
                        val status = getStatusFromDocument(document)
                        Log.d(TAG, "Final application status for search: $status")

                        val application = Application(
                            id = document.id,
                            jobId = document.getString("jobId") ?: "",
                            jobTitle = document.getString("jobTitle") ?: "Unknown Job",
                            companyName = document.getString("companyName") ?: "Unknown Company",
                            userId = document.getString("userId") ?: "",
                            applicantName = document.getString("applicantName") ?: "Unknown Applicant",
                            applicantEmail = document.getString("applicantEmail") ?: "",
                            applicantPhone = document.getString("applicantPhone") ?: "",
                            status = status,
                            // Use the current timestamp if appliedDate is missing
                            appliedDate = document.getTimestamp("appliedDate") ?: com.google.firebase.Timestamp.now()
                        )

                        // Filter by query
                        if (application.jobTitle.contains(query, ignoreCase = true) ||
                            application.applicantName.contains(query, ignoreCase = true) ||
                            application.status.contains(query, ignoreCase = true)) {
                            applications.add(application)
                            Log.d(TAG, "Matched application: ${application.jobTitle}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing document: ${e.message}", e)
                    }
                }

                // Update the UI
                Log.d(TAG, "Filtered to ${applications.size} applications")
                adapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching applications: ${e.message}", e)
                Toast.makeText(this, "Error searching applications: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressIndicator.visibility = View.GONE
            }
    }

    private fun updateEmptyState() {
        val isEmpty = applications.isEmpty()
        Log.d(TAG, "Updating empty state. Applications list is empty: $isEmpty")

        // Update visibility of empty state and recycler view
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

        // Log visibility states for debugging
        Log.d(TAG, "EmptyStateLayout visibility: ${binding.emptyStateLayout.visibility == View.VISIBLE}")
        Log.d(TAG, "RecyclerView visibility: ${binding.recyclerView.visibility == View.VISIBLE}")
    }

    private fun showApplicationDetails(application: Application) {
        Log.d(TAG, "Showing details for application: ${application.jobTitle}")

        // Use the new edit dialog instead of the details dialog
        val dialog = AdminEditApplicationDialog.newInstance(application)

        // Set callbacks for when application is updated or deleted
        dialog.onApplicationUpdated = {
            // Reload applications when an application is updated
            loadApplications()
        }

        dialog.onApplicationDeleted = {
            // Reload applications when an application is deleted
            loadApplications()
            Toast.makeText(this, "Application deleted successfully", Toast.LENGTH_SHORT).show()
        }

        dialog.show(supportFragmentManager, "EditApplication")
    }

    /**
     * Helper method to get the status from a document, checking multiple possible field names
     */
    private fun getStatusFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): String {
        // Try different possible field names for status
        val possibleFieldNames = listOf(
            "status", "applicationStatus", "app_status", "Status", "APPLICATION_STATUS",
            "application_status", "appStatus", "state", "applicationState"
        )

        // First try direct field access
        for (fieldName in possibleFieldNames) {
            val status = document.getString(fieldName)
            if (!status.isNullOrEmpty()) {
                Log.d(TAG, "Found status in field '$fieldName': $status")
                return status
            }
        }

        // If direct access fails, try checking all fields for any that might contain "status" in the name
        document.data?.forEach { (key, value) ->
            if (key.lowercase().contains("status") && value is String) {
                Log.d(TAG, "Found status-like field '$key': $value")
                return value
            }
        }

        // If no status field found, return default
        Log.d(TAG, "No status field found, using default 'pending'")
        return "pending"
    }
}