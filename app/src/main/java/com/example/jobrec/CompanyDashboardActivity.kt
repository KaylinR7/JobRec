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

class CompanyDashboardActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var activeJobsCount: TextView
    private lateinit var applicationsCount: TextView
    private lateinit var recentActivityTitle: TextView
    private lateinit var recentActivityDescription: TextView

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

            // Setup toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            // Setup bottom navigation
            setupBottomNavigation()

            // Setup quick action buttons
            setupQuickActions()

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
            }

        // Load recent activity
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val latestApplication = documents.documents[0]
                    recentActivityTitle.text = "New Application Received"
                    recentActivityDescription.text = "Application for ${latestApplication.getString("jobTitle")}"
                }
            }
            .addOnFailureListener { e ->
                Log.e("CompanyDashboardActivity", "Error loading recent activity", e)
            }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh data when returning to dashboard
    }
} 