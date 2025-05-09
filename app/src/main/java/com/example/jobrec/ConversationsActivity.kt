package com.example.jobrec

import android.content.Intent
import android.os.Bundle
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

        // First check if user exists in companies collection with userId field
        db.collection("companies")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // User found as company with userId field
                    isCompanyUser = true
                    val companyDoc = documents.documents[0]
                    val companyId = companyDoc.getString("companyId") ?: "unknown"
                    val companyName = companyDoc.getString("companyName") ?: "unknown"
                    android.util.Log.d("ConversationsActivity", "Company details - companyId: $companyId, name: $companyName")
                    loadConversations()
                } else {
                    // If not found, check if user exists with companyId field
                    db.collection("companies")
                        .whereEqualTo("companyId", currentUserId)
                        .get()
                        .addOnSuccessListener { companyDocs ->
                            isCompanyUser = !companyDocs.isEmpty
                            android.util.Log.d("ConversationsActivity", "User is company (by companyId): $isCompanyUser")

                            if (isCompanyUser && !companyDocs.isEmpty) {
                                val companyDoc = companyDocs.documents[0]
                                val companyId = companyDoc.getString("companyId") ?: "unknown"
                                val companyName = companyDoc.getString("companyName") ?: "unknown"
                                android.util.Log.d("ConversationsActivity", "Company details - companyId: $companyId, name: $companyName")
                            }

                            // Also check if the user ID matches any companyId in conversations
                            db.collection("conversations")
                                .whereEqualTo("companyId", currentUserId)
                                .get()
                                .addOnSuccessListener { conversationDocs ->
                                    if (!conversationDocs.isEmpty) {
                                        isCompanyUser = true
                                        android.util.Log.d("ConversationsActivity", "User is company (found in conversations): true")
                                    }

                                    // Finally, load conversations
                                    loadConversations()
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("ConversationsActivity", "Error checking conversations", e)
                                    loadConversations()
                                }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("ConversationsActivity", "Error checking company by companyId", e)
                            isCompanyUser = false
                            loadConversations()
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ConversationsActivity", "Error checking user type", e)
                // Default to student if there's an error
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
        // Always enable company mode for presentation
        val sharedPreferences = getSharedPreferences("JobRecPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("force_company_mode", true).apply()

        isCompanyUser = true
        android.util.Log.d("ConversationsActivity", "Company mode automatically enabled")
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
        // No need to update button UI since it's hidden

        try {
            // First try the regular fixer
            val fixer = ConversationFixer()
            fixer.fixAllConversations()

            // If that didn't work, try a direct approach
            // Get all conversations
            val conversations = db.collection("conversations")
                .get()
                .await()

            android.util.Log.d("ConversationsActivity", "Direct fix: Found ${conversations.size()} conversations")

            // For each conversation with a numeric companyId, update it to the current user ID
            for (conversationDoc in conversations.documents) {
                val companyId = conversationDoc.getString("companyId") ?: continue

                // If companyId is numeric, update it to current user ID
                if (companyId.matches(Regex("\\d+"))) {
                    android.util.Log.d("ConversationsActivity", "Direct fix: Updating conversation ${conversationDoc.id} with current user ID")

                    // Update the conversation with current user ID
                    db.collection("conversations")
                        .document(conversationDoc.id)
                        .update("companyId", currentUserId)
                        .await()

                    android.util.Log.d("ConversationsActivity", "Direct fix: Successfully updated conversation ${conversationDoc.id}")
                }
            }

            // Don't show toast for presentation
            // Toast.makeText(this@ConversationsActivity, "Conversations fixed successfully", Toast.LENGTH_SHORT).show()
            loadConversations()
        } catch (e: Exception) {
            android.util.Log.e("ConversationsActivity", "Error fixing conversations", e)
            // Don't show error toast for presentation
            // Toast.makeText(this@ConversationsActivity, "Error fixing conversations: ${e.message}", Toast.LENGTH_SHORT).show()
            loadConversations() // Still try to load conversations even if fixing failed
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messages"
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

        // Load conversations using the repository instead of direct Firestore queries
        lifecycleScope.launch {
            try {
                // If in company mode, try to directly update any conversations with numeric companyId
                if (isCompanyUser) {
                    val allConversationsCheck = db.collection("conversations")
                        .get()
                        .await()

                    for (doc in allConversationsCheck.documents) {
                        val companyId = doc.getString("companyId") ?: continue

                        // If companyId is numeric, update it to current user ID
                        if (companyId.matches(Regex("\\d+"))) {
                            android.util.Log.d("ConversationsActivity", "Direct update: Found numeric companyId $companyId in conversation ${doc.id}")

                            // Update the conversation with current user ID
                            db.collection("conversations")
                                .document(doc.id)
                                .update("companyId", currentUserId)
                                .await()

                            android.util.Log.d("ConversationsActivity", "Direct update: Successfully updated conversation ${doc.id}")
                        }
                    }
                }

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
                    // Buttons always hidden for presentation
                } else {
                    // If in company mode and no conversations found, try one more direct approach
                    if (isCompanyUser) {
                        // Get all conversations
                        val allConversations = db.collection("conversations")
                            .get()
                            .await()

                        if (!allConversations.isEmpty) {
                            // Take the first conversation and force it to use this user ID
                            val firstConversation = allConversations.documents[0]
                            android.util.Log.d("ConversationsActivity", "Last resort: Updating conversation ${firstConversation.id} with current user ID")

                            // Update the conversation with current user ID
                            db.collection("conversations")
                                .document(firstConversation.id)
                                .update("companyId", currentUserId)
                                .await()

                            android.util.Log.d("ConversationsActivity", "Last resort: Successfully updated conversation ${firstConversation.id}")

                            // Try to load conversations again
                            val updatedConversations = conversationRepository.getUserConversations(currentUserId)
                            if (updatedConversations.isNotEmpty()) {
                                conversationAdapter.submitList(updatedConversations)
                                emptyView.visibility = View.GONE
                                return@launch
                            }
                        }
                    }

                    // If we still have no conversations, show empty state
                    conversationAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                    // Buttons always hidden for presentation

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

    override fun onDestroy() {
        super.onDestroy()
        conversationsListener?.remove()
    }
}
