package com.example.jobrec
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.jobrec.notifications.NotificationPermissionHelper
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
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_dashboard_new)
        try {
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            notificationPermissionHelper = NotificationPermissionHelper(this)
            companyId = intent.getStringExtra("companyId") ?: run {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userEmail = currentUser.email?.lowercase() ?: ""
                    Log.d("CompanyDashboard", "Looking for company with email (lowercase): $userEmail")
                    db.collection("companies")
                        .get()
                        .addOnSuccessListener { documents ->
                            val companyDoc = documents.find { doc ->
                                doc.getString("email")?.lowercase() == userEmail
                            }
                            if (companyDoc != null) {
                                val registrationNumber = companyDoc.getString("registrationNumber")
                                if (registrationNumber != null) {
                                    companyId = registrationNumber
                                    Log.d("CompanyDashboard", "Found company with registration number: $companyId")
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
            initializeViews()
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowHomeEnabled(false)
                title = "Company Dashboard"
            }
            setupBottomNavigation()
            setupQuickActions()
            setupSwipeRefresh()
            loadDashboardData()

            // Request notification permissions for companies
            notificationPermissionHelper.requestNotificationPermission(
                onGranted = {
                    Log.d("CompanyDashboard", "Notification permission granted for company")
                    // Subscribe to company-specific topics
                    com.example.jobrec.notifications.NotificationManager.getInstance()
                        .subscribeToTopics("company")
                },
                onDenied = {
                    Log.d("CompanyDashboard", "Notification permission denied for company")
                }
            )
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
        setupRecyclerViews()
    }
    private fun setupRecyclerViews() {
        recentActivityAdapter = RecentActivityAdapter { activity ->
            handleActivityClick(activity)
        }
        recentActivityRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CompanyDashboardActivityNew, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentActivityAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }
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
                R.id.nav_calendar -> {
                    val intent = Intent(this, CompanyCalendarActivity::class.java)
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
        findViewById<View>(R.id.btnAiAssistant).setOnClickListener {
            val intent = Intent(this, com.example.jobrec.chatbot.ChatbotActivity::class.java)
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
        swipeRefreshLayout.isRefreshing = true
        emptyActivityView.text = "Loading activities..."
        emptyActivityView.visibility = View.VISIBLE
        emptyApplicationsView.text = "Loading applications..."
        emptyApplicationsView.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                loadStats()
                val activitiesJob = lifecycleScope.launch { loadRecentActivities() }
                val applicationsJob = lifecycleScope.launch { loadRecentApplications() }
                activitiesJob.join()
                applicationsJob.join()
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                    applyAnimations()
                }
            } catch (e: Exception) {
                Log.e("CompanyDashboard", "Error loading dashboard data", e)
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        this@CompanyDashboardActivityNew,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
            val jobsSnapshot = db.collection("jobs")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
            val activeJobsCount = jobsSnapshot.count { doc ->
                doc.getString("status") == "active"
            }
            this.activeJobsCount.text = activeJobsCount.toString()
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
            val applicationsSnapshot = db.collection("applications")
                .whereEqualTo("companyId", companyId)
                .limit(20)
                .get()
                .await()
            Log.d("CompanyDashboard", "Found ${applicationsSnapshot.size()} recent applications")
            for (doc in applicationsSnapshot.documents) {
                try {
                    val application = doc.toObject(Application::class.java)?.copy(id = doc.id) ?: continue
                    var candidateName = application.candidateName ?: ""
                    if (candidateName.isBlank()) {
                        try {
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
                                    Timestamp.now()
                                }
                            }
                        )
                    )
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error processing application document", e)
                }
            }
            val jobsSnapshot = db.collection("jobs")
                .whereEqualTo("companyId", companyId)
                .limit(10)
                .get()
                .await()
            Log.d("CompanyDashboard", "Found ${jobsSnapshot.size()} recent job posts")
            for (doc in jobsSnapshot.documents) {
                try {
                    val job = doc.toObject(Job::class.java)?.copy(id = doc.id) ?: continue
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
            try {
                val conversationsSnapshot = db.collection("conversations")
                    .whereEqualTo("companyId", companyId)
                    .limit(5)
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
            val sortedActivities = activities.sortedByDescending {
                it.timestamp.seconds
            }
            Log.d("CompanyDashboard", "Total activities after sorting: ${sortedActivities.size}")
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
            val applicationsSnapshot = db.collection("applications")
                .whereEqualTo("companyId", companyId)
                .limit(3)
                .get()
                .await()
            Log.d("CompanyDashboard", "Found ${applicationsSnapshot.size()} recent applications")
            val applications = mutableListOf<Application>()
            for (doc in applicationsSnapshot.documents) {
                try {
                    val application = doc.toObject(Application::class.java)?.copy(id = doc.id) ?: continue
                    if (application.candidateName.isNullOrEmpty()) {
                        try {
                            try {
                                val appObj = doc.toObject(com.example.jobrec.Application::class.java)
                                if (!appObj?.applicantName.isNullOrEmpty()) {
                                    application.candidateName = appObj?.applicantName
                                    Log.d("CompanyDashboard", "Using applicantName: ${application.candidateName}")
                                }
                            } catch (e: Exception) {
                                Log.d("CompanyDashboard", "Could not get applicantName, trying user document")
                            }
                            if (application.candidateName.isNullOrEmpty()) {
                                val userId = application.candidateId.takeIf { it.isNotEmpty() }
                                    ?: doc.getString("userId")
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
                    applications.add(application)
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error processing application document", e)
                }
            }
            val sortedApplications = applications.sortedByDescending {
                try {
                    try {
                        val appliedDateField = it.javaClass.getDeclaredField("applieddate")
                        appliedDateField.isAccessible = true
                        val appliedDateValue = appliedDateField.get(it) as? Timestamp
                        appliedDateValue?.seconds ?: 0
                    } catch (e: Exception) {
                        try {
                            (it as? com.example.jobrec.Application)?.appliedDate?.seconds ?: 0
                        } catch (e3: Exception) {
                            try {
                                it.appliedAt?.seconds ?: 0
                            } catch (e2: Exception) {
                                it.timestamp.seconds
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CompanyDashboard", "Error getting application date for sorting", e)
                    0
                }
            }
            Log.d("CompanyDashboard", "Processed ${sortedApplications.size} applications for display")
            runOnUiThread {
                if (sortedApplications.isNotEmpty()) {
                    recentApplicationsAdapter.submitList(sortedApplications)
                    recentApplicationsRecyclerView.visibility = View.VISIBLE
                    emptyApplicationsView.visibility = View.GONE
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
        loadDashboardData()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        notificationPermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Log.d("CompanyDashboard", "Notification permission granted in CompanyDashboardActivityNew")
                com.example.jobrec.notifications.NotificationManager.getInstance()
                    .subscribeToTopics("company")
            },
            onDenied = {
                Log.d("CompanyDashboard", "Notification permission denied in CompanyDashboardActivityNew")
            }
        )
    }
}
