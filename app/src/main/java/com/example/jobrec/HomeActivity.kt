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
import androidx.lifecycle.lifecycleScope
import com.example.jobrec.ai.JobMatchingRepository
import com.example.jobrec.chatbot.ChatbotHelper
import com.example.jobrec.databinding.ActivityHomeBinding
import com.example.jobrec.adapters.PendingInvitationAdapter
import com.example.jobrec.models.Message
import com.example.jobrec.repositories.ConversationRepository
import com.example.jobrec.repositories.MessageRepository
import com.example.jobrec.utils.SpacingItemDecoration
import kotlinx.coroutines.launch
class HomeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "HomeActivity"
    }
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private lateinit var recentJobsAdapter: RecentJobsAdapter
    private lateinit var recommendedJobsAdapter: JobsAdapter
    private lateinit var pendingInvitationsAdapter: PendingInvitationAdapter
    private lateinit var jobMatchingRepository: JobMatchingRepository
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var messageRepository: MessageRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid
        jobMatchingRepository = JobMatchingRepository()
        conversationRepository = ConversationRepository()
        messageRepository = MessageRepository()
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
        recommendedJobsAdapter = JobsAdapter { job ->
            navigateToJobDetails(job.id)
        }
        pendingInvitationsAdapter = PendingInvitationAdapter(
            invitations = emptyList(),
            onAccept = { invitation ->
                acceptMeetingInvitation(invitation)
            },
            onDecline = { invitation ->
                declineMeetingInvitation(invitation)
            }
        )
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
        binding.pendingInvitationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = pendingInvitationsAdapter
            addItemDecoration(SpacingItemDecoration(8))
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
                R.id.navigation_calendar -> {
                    Log.d("HomeActivity", "Calendar button clicked")
                    Toast.makeText(this, "Calendar button clicked! Opening calendar...", Toast.LENGTH_SHORT).show()
                    try {
                        startActivity(Intent(this, CalendarActivity::class.java))
                    } catch (e: Exception) {
                        Log.e("HomeActivity", "Error starting CalendarActivity", e)
                        Toast.makeText(this, "Error opening calendar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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
        loadPendingInvitations()
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
        Log.d(TAG, "Loading recent jobs with match percentages...")
        lifecycleScope.launch {
            try {
                // Get all jobs with match percentages
                val allJobsWithMatches = jobMatchingRepository.getJobsWithMatches(50)

                // Filter for active jobs and sort by posted date
                val recentJobs = allJobsWithMatches
                    .filter { it.status == "active" }
                    .sortedByDescending { it.postedDate.toDate() }
                    .take(10) // Limit to 10 most recent jobs

                Log.d(TAG, "Successfully loaded ${recentJobs.size} recent jobs with match percentages")
                recentJobsAdapter.submitList(recentJobs)
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent jobs with matches: ${e.message}", e)
                // Fallback to loading jobs without match percentages
                loadRecentJobsFallback()
            }
        }
    }

    private fun loadRecentJobsFallback() {
        Log.d(TAG, "Loading recent jobs fallback...")
        db.collection("jobs")
            .whereEqualTo("status", "active")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} recent jobs")
                val jobs = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Job::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Job object: ${e.message}")
                        null
                    }
                }
                Log.d(TAG, "Successfully mapped ${jobs.size} recent jobs")
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
        Log.d(TAG, "Loading AI-powered recommended jobs...")
        lifecycleScope.launch {
            try {
                val allJobsWithMatches = jobMatchingRepository.getJobsWithMatches(100) // Get more jobs to filter from
                // Filter for recommended jobs (50%+ match with much improved algorithm)
                val highMatchJobs = allJobsWithMatches.filter { job ->
                    job.matchPercentage >= 50
                }.take(10) // Limit to 10 high-match jobs

                Log.d(TAG, "Successfully loaded ${highMatchJobs.size} recommended jobs (50%+ match) from ${allJobsWithMatches.size} total jobs")
                recommendedJobsAdapter.submitList(highMatchJobs)

                // Update subtitle based on results
                updateRecommendedJobsSubtitle(highMatchJobs.size, true)

                // If no high-match jobs found, show empty state message
                if (highMatchJobs.isEmpty()) {
                    Log.d(TAG, "No high-match jobs (75%+) found")
                    updateRecommendedJobsSubtitle(0, true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading AI-powered recommended jobs: ${e.message}", e)
                // Show empty state instead of fallback for recommended jobs
                updateRecommendedJobsSubtitle(0, false)
            }
        }
    }

    private fun loadFallbackRecommendedJobs() {
        Log.d(TAG, "Loading fallback recommended jobs...")
        db.collection("jobs")
            .whereEqualTo("status", "active")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .limit(20) // Get more jobs to potentially find some with matches
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} fallback recommended jobs")
                val jobs = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Job::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Job object: ${e.message}")
                        null
                    }
                }

                // Try to prioritize jobs with any match percentage > 0, then by posted date
                val sortedJobs = jobs.sortedWith(compareByDescending<Job> { it.matchPercentage }.thenByDescending { it.postedDate.toDate() })
                    .take(10)

                Log.d(TAG, "Successfully mapped ${sortedJobs.size} fallback recommended jobs")
                recommendedJobsAdapter.submitList(sortedJobs)

                // Update subtitle for fallback jobs
                updateRecommendedJobsSubtitle(sortedJobs.size, false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading fallback recommended jobs: ${e.message}", e)
                showError("Failed to load recommended jobs: ${e.message}")
            }
    }

    private fun updateRecommendedJobsSubtitle(jobCount: Int, isHighMatch: Boolean) {
        val subtitle = when {
            jobCount == 0 && isHighMatch -> "No matching jobs found at the moment"
            jobCount == 0 -> "No recommendations available at the moment"
            isHighMatch -> "Recommended jobs (50%+ compatibility) • $jobCount found"
            else -> "Recent job postings • $jobCount available"
        }
        binding.recommendedJobsSubtitle.text = subtitle
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
    }
    override fun onPause() {
        super.onPause()
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

    private fun loadPendingInvitations() {
        userId?.let { uid ->
            lifecycleScope.launch {
                try {
                    // Get all conversations where the current user is the receiver
                    val conversations = conversationRepository.getUserConversations(uid)
                    val pendingInvitations = mutableListOf<Message>()

                    // Check each conversation for pending meeting invitations
                    for (conversation in conversations) {
                        val messages = messageRepository.getConversationMessages(conversation.id)
                        val pendingMessages = messages.filter { message: Message ->
                            message.type == "meeting_invite" &&
                            message.receiverId == uid &&
                            message.interviewDetails?.status == "pending"
                        }
                        pendingInvitations.addAll(pendingMessages)
                    }

                    // Update UI
                    if (pendingInvitations.isNotEmpty()) {
                        binding.pendingInvitationsSection.visibility = View.VISIBLE
                        binding.noPendingInvitationsText.visibility = View.GONE
                        pendingInvitationsAdapter.updateInvitations(pendingInvitations)
                    } else {
                        binding.pendingInvitationsSection.visibility = View.GONE
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error loading pending invitations", e)
                    binding.pendingInvitationsSection.visibility = View.GONE
                }
            }
        }
    }

    private fun acceptMeetingInvitation(invitation: Message) {
        lifecycleScope.launch {
            try {
                // Update meeting status to accepted
                conversationRepository.updateMeetingStatus(invitation.id, "accepted")

                // Create calendar event for student
                invitation.interviewDetails?.let { details ->
                    createCalendarEventForStudent(
                        studentId = userId ?: "",
                        messageId = invitation.id,
                        details = details
                    )
                }

                Toast.makeText(this@HomeActivity, "Meeting invitation accepted", Toast.LENGTH_SHORT).show()

                // Reload pending invitations
                loadPendingInvitations()

            } catch (e: Exception) {
                Log.e(TAG, "Error accepting meeting invitation", e)
                Toast.makeText(this@HomeActivity, "Error accepting invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun declineMeetingInvitation(invitation: Message) {
        lifecycleScope.launch {
            try {
                // Update meeting status to rejected
                conversationRepository.updateMeetingStatus(invitation.id, "rejected")

                Toast.makeText(this@HomeActivity, "Meeting invitation declined", Toast.LENGTH_SHORT).show()

                // Reload pending invitations
                loadPendingInvitations()

            } catch (e: Exception) {
                Log.e(TAG, "Error declining meeting invitation", e)
                Toast.makeText(this@HomeActivity, "Error declining invitation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createCalendarEventForStudent(
        studentId: String,
        messageId: String,
        details: com.example.jobrec.models.InterviewDetails
    ) {
        try {
            val eventTitle = "Interview - ${details.jobTitle ?: "Job Interview"}"
            val eventDescription = "Interview with ${details.companyName ?: "Company"}"

            val calendarEvent = com.example.jobrec.models.CalendarEvent(
                userId = studentId,
                title = eventTitle,
                description = eventDescription,
                date = details.date,
                time = details.time,
                duration = details.duration,
                meetingType = details.type,
                location = details.location,
                meetingLink = details.meetingLink,
                notes = "Scheduled via CareerWorx messaging",
                isInterview = true,
                jobId = details.jobId,
                companyId = details.companyId,
                status = "scheduled",
                invitationMessageId = messageId
            )

            db.collection("calendar_events").add(calendarEvent)
            Log.d(TAG, "Calendar event created for student: $studentId")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating calendar event for student", e)
            throw e
        }
    }
}