package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var conversationsListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var isLoading = false

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

        // Initialize views
        initializeViews()
        setupToolbar()
        setupRecyclerView()

        // Load conversations
        startConversationsListener()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadConversations()
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
        // Initial load
        loadConversations()
    }

    private fun loadConversations() {
        if (isLoading) return
        isLoading = true

        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true

        // Stop any existing listener
        conversationsListener?.remove()

        // Load conversations using the repository instead of direct Firestore queries
        lifecycleScope.launch {
            try {
                val conversations = conversationRepository.getUserConversations(currentUserId)

                // Update UI
                if (conversations.isNotEmpty()) {
                    conversationAdapter.submitList(conversations)
                    emptyView.visibility = View.GONE
                } else {
                    conversationAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConversationsActivity, "Error loading conversations: ${e.message}", Toast.LENGTH_SHORT).show()
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
