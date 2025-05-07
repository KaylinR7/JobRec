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

            // Get company ID from intent
            companyId = intent.getStringExtra("companyId") ?: run {
                Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Initialize views
            bottomNavigation = findViewById(R.id.bottomNavigation)
            activeJobsCount = findViewById(R.id.activeJobsCount)
            applicationsCount = findViewById(R.id.applicationsCount)
            recentActivityTitle = findViewById(R.id.recentActivityTitle)
            recentActivityDescription = findViewById(R.id.recentActivityDescription)
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

            // Setup toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.company_dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        auth.signOut()
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on home
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, CandidateSearchActivity::class.java))
                    false
                }
                R.id.navigation_applications -> {
                    val intent = Intent(this, ApplicationsActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    false
                }
                R.id.navigation_profile -> {
                    val intent = Intent(this, CompanyProfileActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    false
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
            .addOnFailureListener { e ->
                Log.e("CompanyDashboardActivity", "Error loading active jobs", e)
                activeJobsCount.text = "0"
            }

        // Load applications count
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                applicationsCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("CompanyDashboardActivity", "Error loading applications", e)
                applicationsCount.text = "0"
            }

        // Load recent activity - simplified query
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Sort in memory instead of using orderBy
                    val latestApplication = documents.documents
                        .maxByOrNull { it.getTimestamp("appliedDate")?.seconds ?: 0 }
                    
                    latestApplication?.let {
                        recentActivityTitle.text = "New Application Received"
                        recentActivityDescription.text = "Application for ${it.getString("jobTitle")}"
                    }
                } else {
                    recentActivityTitle.text = "No Recent Activity"
                    recentActivityDescription.text = "You haven't received any applications yet"
                }
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("CompanyDashboardActivity", "Error loading recent activity", e)
                recentActivityTitle.text = "Error Loading Activity"
                recentActivityDescription.text = "Please try again later"
                swipeRefreshLayout.isRefreshing = false
            }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning to dashboard
    }
} 