package com.example.jobrec
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.core.content.ContextCompat
import com.example.jobrec.chatbot.ChatbotHelper
import com.example.jobrec.databinding.ActivityHomeBinding
import com.example.jobrec.utils.NotificationHelper
class HomeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "HomeActivity"
    }
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var recentJobsAdapter: RecentJobsAdapter
    private lateinit var recommendedJobsAdapter: RecentJobsAdapter
    private lateinit var notificationHelper: NotificationHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        setupSwipeRefresh()
        loadData()
    }
    private fun initializeViews() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Student Dashboard"
        recentJobsAdapter = RecentJobsAdapter { job ->
            navigateToJobDetails(job.id)
        }
        recommendedJobsAdapter = RecentJobsAdapter { job ->
            navigateToJobDetails(job.id)
        }
        binding.recentJobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentJobsAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }
        binding.recommendedJobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = recommendedJobsAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }
        applyEntranceAnimations()
    }
    private fun applyEntranceAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.welcomeText.startAnimation(fadeIn)
        binding.searchCard.startAnimation(slideUp)
        binding.myApplicationsCard.startAnimation(slideUp)
        binding.jobAlertsCard.startAnimation(slideUp)
    }
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.accent),
            ContextCompat.getColor(this, R.color.primary_dark)
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }
    private fun setupClickListeners() {
        binding.searchCard.setOnClickListener {
            animateClick(binding.searchCard) {
                startActivity(Intent(this, SearchActivity::class.java))
            }
        }
        binding.myApplicationsCard.setOnClickListener {
            animateClick(binding.myApplicationsCard) {
                startActivity(Intent(this, MyApplicationsActivity::class.java))
            }
        }
        binding.jobAlertsCard.setOnClickListener {
            animateClick(binding.jobAlertsCard) {
                startActivity(Intent(this, ConversationsActivity::class.java))
            }
        }
        findViewById<View>(R.id.savedJobsCard).setOnClickListener {
            animateClick(findViewById(R.id.savedJobsCard)) {
                startActivity(Intent(this, SavedJobsActivity::class.java))
            }
        }
    }
    private fun logout() {
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
    }
    private fun animateClick(view: View, action: () -> Unit) {
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        view.startAnimation(bounceAnimation)
        bounceAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                action()
            }
        })
    }
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_applications -> {
                    startActivity(Intent(this, MyApplicationsActivity::class.java))
                    false
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                R.id.navigation_ai_assistant -> {
                    startActivity(Intent(this, com.example.jobrec.chatbot.ChatbotActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
    private fun loadData() {
        loadUserData()
        loadRecentJobs()
        loadRecommendedJobs()
        loadStats()
    }
    private fun loadUserData() {
        val isDefaultStudent = intent.getBooleanExtra("isDefaultStudent", false)
        binding.returnToCompanyView.visibility = View.GONE
        val userId = intent.getStringExtra("userId") ?: FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "Loading user data for userId: $userId")
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    Log.d(TAG, "Document exists: ${document.exists()}")
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        Log.d(TAG, "Retrieved name: $name")
                        val displayName = if (!name.isNullOrEmpty()) {
                            name.trim()
                        } else {
                            Log.d(TAG, "Name is empty, using default name")
                            "Student"
                        }
                        binding.welcomeText.text = "Welcome back, $displayName!"
                    } else {
                        Log.d(TAG, "Document does not exist or is null")
                        binding.welcomeText.text = "Welcome back, Student!"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user data", e)
                    binding.welcomeText.text = "Welcome back, Student!"
                }
        } else {
            Log.d(TAG, "No user ID found")
            binding.welcomeText.text = "Welcome back, Student!"
        }
    }
    private fun loadStats() {
        userId?.let { uid ->
            db.collection("applications")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    binding.applicationsCount.text = documents.size().toString()
                    binding.applicationsCount.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error loading applications count", e)
                }
            db.collection("savedJobs")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    binding.savedJobsCount.text = documents.size().toString()
                    binding.savedJobsCount.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error loading saved jobs count", e)
                }
        }
    }
    private fun loadRecentJobs() {
        Log.d(TAG, "Loading recent jobs...")
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} jobs")
                val jobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java).copy(id = doc.id)
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Job object: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } 
                Log.d(TAG, "Successfully mapped ${jobs.size} jobs")
                recentJobsAdapter.submitList(jobs)
                binding.swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading recent jobs: ${e.message}", e)
                showError("Failed to load recent jobs: ${e.message}")
                binding.swipeRefreshLayout.isRefreshing = false
            }
    }
    private fun loadRecommendedJobs() {
        Log.d(TAG, "Loading recommended jobs...")
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} recommended jobs")
                val jobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java).copy(id = doc.id)
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Job object: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } 
                Log.d(TAG, "Successfully mapped ${jobs.size} recommended jobs")
                recommendedJobsAdapter.submitList(jobs)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading recommended jobs: ${e.message}", e)
                showError("Failed to load recommended jobs: ${e.message}")
            }
    }
    private fun navigateToJobDetails(jobId: String) {
        if (jobId.isBlank()) {
            Log.e(TAG, "Invalid job ID: $jobId")
            return
        }
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("jobId", jobId)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun onResume() {
        super.onResume()
        notificationHelper.startJobNotificationsListener()
    }
    override fun onPause() {
        super.onPause()
        notificationHelper.stopJobNotificationsListener()
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
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
                true
            }
            R.id.action_saved_jobs -> {
                startActivity(Intent(this, SavedJobsActivity::class.java))
                true
            }
            R.id.action_messages -> {
                startActivity(Intent(this, ConversationsActivity::class.java))
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}