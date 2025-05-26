package com.example.jobrec
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.utils.AdminPagination
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
class AdminJobsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminJobsAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyStateView: LinearLayout
    private lateinit var pagination: AdminPagination
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminJobsActivity"
    private var allJobs: MutableList<Job> = mutableListOf()
    private var filteredJobs: MutableList<Job> = mutableListOf()
    private var searchQuery: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_jobs)
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Manage Jobs"
        recyclerView = findViewById(R.id.recyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        progressIndicator = findViewById(R.id.progressIndicator)
        emptyStateView = findViewById(R.id.emptyStateView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminJobsAdapter(
            emptyList(),
            onViewClick = { job -> viewJob(job) },
            onEditClick = { job -> editJob(job) },
            onDeleteClick = { job -> deleteJob(job.id) }
        )
        recyclerView.adapter = adapter
        pagination = AdminPagination(
            findViewById(R.id.pagination_layout),
            pageSize = 5
        ) { page ->
            updateJobsList()
        }
        findViewById<FloatingActionButton>(R.id.addJobFab).setOnClickListener {
            addJob()
        }
        setupSearch()
        intent.getStringExtra("SEARCH_QUERY")?.let { query ->
            if (query.isNotEmpty()) {
                searchEditText.setText(query)
                searchJobs(query)
            } else {
                loadJobs()
            }
        } ?: loadJobs()
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
                loadJobs()
            } else {
                searchJobs(query)
            }
        }
    }
    private fun loadJobs() {
        Log.d(TAG, "Loading jobs from Firestore")
        showLoading(true)
        searchQuery = ""
        allJobs.clear()
        filteredJobs.clear()
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} jobs")
                allJobs = documents.mapNotNull { document ->
                    try {
                        document.toObject(Job::class.java).copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Job: ${e.message}")
                        null
                    }
                }.toMutableList()
                filteredJobs.addAll(allJobs)
                updateJobsList()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading jobs", exception)
                Toast.makeText(this, "Error loading jobs: ${exception.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
                showEmptyState(true)
            }
    }
    private fun searchJobs(query: String) {
        Log.d(TAG, "Searching jobs with query: $query")
        showLoading(true)
        searchQuery = query.lowercase()
        filteredJobs.clear()
        filteredJobs.addAll(allJobs.filter { job ->
            job.title.lowercase().contains(searchQuery) ||
            job.description.lowercase().contains(searchQuery) ||
            job.companyName.lowercase().contains(searchQuery) ||
            job.location.lowercase().contains(searchQuery) ||
            job.jobField.lowercase().contains(searchQuery)
        })
        pagination.resetToFirstPage()
        updateJobsList()
        showLoading(false)
    }
    private fun updateJobsList() {
        val pageItems = pagination.getPageItems(filteredJobs)
        adapter.updateJobs(pageItems)
        pagination.updateItemCount(filteredJobs.size)
        showEmptyState(filteredJobs.isEmpty())
    }
    private fun showLoading(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    private fun showEmptyState(show: Boolean) {
        emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    private fun deleteJob(jobId: String) {
        Log.d(TAG, "Attempting to delete job with ID: $jobId")
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteJob(jobId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun performDeleteJob(jobId: String) {
        showLoading(true)
        db.collection("jobs").document(jobId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Job deleted successfully")
                Toast.makeText(this, "Job deleted", Toast.LENGTH_SHORT).show()
                loadJobs()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting job", e)
                Toast.makeText(this, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    private fun addJob() {
        val dialog = AdminEditJobDialog.newInstance(Job())
        dialog.onJobUpdated = {
            loadJobs()
        }
        dialog.show(supportFragmentManager, "AdminEditJobDialog")
    }
    private fun viewJob(job: Job) {
        val dialog = AdminJobDetailsDialog.newInstance(job)
        dialog.show(supportFragmentManager, "JobDetails")
    }
    private fun editJob(job: Job) {
        val dialog = AdminEditJobDialog.newInstance(job)
        dialog.onJobUpdated = {
            loadJobs()
        }
        dialog.onJobDeleted = {
            loadJobs()
        }
        dialog.show(supportFragmentManager, "AdminEditJobDialog")
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}