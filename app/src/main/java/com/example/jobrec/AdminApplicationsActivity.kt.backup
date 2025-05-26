package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivityAdminApplicationsBinding
import com.example.jobrec.utils.AdminPagination
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminApplicationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminApplicationsBinding
    private lateinit var adapter: AdminApplicationsAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var pagination: AdminPagination

    private val allApplications = ArrayList<Application>()
    private val filteredApplications = ArrayList<Application>()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminApplicationsActivity"

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }

        // Set toolbar title
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Manage Applications"

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)

        // Setup RecyclerView with empty adapter initially
        setupRecyclerView()

        // Setup pagination
        pagination = AdminPagination(
            findViewById(R.id.pagination_layout),
            pageSize = 5
        ) { page ->
            updateApplicationsList()
        }

        // Setup search functionality
        setupSearch()

        // Setup FAB
        findViewById<FloatingActionButton>(R.id.addApplicationFab).setOnClickListener {
            addApplication()
        }

        // Check for search query from intent
        intent.getStringExtra("SEARCH_QUERY")?.let { query ->
            if (query.isNotEmpty()) {
                searchEditText.setText(query)
                searchApplications(query)
            } else {
                loadApplications()
            }
        } ?: loadApplications()
    }

    private fun addApplication() {
        val dialog = AdminEditApplicationDialog.newInstance(Application())
        dialog.onApplicationUpdated = {
            loadApplications()
        }
        dialog.show(supportFragmentManager, "AdminEditApplicationDialog")
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
        adapter = AdminApplicationsAdapter(filteredApplications) { application ->
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
        // Setup search text change listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // We'll handle search on button click
            }
        })

        // Setup search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isEmpty()) {
                loadApplications()
            } else {
                searchApplications(query)
            }
        }
    }

    private fun updateApplicationsList() {
        val pageItems = pagination.getPageItems(filteredApplications)

        // Update adapter with current page items
        adapter = AdminApplicationsAdapter(pageItems) { application ->
            showApplicationDetails(application)
        }
        binding.recyclerView.adapter = adapter

        // Update pagination info
        pagination.updateItemCount(filteredApplications.size)

        // Show empty state if no applications found
        updateEmptyState()
    }

    private fun loadApplications() {
        Log.d(TAG, "Loading applications...")
        binding.progressIndicator.visibility = View.VISIBLE
        searchQuery = ""

        // Clear existing applications
        allApplications.clear()
        filteredApplications.clear()

        // Log that we're querying the applications collection
        Log.d(TAG, "Querying the 'applications' collection in Firestore")

        // Query Firestore for applications without any ordering or filtering
        db.collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} applications in Firestore")

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

                        allApplications.add(application)
                        Log.d(TAG, "Added application: ${application.jobTitle} - ${application.applicantName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing document: ${e.message}", e)
                    }
                }

                // Update the filtered list
                filteredApplications.addAll(allApplications)

                // Reset pagination to first page
                pagination.resetToFirstPage()

                // Update the UI
                Log.d(TAG, "Total applications loaded: ${allApplications.size}")
                updateApplicationsList()
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
        searchQuery = query.lowercase()

        // Filter applications based on search query
        filteredApplications.clear()
        filteredApplications.addAll(allApplications.filter { application ->
            application.jobTitle.lowercase().contains(searchQuery) ||
            application.applicantName.lowercase().contains(searchQuery) ||
            application.companyName.lowercase().contains(searchQuery) ||
            application.status.lowercase().contains(searchQuery) ||
            application.applicantEmail.lowercase().contains(searchQuery)
        })

        // Reset pagination to first page
        pagination.resetToFirstPage()

        // Update the UI
        Log.d(TAG, "Filtered to ${filteredApplications.size} applications")
        updateApplicationsList()
        binding.progressIndicator.visibility = View.GONE
    }

    private fun updateEmptyState() {
        val isEmpty = filteredApplications.isEmpty()
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