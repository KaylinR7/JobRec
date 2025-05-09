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
import com.example.jobrec.adapters.ConversationAdapter
import com.example.jobrec.models.Conversation
import com.example.jobrec.repositories.ConversationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private var conversationsListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    
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
        // Stop any existing listener
        conversationsListener?.remove()
        
        // Start new listener for conversations where the user is either the candidate or the company
        val candidateQuery = db.collection("conversations")
            .whereEqualTo("candidateId", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        
        val companyQuery = db.collection("conversations")
            .whereEqualTo("companyId", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        
        // We'll use the candidate query for the listener, but we'll merge results in the snapshot
        conversationsListener = candidateQuery.addSnapshotListener { candidateSnapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Error loading conversations: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            
            // Get company conversations
            companyQuery.get().addOnSuccessListener { companySnapshot ->
                val conversations = mutableListOf<Conversation>()
                
                // Add candidate conversations
                if (candidateSnapshot != null && !candidateSnapshot.isEmpty) {
                    conversations.addAll(candidateSnapshot.toObjects(Conversation::class.java))
                }
                
                // Add company conversations
                if (!companySnapshot.isEmpty) {
                    conversations.addAll(companySnapshot.toObjects(Conversation::class.java))
                }
                
                // Sort by updated time
                val sortedConversations = conversations.sortedByDescending { it.updatedAt.seconds }
                
                // Update UI
                if (sortedConversations.isNotEmpty()) {
                    conversationAdapter.submitList(sortedConversations)
                    emptyView.visibility = View.GONE
                } else {
                    conversationAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                }
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
