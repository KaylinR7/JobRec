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
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Manage Applications"
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        setupRecyclerView()
        pagination = AdminPagination(
            findViewById(R.id.pagination_layout),
            pageSize = 5
        ) { page ->
            updateApplicationsList()
        }
        setupSearch()
        findViewById<FloatingActionButton>(R.id.addApplicationFab).setOnClickListener {
            addApplication()
        }
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
        Log.d(TAG, "Test applications disabled")
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        adapter = AdminApplicationsAdapter(filteredApplications) { application ->
            showApplicationDetails(application)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminApplicationsActivity)
            adapter = this@AdminApplicationsActivity.adapter
        }
        Log.d(TAG, "RecyclerView setup complete")
    }
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
            }
        })
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
        adapter = AdminApplicationsAdapter(pageItems) { application ->
            showApplicationDetails(application)
        }
        binding.recyclerView.adapter = adapter
        pagination.updateItemCount(filteredApplications.size)
        updateEmptyState()
    }
    private fun loadApplications() {
        Log.d(TAG, "Loading applications...")
        binding.progressIndicator.visibility = View.VISIBLE
        searchQuery = ""
        allApplications.clear()
        filteredApplications.clear()
        Log.d(TAG, "Querying the 'applications' collection in Firestore")
        db.collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} applications in Firestore")
                for (document in documents) {
                    try {
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
                            appliedDate = document.getTimestamp("appliedDate") ?: com.google.firebase.Timestamp.now()
                        )
                        allApplications.add(application)
                        Log.d(TAG, "Added application: ${application.jobTitle} - ${application.applicantName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing document: ${e.message}", e)
                    }
                }
                filteredApplications.addAll(allApplications)
                pagination.resetToFirstPage()
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
        filteredApplications.clear()
        filteredApplications.addAll(allApplications.filter { application ->
            application.jobTitle.lowercase().contains(searchQuery) ||
            application.applicantName.lowercase().contains(searchQuery) ||
            application.companyName.lowercase().contains(searchQuery) ||
            application.status.lowercase().contains(searchQuery) ||
            application.applicantEmail.lowercase().contains(searchQuery)
        })
        pagination.resetToFirstPage()
        Log.d(TAG, "Filtered to ${filteredApplications.size} applications")
        updateApplicationsList()
        binding.progressIndicator.visibility = View.GONE
    }
    private fun updateEmptyState() {
        val isEmpty = filteredApplications.isEmpty()
        Log.d(TAG, "Updating empty state. Applications list is empty: $isEmpty")
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        Log.d(TAG, "EmptyStateLayout visibility: ${binding.emptyStateLayout.visibility == View.VISIBLE}")
        Log.d(TAG, "RecyclerView visibility: ${binding.recyclerView.visibility == View.VISIBLE}")
    }
    private fun showApplicationDetails(application: Application) {
        Log.d(TAG, "Showing details for application: ${application.jobTitle}")
        val dialog = AdminEditApplicationDialog.newInstance(application)
        dialog.onApplicationUpdated = {
            loadApplications()
        }
        dialog.onApplicationDeleted = {
            loadApplications()
            Toast.makeText(this, "Application deleted successfully", Toast.LENGTH_SHORT).show()
        }
        dialog.show(supportFragmentManager, "EditApplication")
    }
    private fun getStatusFromDocument(document: com.google.firebase.firestore.DocumentSnapshot): String {
        val possibleFieldNames = listOf(
            "status", "applicationStatus", "app_status", "Status", "APPLICATION_STATUS",
            "application_status", "appStatus", "state", "applicationState"
        )
        for (fieldName in possibleFieldNames) {
            val status = document.getString(fieldName)
            if (!status.isNullOrEmpty()) {
                Log.d(TAG, "Found status in field '$fieldName': $status")
                return status
            }
        }
        document.data?.forEach { (key, value) ->
            if (key.lowercase().contains("status") && value is String) {
                Log.d(TAG, "Found status-like field '$key': $value")
                return value
            }
        }
        Log.d(TAG, "No status field found, using default 'pending'")
        return "pending"
    }
}