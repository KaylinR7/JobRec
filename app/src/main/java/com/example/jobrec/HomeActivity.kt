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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {
    private lateinit var logoutButton: MaterialButton
    private lateinit var helpButton: MaterialButton
    private lateinit var contactButton: MaterialButton
    private lateinit var profileButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var recentJobsRecyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var recentJobsAdapter: RecentJobsAdapter

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
        toolbar = findViewById(R.id.toolbar)
        logoutButton = findViewById(R.id.logoutButton)
        helpButton = findViewById(R.id.helpButton)
        contactButton = findViewById(R.id.contactButton)
        profileButton = findViewById(R.id.profileButton)
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView)
        recentJobsRecyclerView = findViewById(R.id.recentJobsRecyclerView)

        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
        toolbar.setNavigationIcon(null)
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert))

        // Set up RecyclerViews
        jobsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Set up recent jobs RecyclerView
        recentJobsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recentJobsAdapter = RecentJobsAdapter { job ->
            // Handle job click
            val intent = Intent(this, JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }
        recentJobsRecyclerView.adapter = recentJobsAdapter

        // Load recent jobs
        loadRecentJobs()

        // Set up button click listeners
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        helpButton.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        contactButton.setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }

        profileButton.setOnClickListener {
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

    private fun getPostedDate(document: DocumentSnapshot): Timestamp {
        return document.getTimestamp("postedDate") ?: Timestamp.now()
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
            else -> super.onOptionsItemSelected(item)
        }
    }
} 