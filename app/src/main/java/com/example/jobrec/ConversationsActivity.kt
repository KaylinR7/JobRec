package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jobrec.adapters.ConversationAdapter
import com.example.jobrec.models.Conversation
import com.example.jobrec.repositories.ConversationRepository
import com.example.jobrec.utils.ConversationFixer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConversationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fixConversationsButton: Button
    private lateinit var forceCompanyModeButton: Button

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var conversationsListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var isLoading = false
    private var isCompanyUser = false

    // Auto-refresh handler
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInterval = 5000L // 5 seconds
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isLoading) {
                loadConversations()
            }
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize repository
        conversationRepository = ConversationRepository()

        // Check if user is a company
        checkUserType()

        // Initialize views
        initializeViews()
        setupToolbar()
        setupRecyclerView()

        // Load conversations
        startConversationsListener()
    }

    private fun checkUserType() {
        android.util.Log.d("ConversationsActivity", "Checking user type for userId: $currentUserId")

        // First check if the user's email is in the companies collection
        val currentUserEmail = auth.currentUser?.email
        if (currentUserEmail != null) {
            db.collection("companies")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // User found as company by email
                        isCompanyUser = true
                        val companyDoc = documents.documents[0]
                        val companyId = companyDoc.id
                        val companyName = companyDoc.getString("companyName") ?: "unknown"

                        // Update the company document with the userId field if it doesn't exist
                        if (companyDoc.getString("userId") == null) {
                            db.collection("companies")
                                .document(companyId)
                                .update("userId", currentUserId)
                                .addOnSuccessListener {
                                    android.util.Log.d("ConversationsActivity", "Updated company document with userId: $currentUserId")
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("ConversationsActivity", "Failed to update company with userId", e)
                                }
                        }

                        android.util.Log.d("ConversationsActivity", "User is company (by email): true, companyId: $companyId, name: $companyName")
                        loadConversations()
                    } else {
                        // If not found by email, check if user exists in companies collection with userId field
                        db.collection("companies")
                            .whereEqualTo("userId", currentUserId)
                            .get()
                            .addOnSuccessListener { userIdDocs ->
                                if (!userIdDocs.isEmpty) {
                                    // User found as company with userId field
                                    isCompanyUser = true
                                    val companyDoc = userIdDocs.documents[0]
                                    val companyId = companyDoc.id
                                    val companyName = companyDoc.getString("companyName") ?: "unknown"
                                    android.util.Log.d("ConversationsActivity", "User is company (by userId): true, companyId: $companyId, name: $companyName")
                                } else {
                                    // Not a company user
                                    isCompanyUser = false
                                    android.util.Log.d("ConversationsActivity", "User is not a company")
                                }
                                loadConversations()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("ConversationsActivity", "Error checking company by userId", e)
                                isCompanyUser = false
                                loadConversations()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ConversationsActivity", "Error checking company by email", e)
                    isCompanyUser = false
                    loadConversations()
                }
        } else {
            // No email available, default to student
            android.util.Log.d("ConversationsActivity", "No email available for current user, defaulting to student")
            isCompanyUser = false
            loadConversations()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        fixConversationsButton = findViewById(R.id.fixConversationsButton)

        // Create force company mode button programmatically (hidden for presentation)
        forceCompanyModeButton = Button(this)
        forceCompanyModeButton.text = "Force Company Mode"
        forceCompanyModeButton.setBackgroundColor(resources.getColor(android.R.color.black))
        forceCompanyModeButton.setTextColor(resources.getColor(android.R.color.white))
        forceCompanyModeButton.visibility = View.GONE

        // Add the button to the layout
        val layout = findViewById<LinearLayout>(R.id.buttonsLayout)
        layout.addView(forceCompanyModeButton)

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadConversations()
        }

        // Setup fix conversations button (hidden for presentation)
        fixConversationsButton.visibility = View.GONE
        fixConversationsButton.setOnClickListener {
            lifecycleScope.launch {
                fixConversations()
            }
        }

        // Setup force company mode button
        forceCompanyModeButton.setOnClickListener {
            toggleCompanyMode()
        }

        // Automatically enable company mode for presentation
        enableCompanyMode()

        // Automatically fix conversations on startup
        lifecycleScope.launch {
            fixConversations()
        }
    }

    private fun enableCompanyMode() {
        // This method is now disabled to prevent data issues
        android.util.Log.d("ConversationsActivity", "Company mode auto-enabling is disabled to prevent data issues")

        // Don't force company mode anymore
        val sharedPreferences = getSharedPreferences("JobRecPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("force_company_mode", false).apply()
    }

    private fun toggleCompanyMode() {
        val sharedPreferences = getSharedPreferences("JobRecPrefs", MODE_PRIVATE)
        val currentMode = sharedPreferences.getBoolean("force_company_mode", false)
        val newMode = !currentMode

        sharedPreferences.edit().putBoolean("force_company_mode", newMode).apply()

        isCompanyUser = newMode
        android.util.Log.d("ConversationsActivity", "Company mode set to: $newMode")

        // Update button text
        forceCompanyModeButton.text = if (newMode) "Disable Company Mode" else "Force Company Mode"

        // Reload conversations
        loadConversations()
    }

    private suspend fun fixConversations() {
        // This method is now a no-op to prevent modifying conversations that don't belong to the user
        android.util.Log.d("ConversationsActivity", "Conversation fixing is disabled to prevent data issues")

        // Just load conversations without modifying anything
        loadConversations()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messages"

        // Explicitly set white navigation icon
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            // Open chat activity when a conversation is clicked
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("conversationId", conversation.id)
            }
            startActivity(intent)
        }

        conversationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConversationsActivity)
            adapter = conversationAdapter
        }
    }

    private fun startConversationsListener() {
        // We don't need to load conversations here anymore
        // The checkUserType method will call loadConversations after determining the user type
    }

    private fun loadConversations() {
        if (isLoading) return
        isLoading = true

        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true

        // Stop any existing listener
        conversationsListener?.remove()

        // Check if we need to force company mode
        val sharedPreferences = getSharedPreferences("JobRecPrefs", MODE_PRIVATE)
        val forceCompanyMode = sharedPreferences.getBoolean("force_company_mode", false)

        if (forceCompanyMode) {
            isCompanyUser = true
            android.util.Log.d("ConversationsActivity", "Forcing company mode")
        }

        // Log user type for debugging
        val userType = if (isCompanyUser) "COMPANY" else "STUDENT"
        android.util.Log.d("ConversationsActivity", "Loading conversations for $userType user: $currentUserId")

        // Load conversations using the repository
        lifecycleScope.launch {
            try {
                val conversations = conversationRepository.getUserConversations(currentUserId)

                // Log conversation data for debugging
                android.util.Log.d("ConversationsActivity", "User ID: $currentUserId")
                android.util.Log.d("ConversationsActivity", "Found ${conversations.size} conversations")
                conversations.forEach { conversation ->
                    android.util.Log.d("ConversationsActivity", "Conversation: ${conversation.id}, " +
                            "Company: ${conversation.companyId} (${conversation.companyName}), " +
                            "Candidate: ${conversation.candidateId} (${conversation.candidateName})")
                }

                // Update UI
                if (conversations.isNotEmpty()) {
                    conversationAdapter.submitList(conversations)
                    emptyView.visibility = View.GONE
                } else {
                    // If we have no conversations, show empty state
                    conversationAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE

                    // Log more details about why we might not have conversations
                    android.util.Log.d("ConversationsActivity", "No conversations found. User type: $userType, userId: $currentUserId")

                    // Check if there are any conversations in the database at all
                    val allConversations = db.collection("conversations")
                        .get()
                        .await()

                    android.util.Log.d("ConversationsActivity", "Total conversations in database: ${allConversations.size()}")

                    if (allConversations.isEmpty) {
                        android.util.Log.d("ConversationsActivity", "No conversations exist in the database")
                    } else {
                        android.util.Log.d("ConversationsActivity", "Conversations exist but none match this user")

                        // Log the first few conversations for debugging
                        val limit = minOf(5, allConversations.size())
                        for (i in 0 until limit) {
                            val doc = allConversations.documents[i]
                            val companyId = doc.getString("companyId") ?: "null"
                            val candidateId = doc.getString("candidateId") ?: "null"
                            android.util.Log.d("ConversationsActivity", "Sample conversation ${doc.id}: companyId=$companyId, candidateId=$candidateId")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationsActivity", "Error loading conversations", e)
                Toast.makeText(this@ConversationsActivity, "Error loading conversations: ${e.message}", Toast.LENGTH_SHORT).show()

                // Show empty state and fix button on error
                conversationAdapter.submitList(emptyList())
                emptyView.visibility = View.VISIBLE
                fixConversationsButton.visibility = View.VISIBLE
            } finally {
                // Hide loading indicator
                swipeRefreshLayout.isRefreshing = false
                isLoading = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Start auto-refresh when activity is resumed
        refreshHandler.postDelayed(refreshRunnable, refreshInterval)
    }

    override fun onPause() {
        super.onPause()
        // Stop auto-refresh when activity is paused
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationsListener?.remove()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}
