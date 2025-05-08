package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.content.ContextCompat

class CompanyDashboardActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var activeJobsCount: TextView
    private lateinit var applicationsCount: TextView
    private lateinit var recentActivityTitle: TextView
    private lateinit var recentActivityDescription: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_dashboard)

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            // Get company ID from intent or find it using the current user's email
            companyId = intent.getStringExtra("companyId") ?: run {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Query companies collection to find the company with matching email
                    db.collection("companies")
                        .whereEqualTo("email", currentUser.email)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                companyId = documents.documents[0].id
                                // Initialize views and load data after getting company ID
                                initializeViews()
                                setupBottomNavigation()
                                setupQuickActions()
                                setupSwipeRefresh()
                                loadDashboardData()
                            } else {
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

            // Setup toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Company Dashboard"

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
        recentActivityTitle = findViewById(R.id.recentActivityTitle)
        recentActivityDescription = findViewById(R.id.recentActivityDescription)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
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
            R.id.action_logout -> {
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
        // Load active jobs count
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                activeJobsCount.text = documents.size().toString()
            }

        // Load applications count
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                applicationsCount.text = documents.size().toString()
            }

        // Load recent activity
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val application = documents.documents[0]
                    recentActivityTitle.text = "New Application"
                    recentActivityDescription.text = "Received application for ${application.getString("jobTitle")}"
                } else {
                    recentActivityTitle.text = "No Recent Activity"
                    recentActivityDescription.text = "Check back later for updates"
                }
                swipeRefreshLayout.isRefreshing = false
            }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning to dashboard
    }
} 