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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.content.ContextCompat
import com.example.jobrec.chatbot.ChatbotHelper
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
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
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
            supportActionBar?.title = "Company Dashboard"
            setupBottomNavigation()
            setupQuickActions()
            setupSwipeRefresh()
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
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val activeJobsCount = documents.count { doc ->
                    doc.getString("status") == "active"
                }
                this.activeJobsCount.text = activeJobsCount.toString()
            }
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                applicationsCount.text = documents.size().toString()
            }
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
        loadDashboardData()
    }
}