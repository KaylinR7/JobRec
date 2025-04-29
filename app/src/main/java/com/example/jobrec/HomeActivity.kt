package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {
    private lateinit var notificationsButton: MaterialButton
    private lateinit var profileButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recentJobsRecyclerView: RecyclerView
    private lateinit var recommendedJobsRecyclerView: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var searchCard: MaterialCardView
    private lateinit var myApplicationsCard: MaterialCardView
    private lateinit var jobAlertsCard: MaterialCardView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var recentJobsAdapter: RecentJobsAdapter
    private lateinit var recommendedJobsAdapter: RecentJobsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Create Firestore indexes
        FirestoreIndexManager.createIndexes()
        
        // Get user ID from intent extras
        userId = intent.getStringExtra("userId")
        Log.d("HomeActivity", "User ID from intent: $userId")

        // Initialize views
        initializeViews()
        
        // Set up toolbar
        setupToolbar()
        
        // Set up RecyclerViews
        setupRecyclerViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up bottom navigation
        setupBottomNavigation()

        // Load data
        loadRecentJobs()
        loadRecommendedJobs()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        notificationsButton = findViewById(R.id.notificationsButton)
        profileButton = findViewById(R.id.profileButton)
        recentJobsRecyclerView = findViewById(R.id.recentJobsRecyclerView)
        recommendedJobsRecyclerView = findViewById(R.id.recommendedJobsRecyclerView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        searchCard = findViewById(R.id.searchCard)
        myApplicationsCard = findViewById(R.id.myApplicationsCard)
        jobAlertsCard = findViewById(R.id.jobAlertsCard)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
        toolbar.setNavigationIcon(null)
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert))
    }

    private fun setupRecyclerViews() {
        // Set up recent jobs RecyclerView
        recentJobsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recentJobsAdapter = RecentJobsAdapter { job ->
            navigateToJobDetails(job.id)
        }
        recentJobsRecyclerView.adapter = recentJobsAdapter

        // Set up recommended jobs RecyclerView
        recommendedJobsRecyclerView.layoutManager = LinearLayoutManager(this)
        recommendedJobsAdapter = RecentJobsAdapter { job ->
            navigateToJobDetails(job.id)
        }
        recommendedJobsRecyclerView.adapter = recommendedJobsAdapter
    }

    private fun setupClickListeners() {
        notificationsButton.setOnClickListener {
            startActivity(Intent(this, JobAlertsActivity::class.java))
        }

        profileButton.setOnClickListener {
            navigateToProfile()
        }

        searchCard.setOnClickListener {
            startActivity(Intent(this, CandidateSearchActivity::class.java))
        }

        myApplicationsCard.setOnClickListener {
            startActivity(Intent(this, MyApplicationsActivity::class.java))
        }

        jobAlertsCard.setOnClickListener {
            startActivity(Intent(this, JobAlertsActivity::class.java))
        }
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
                    startActivity(Intent(this, MyApplicationsActivity::class.java))
                    false
                }
                R.id.navigation_profile -> {
                    navigateToProfile()
                    false
                }
                else -> false
            }
        }
    }

    private fun loadRecentJobs() {
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { document ->
                    try {
                        Job(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            companyName = document.getString("companyName") ?: "",
                            location = document.getString("location") ?: "",
                            description = document.getString("description") ?: "",
                            requirements = document.getString("requirements") ?: "",
                            type = document.getString("type") ?: "Full-time",
                            salary = document.getString("salary") ?: "",
                            postedDate = document.getTimestamp("postedDate") ?: Timestamp.now(),
                            companyId = document.getString("companyId") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("HomeActivity", "Error parsing job document: ${document.id}", e)
                        null
                    }
                }
                recentJobsAdapter.updateJobs(jobs)
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error loading recent jobs", e)
                Toast.makeText(this, "Error loading recent jobs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRecommendedJobs() {
        // TODO: Implement job recommendations based on user profile
        // For now, just load some jobs
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { document ->
                    try {
                        Job(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            companyName = document.getString("companyName") ?: "",
                            location = document.getString("location") ?: "",
                            description = document.getString("description") ?: "",
                            requirements = document.getString("requirements") ?: "",
                            type = document.getString("type") ?: "Full-time",
                            salary = document.getString("salary") ?: "",
                            postedDate = document.getTimestamp("postedDate") ?: Timestamp.now(),
                            companyId = document.getString("companyId") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("HomeActivity", "Error parsing job document: ${document.id}", e)
                        null
                    }
                }
                recommendedJobsAdapter.updateJobs(jobs)
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error loading recommended jobs", e)
                Toast.makeText(this, "Error loading recommended jobs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToProfile() {
        if (userId != null) {
            Log.d("HomeActivity", "Navigating to ProfileActivity with user ID: $userId")
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        } else {
            // If we don't have the user ID, try to get it from Firestore
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("HomeActivity", "Getting user ID for email: ${currentUser.email}")
                getUserByIdFromEmail(currentUser.email!!)
            } else {
                Log.e("HomeActivity", "No user ID and no Firebase Auth user")
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToJobDetails(jobId: String) {
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("jobId", jobId)
        startActivity(intent)
    }

    private fun getUserByIdFromEmail(email: String) {
        Log.d("HomeActivity", "Getting user ID for email: $email")
        db.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("HomeActivity", "No user found with email: $email")
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Get the first matching document
                val document = documents.documents[0]
                val userId = document.id
                Log.d("HomeActivity", "Found user ID: $userId")
                
                // Navigate to ProfileActivity with user ID
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error getting user ID", e)
                Toast.makeText(this, "Error getting user data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_my_applications -> {
                startActivity(Intent(this, MyApplicationsActivity::class.java))
                true
            }
            R.id.action_job_alerts -> {
                startActivity(Intent(this, JobAlertsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 