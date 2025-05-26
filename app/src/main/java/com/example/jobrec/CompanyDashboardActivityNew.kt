package com.example.jobrec

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jobrec.adapters.RecentActivityAdapter
import com.example.jobrec.adapters.RecentApplicationsAdapter
import com.example.jobrec.chatbot.ChatbotHelper
import com.example.jobrec.models.Application
import com.example.jobrec.models.RecentActivity
import com.example.jobrec.Job
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import android.view.animation.AnimationUtils
import com.example.jobrec.utils.SpacingItemDecoration
import java.util.UUID

class CompanyDashboardActivityNew : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var activeJobsCount: TextView
    private lateinit var applicationsCount: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recentActivityRecyclerView: RecyclerView
    private lateinit var recentApplicationsRecyclerView: RecyclerView
    private lateinit var emptyActivityView: TextView
    private lateinit var emptyApplicationsView: TextView
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private lateinit var recentApplicationsAdapter: RecentApplicationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_dashboard_new)

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            // Get company ID from intent or find it using the current user's email
            companyId = intent.getStringExtra("companyId") ?: run {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Query companies collection to find the company with matching email (case-insensitive)
                    val userEmail = currentUser.email?.lowercase() ?: ""
                    Log.d("CompanyDashboard", "Looking for company with email (lowercase): $userEmail")

                    // Get all companies and filter by email case-insensitively
                    db.collection("companies")
                        .get()
                        .addOnSuccessListener { documents ->
                            // Find company with matching email (case-insensitive)
                            val companyDoc = documents.find { doc ->
                                doc.getString("email")?.lowercase() == userEmail
                            }

                            if (companyDoc != null) {
                                // Use registration number as company ID for consistency
                                val registrationNumber = companyDoc.getString("registrationNumber")
                                if (registrationNumber != null) {
                                    companyId = registrationNumber
                                    Log.d("CompanyDashboard", "Found company with registration number: $companyId")
                                    // Initialize views and load data after getting company ID
                                    initializeViews()
                                    setupBottomNavigation()
                                    setupQuickActions()
                                    setupSwipeRefresh()
                                    loadDashboardData()
                                } else {
                                    Log.e("CompanyDashboard", "Registration number not found in company document")
                                    Toast.makeText(this, "Error: Company data incomplete", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            } else {
                                Log.e("CompanyDashboard", "No company found for email: ${currentUser.email}")
                                Toast.makeText(this, "Error: Company not found", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error finding company: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    return@run ""
                } else {
                    Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@run ""
                }
            }

            // Initialize views
            initializeViews()

            // Setup toolbar without back button (dashboard is the main screen)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowHomeEnabled(false)
                title = "Company Dashboard"
            }

            // Setup bottom navigation
            setupBottomNavigation()

            // Setup quick action buttons
            setupQuickActions()

            // Setup swipe refresh
            setupSwipeRefresh()

            // Load dashboard data
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("CompanyDashboardActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing activity: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        activeJobsCount = findViewById(R.id.activeJobsCount)
        applicationsCount = findViewById(R.id.applicationsCount)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView)
        recentApplicationsRecyclerView = findViewById(R.id.recentApplicationsRecyclerView)
        emptyActivityView = findViewById(R.id.emptyActivityView)
        emptyApplicationsView = findViewById(R.id.emptyApplicationsView)

        // Setup RecyclerViews
        setupRecyclerViews()

        // Chatbot now in bottom navigation
    }

    private fun setupRecyclerViews() {
        // Setup Recent Activity RecyclerView
        recentActivityAdapter = RecentActivityAdapter { activity ->
            handleActivityClick(activity)
        }
        recentActivityRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyDashboardActivityNew, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentActivityAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }

        // Setup Recent Applications RecyclerView
        recentApplicationsAdapter = RecentApplicationsAdapter { application ->
            handleApplicationClick(application)
        }
        recentApplicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyDashboardActivityNew)
            adapter = recentApplicationsAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }
    }

    private fun handleActivityClick(activity: RecentActivity) {
        when (activity.type) {
            "application" -> {
                val intent = Intent(this, CompanyApplicationDetailsActivity::class.java)
                intent.putExtra("applicationId", activity.relatedId)
                startActivity(intent)
            }
            "job_post" -> {
                val intent = Intent(this, JobDetailsActivity::class.java)
                intent.putExtra("jobId", activity.relatedId)
                startActivity(intent)
            }
            "message" -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("conversationId", activity.relatedId)
                startActivity(intent)
            }
        }
    }

    private fun handleApplicationClick(application: Application) {
        val intent = Intent(this, CompanyApplicationDetailsActivity::class.java)
        intent.putExtra("applicationId", application.id)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.company_dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, CandidateSearchActivity::class.java))
                true
            }
            R.id.action_messages -> {
                startActivity(Intent(this, ConversationsActivity::class.java))
                true
            }

            R.id.action_logout -> {
                // Clear all user session data when logging out
                val sharedPreferences = getSharedPreferences("JobRecPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("override_to_student", false)
                    .remove("user_type")
                    .remove("user_id")
                    .apply()

                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_jobs -> {
                    val intent = Intent(this, EmployerJobsActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    true
                }
                R.id.nav_applications -> {
                    val intent = Intent(this, ApplicationsActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, CompanyProfileActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    true
                }
                R.id.nav_ai_assistant -> {
                    val intent = Intent(this, com.example.jobrec.chatbot.ChatbotActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupQuickActions() {
        findViewById<View>(R.id.btnPostJob).setOnClickListener {
            val intent = Intent(this, PostJobActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnViewApplications).setOnClickListener {
            val intent = Intent(this, ApplicationsActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSearchCandidates).setOnClickListener {
            val intent = Intent(this, CandidateSearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnMessages).setOnClickListener {
            val intent = Intent(this, ConversationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.accent),
            ContextCompat.getColor(this, R.color.primary_dark)
        )
        swipeRefreshLayout.setOnRefreshListener {
            loadDashboardData()
        }
    }

    private fun loadDashboardData() {
        Log.d("CompanyDashboard", "Loading dashboard data for company: $companyId")

        // Show loading indicators
        swipeRefreshLayout.isRefreshing = true

        // Show empty state views with loading messages
        emptyActivityView.text = "Loading activities..."
        emptyActivityView.visibility = View.VISIBLE
        emptyApplicationsView.text = "Loading applications..."
        emptyApplicationsView.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load stats first (quick operation)
                loadStats()

                // Load data in parallel for better performance
                val activitiesJob = lifecycleScope.launch { loadRecentActivities() }
                val applicationsJob = lifecycleScope.launch { loadRecentApplications() }

                // Wait for all data to load
                activitiesJob.join()
                applicationsJob.join()

                // Hide refresh indicator
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false

                    // Apply animations
                    applyAnimations()
                }
            } catch (e: Exception) {
                Log.e("CompanyDashboard", "Error loading dashboard data", e)

                runOnUiThread {
                    // Hide refresh indicator
                    swipeRefreshLayout.isRefreshing = false

                    // Show error message
                    Toast.makeText(
                        this@CompanyDashboardActivityNew,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Update empty state views with error messages
                    if (recentActivityRecyclerView.visibility != View.VISIBLE) {
                        emptyActivityView.text = "Could not load activities"
                        emptyActivityView.visibility = View.VISIBLE
                    }

                    if (recentApplicationsRecyclerView.visibility != View.VISIBLE) {
                        emptyApplicationsView.text = "Could not load applications"
                        emptyApplicationsView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private suspend fun loadStats() {
        try {
            // Load active jobs count
            val jobsSnapshot = db.collection("jobs")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()

            val activeJobsCount = jobsSnapshot.count { doc ->
                doc.getString("status") == "active"
            }
            this.activeJobsCount.text = activeJobsCount.toString()

            // Load applications count
            val applicationsSnapshot = db.collection("applications")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()

            this.applicationsCount.text = applicationsSnapshot.size().toString()
        } catch (e: Exception) {
            Log.e("CompanyDashboard", "Error loading stats", e)
        }
    }

    private suspend fun loadRecentActivities() {
        try {
            Log.d("CompanyDashboard", "Loading recent activities for company: $companyId")
            val activities = mutableListOf<RecentActivity>()

            // Get recent applications with enhanced details - using simpler query
            val applicationsSnapshot = db.collection("applications")
                .whereEqualTo("companyId", companyId)
                .limit(20) // Increased limit for more data since we'll sort in memory
                .get()
                .await()

            Log.d("CompanyDashboard", "Found ${applicationsSnapshot.size()} recent applications")

            for (doc in applicationsSnapshot.documents) {
                try {
                    val application = doc.toObject(Application::class.java)?.copy(id = doc.id) ?: continue

                    // Get candidate name if not already set
                    var candidateName = application.candidateName ?: ""
                    if (candidateName.isBlank()) {
                        try {
                            // Check if candidateId is valid before using it
                            if (application.candidateId.isNotBlank() && application.candidateId != "users") {
                                val candidateDoc = db.collection("users")
                                    .document(application.candidateId)
                                    .get()
                                    .await()

                                if (candidateDoc.exists()) {
                                    candidateName = candidateDoc.getString("name") ?: "Unknown Candidate"
                                    application.candidateName = candidateName
                                }
                            } else {
                                // Log the invalid candidateId for debugging
                                Log.e("CompanyDashboard", "Error processing application document (Invalid candidateId): '${application.candidateId}'")
                                candidateName = "Unknown Candidate"
                                application.candidateName = candidateName
                            }
                        } catch (e: Exception) {
                            Log.e("CompanyDashboard", "Error processing application document (Ask Gemini)", e)
                            candidateName = "Unknown Candidate"
                            application.candidateName = candidateName
                        }
                    }

                    // Get job title if not already set
                    var jobTitle = application.jobTitle ?: ""
                    if (jobTitle.isBlank()) {
                        val jobDoc = db.collection("jobs")
                            .document(application.jobId)
                            .get()
                            .await()

                        if (jobDoc.exists()) {
                            jobTitle = jobDoc.getString("title") ?: "Unknown Job"
                            application.jobTitle = jobTitle
                        }
                    }

                    // Create activity with enhanced description
                    val description = when (application.status.lowercase()) {
                        "applied" -> "New application from $candidateName for $jobTitle"
                        "reviewed" -> "Application from $candidateName for $jobTitle was reviewed"
                        "shortlisted" -> "Application from $candidateName for $jobTitle was shortlisted"
                        "interviewed" -> "Interview completed with $candidateName for $jobTitle"
                        "offered" -> "Job offer sent to $candidateName for $jobTitle"
                        "rejected" -> "Application from $candidateName for $jobTitle was rejected"
                        else -> "Received application for $jobTitle from $candidateName"
                    }

                    activities.add(
                        RecentActivity(
                            id = UUID.randomUUID().toString(),
                            companyId = companyId,
                            type = "application",
                            title = getActivityTitle(application.status),
                            description = description,
                            relatedId = application.id,
                            relatedName = jobTitle,
                            timestamp = try {
                                application.timestamp
                            } catch (e: Exception) {
                                try {
                                    application.appliedAt ?: Timestamp.now()
                                } catch (e2: Exception) {
                                    Timestamp.now() // Fallback if neither field exists
                                }
                            }
                        )
                    )
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error processing application document", e)
                }
            }

            // Get recent job posts - using simpler query
            val jobsSnapshot = db.collection("jobs")
                .whereEqualTo("companyId", companyId)
                .limit(10) // Increased limit for more data since we'll sort in memory
                .get()
                .await()

            Log.d("CompanyDashboard", "Found ${jobsSnapshot.size()} recent job posts")

            for (doc in jobsSnapshot.documents) {
                try {
                    val job = doc.toObject(Job::class.java)?.copy(id = doc.id) ?: continue

                    // Create activity with enhanced description
                    val description = when (job.status.lowercase()) {
                        "active" -> "You posted a new job: ${job.title} (Active)"
                        "inactive" -> "Job posting ended: ${job.title}"
                        "draft" -> "Job draft created: ${job.title}"
                        else -> "You posted a new job: ${job.title}"
                    }

                    activities.add(
                        RecentActivity(
                            id = UUID.randomUUID().toString(),
                            companyId = companyId,
                            type = "job_post",
                            title = "Job Posted",
                            description = description,
                            relatedId = job.id,
                            relatedName = job.title,
                            timestamp = job.postedDate
                        )
                    )
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error processing job document", e)
                }
            }

            // Get recent messages/conversations
            try {
                val conversationsSnapshot = db.collection("conversations")
                    .whereEqualTo("companyId", companyId)
                    .limit(5) // Increased limit for more data since we'll sort in memory
                    .get()
                    .await()

                Log.d("CompanyDashboard", "Found ${conversationsSnapshot.size()} recent conversations")

                for (doc in conversationsSnapshot.documents) {
                    val conversation = doc.data ?: continue
                    val candidateName = conversation["candidateName"] as? String ?: "a candidate"
                    val jobTitle = conversation["jobTitle"] as? String ?: "a job"
                    val lastMessageTime = conversation["lastMessageTime"] as? com.google.firebase.Timestamp ?: Timestamp.now()

                    activities.add(
                        RecentActivity(
                            id = UUID.randomUUID().toString(),
                            companyId = companyId,
                            type = "message",
                            title = "New Message",
                            description = "New message from $candidateName regarding $jobTitle",
                            relatedId = doc.id,
                            relatedName = candidateName,
                            timestamp = lastMessageTime
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("CompanyDashboard", "Error loading conversations", e)
            }

            // Sort activities by timestamp (most recent first)
            // Since we're using simpler queries without ordering, this in-memory sort is crucial
            val sortedActivities = activities.sortedByDescending {
                // RecentActivity class should always have a timestamp field
                it.timestamp.seconds
            }

            Log.d("CompanyDashboard", "Total activities after sorting: ${sortedActivities.size}")

            // Update UI
            runOnUiThread {
                if (sortedActivities.isNotEmpty()) {
                    recentActivityAdapter.submitList(sortedActivities)
                    recentActivityRecyclerView.visibility = View.VISIBLE
                    emptyActivityView.visibility = View.GONE
                } else {
                    recentActivityRecyclerView.visibility = View.GONE
                    emptyActivityView.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e("CompanyDashboard", "Error loading recent activities", e)
            runOnUiThread {
                recentActivityRecyclerView.visibility = View.GONE
                emptyActivityView.visibility = View.VISIBLE
            }
        }
    }

    private fun getActivityTitle(status: String): String {
        return when (status.lowercase()) {
            "applied" -> "New Application"
            "reviewed" -> "Application Reviewed"
            "shortlisted" -> "Candidate Shortlisted"
            "interviewed" -> "Interview Completed"
            "offered" -> "Job Offered"
            "rejected" -> "Application Rejected"
            else -> "Application Update"
        }
    }

    private suspend fun loadRecentApplications() {
        try {
            Log.d("CompanyDashboard", "Loading recent applications for company: $companyId")

            // Get recent applications with candidate details - using simpler query
            // Limiting to just 3 applications since this is just a preview (there's a separate tab for all applications)
            val applicationsSnapshot = db.collection("applications")
                .whereEqualTo("companyId", companyId)
                .limit(3) // Reduced limit to just show a preview
                .get()
                .await()

            Log.d("CompanyDashboard", "Found ${applicationsSnapshot.size()} recent applications")

            val applications = mutableListOf<Application>()

            for (doc in applicationsSnapshot.documents) {
                try {
                    // Get the application data
                    val application = doc.toObject(Application::class.java)?.copy(id = doc.id) ?: continue

                    // Get candidate name and details if not already set
                    if (application.candidateName.isNullOrEmpty()) {
                        try {
                            // First try to get from applicantName field (from the main Application class)
                            try {
                                val appObj = doc.toObject(com.example.jobrec.Application::class.java)
                                if (!appObj?.applicantName.isNullOrEmpty()) {
                                    application.candidateName = appObj?.applicantName
                                    Log.d("CompanyDashboard", "Using applicantName: ${application.candidateName}")
                                }
                            } catch (e: Exception) {
                                Log.d("CompanyDashboard", "Could not get applicantName, trying user document")
                            }

                            // If still empty, try to get from users collection
                            if (application.candidateName.isNullOrEmpty()) {
                                val userId = application.candidateId.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("userId") // Try to get userId as fallback

                                if (!userId.isNullOrEmpty() && userId != "users") {
                                    try {
                                        val candidateDoc = db.collection("users")
                                            .document(userId)
                                            .get()
                                            .await()

                                        if (candidateDoc.exists()) {
                                            val candidateName = candidateDoc.getString("name") ?: "Unknown Candidate"
                                            application.candidateName = candidateName
                                            Log.d("CompanyDashboard", "Got name from users collection: ${application.candidateName}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CompanyDashboard", "Error getting user document for userId: $userId", e)
                                        application.candidateName = "Unknown Candidate"
                                    }
                                } else {
                                    Log.e("CompanyDashboard", "Invalid userId: '$userId'")
                                    application.candidateName = "Unknown Candidate"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CompanyDashboard", "Error getting candidate name for application ${application.id}", e)
                            application.candidateName = "Unknown Candidate"
                        }
                    }

                    // Get job title and details if not already set
                    if (application.jobTitle.isNullOrEmpty()) {
                        try {
                            val jobDoc = db.collection("jobs")
                                .document(application.jobId)
                                .get()
                                .await()

                            if (jobDoc.exists()) {
                                val jobTitle = jobDoc.getString("title") ?: "Unknown Job"
                                application.jobTitle = jobTitle
                            }
                        } catch (e: Exception) {
                            Log.e("CompanyDashboard", "Error getting job title for application ${application.id}", e)
                            application.jobTitle = "Unknown Job"
                        }
                    }

                    // Add the application to our list
                    applications.add(application)

                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error processing application document", e)
                }
            }

            // Sort applications by applied date (most recent first)
            // Since we're using simpler queries without ordering, this in-memory sort is crucial
            val sortedApplications = applications.sortedByDescending {
                // Try multiple field names for the application date
                try {
                    // First try to get the applieddate field (lowercase, as used in Firestore)
                    try {
                        // Try to access the field using reflection
                        val appliedDateField = it.javaClass.getDeclaredField("applieddate")
                        appliedDateField.isAccessible = true
                        val appliedDateValue = appliedDateField.get(it) as? Timestamp
                        appliedDateValue?.seconds ?: 0
                    } catch (e: Exception) {
                        // Then try appliedDate (from the main Application class)
                        try {
                            (it as? com.example.jobrec.Application)?.appliedDate?.seconds ?: 0
                        } catch (e3: Exception) {
                            // Then try appliedAt (from the models.Application class)
                            try {
                                it.appliedAt?.seconds ?: 0
                            } catch (e2: Exception) {
                                // Fall back to timestamp as last resort
                                it.timestamp.seconds
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error getting application date for sorting", e)
                    0 // Default value if all attempts fail
                }
            }

            Log.d("CompanyDashboard", "Processed ${sortedApplications.size} applications for display")

            // Update UI on the main thread
            runOnUiThread {
                if (sortedApplications.isNotEmpty()) {
                    recentApplicationsAdapter.submitList(sortedApplications)
                    recentApplicationsRecyclerView.visibility = View.VISIBLE
                    emptyApplicationsView.visibility = View.GONE

                    // Add a "View All" button to the adapter footer
                    // Instead of trying to modify the layout, we'll just add a footer text to the adapter
                    // or we can add a button to the existing layout

                    // Add a click listener to the "View Applications" button
                    findViewById<View>(R.id.btnViewApplications).setOnClickListener {
                        val intent = Intent(this@CompanyDashboardActivityNew, ApplicationsActivity::class.java)
                        intent.putExtra("companyId", companyId)
                        startActivity(intent)
                    }

                } else {
                    recentApplicationsRecyclerView.visibility = View.GONE
                    emptyApplicationsView.visibility = View.VISIBLE
                    emptyApplicationsView.text = "No applications received yet"
                }
            }
        } catch (e: Exception) {
            Log.e("CompanyDashboard", "Error loading recent applications", e)
            runOnUiThread {
                recentApplicationsRecyclerView.visibility = View.GONE
                emptyApplicationsView.visibility = View.VISIBLE
                emptyApplicationsView.text = "Error loading applications"
            }
        }
    }

    private fun applyAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        findViewById<TextView>(R.id.welcomeText).startAnimation(fadeIn)
        findViewById<View>(R.id.btnPostJob).startAnimation(slideUp)
        findViewById<View>(R.id.btnViewApplications).startAnimation(slideUp)
        findViewById<View>(R.id.btnSearchCandidates).startAnimation(slideUp)
        findViewById<View>(R.id.btnMessages).startAnimation(slideUp)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning to dashboard
    }
}
