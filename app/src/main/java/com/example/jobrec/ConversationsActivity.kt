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
    private lateinit var searchBarCard: androidx.cardview.widget.CardView
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var conversationsListener: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var isLoading = false
    private var isCompanyUser = false
    private var allConversations: List<Conversation> = emptyList()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInterval = 5000L 
    private val fixInterval = 30000L 
    private var lastFixTime = 0L
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isLoading) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFixTime > fixInterval) {
                    android.util.Log.d("ConversationsActivity", "Periodic conversation fixing")
                    fixConversations()
                    lastFixTime = currentTime
                } else {
                    loadConversations()
                    android.util.Log.d("ConversationsActivity", "Auto-refreshing conversations")
                }
            }
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        conversationRepository = ConversationRepository()
        checkUserType()
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        lifecycleScope.launch {
            fixConversations()
        }
        lastFixTime = System.currentTimeMillis()
        startConversationsListener()
    }
    private fun checkUserType() {
        android.util.Log.d("ConversationsActivity", "Checking user type for userId: $currentUserId")
        val currentUserEmail = auth.currentUser?.email
        if (currentUserEmail != null) {
            db.collection("companies")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        isCompanyUser = true
                        val companyDoc = documents.documents[0]
                        val companyId = companyDoc.id
                        val companyName = companyDoc.getString("companyName") ?: "unknown"
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
                        db.collection("companies")
                            .whereEqualTo("userId", currentUserId)
                            .get()
                            .addOnSuccessListener { userIdDocs ->
                                if (!userIdDocs.isEmpty) {
                                    isCompanyUser = true
                                    val companyDoc = userIdDocs.documents[0]
                                    val companyId = companyDoc.id
                                    val companyName = companyDoc.getString("companyName") ?: "unknown"
                                    android.util.Log.d("ConversationsActivity", "User is company (by userId): true, companyId: $companyId, name: $companyName")
                                } else {
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
        searchBarCard = findViewById(R.id.searchBarCard)
        val layout = findViewById<LinearLayout>(R.id.buttonsLayout)
        layout.visibility = View.GONE
        swipeRefreshLayout.setOnRefreshListener {
            fixConversations() 
        }
        fixConversationsButton.visibility = View.GONE
        fixConversationsButton.setOnClickListener {
            fixConversations()
        }
        setupSearchBar()
    }
    private fun setupSearchBar() {
        searchBarCard.setOnClickListener {
            val searchDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Search Conversations")
                .setView(R.layout.dialog_search)
                .create()
            searchDialog.show()
            val searchEditText = searchDialog.findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.searchEditText)
            val searchButton = searchDialog.findViewById<Button>(R.id.searchButton)
            searchButton?.setOnClickListener {
                val query = searchEditText?.text.toString().trim().lowercase()
                if (query.isNotEmpty()) {
                    searchConversations(query)
                    searchDialog.dismiss()
                }
            }
        }
    }
    private fun searchConversations(query: String) {
        android.util.Log.d("ConversationsActivity", "Searching conversations with query: $query")
        if (allConversations.isEmpty()) {
            Toast.makeText(this, "No conversations to search", Toast.LENGTH_SHORT).show()
            return
        }
        val filteredConversations = allConversations.filter { conversation ->
            if (isCompanyUser) {
                conversation.candidateName.lowercase().contains(query) ||
                conversation.jobTitle.lowercase().contains(query) ||
                conversation.candidateInfo?.lowercase()?.contains(query) ?: false
            }
            else {
                conversation.companyName.lowercase().contains(query) ||
                conversation.jobTitle.lowercase().contains(query)
            }
        }
        if (filteredConversations.isNotEmpty()) {
            conversationAdapter.submitList(filteredConversations)
            emptyView.visibility = View.GONE
            Toast.makeText(this, "Found ${filteredConversations.size} conversations", Toast.LENGTH_SHORT).show()
        } else {
            emptyView.text = "No conversations found matching '$query'"
            emptyView.visibility = View.VISIBLE
            conversationAdapter.submitList(emptyList())
        }
    }
    private fun fixConversations() {
        android.util.Log.d("ConversationsActivity", "Fixing conversations with incorrect names")
        if (!::conversationAdapter.isInitialized) {
            android.util.Log.d("ConversationsActivity", "Adapter not initialized yet, setting up RecyclerView first")
            setupRecyclerView()
        }
        swipeRefreshLayout.isRefreshing = true
        conversationAdapter.fixAllConversations { fixedCount ->
            android.util.Log.d("ConversationsActivity", "Fixed $fixedCount conversations with incorrect names")
            loadConversations()
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messages"
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
    }
    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("conversationId", conversation.id)
            }
            startActivity(intent)
        }
        conversationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConversationsActivity)
            adapter = conversationAdapter
        }
        conversationAdapter.setUserType(isCompanyUser)
        android.util.Log.d("ConversationsActivity", "RecyclerView setup - isCompanyUser: $isCompanyUser")
    }
    private fun startConversationsListener() {
    }
    private fun loadConversations() {
        if (isLoading) return
        isLoading = true
        if (!::conversationAdapter.isInitialized) {
            android.util.Log.d("ConversationsActivity", "Adapter not initialized yet, setting up RecyclerView first")
            setupRecyclerView()
        }
        swipeRefreshLayout.isRefreshing = true
        conversationsListener?.remove()
        val userType = if (isCompanyUser) "COMPANY" else "STUDENT"
        android.util.Log.d("ConversationsActivity", "Loading conversations for $userType user: $currentUserId")
        conversationAdapter.setUserType(isCompanyUser)
        lifecycleScope.launch {
            try {
                val conversations = conversationRepository.getUserConversations(currentUserId)
                android.util.Log.d("ConversationsActivity", "User ID: $currentUserId")
                android.util.Log.d("ConversationsActivity", "Found ${conversations.size} conversations")
                conversations.forEach { conversation ->
                    val displayName = if (isCompanyUser) {
                        "Student: ${conversation.candidateName}"
                    } else {
                        "Company: ${conversation.companyName}"
                    }
                    android.util.Log.d("ConversationsActivity", "Conversation: ${conversation.id}, " +
                            "Display: $displayName, " +
                            "Job: ${conversation.jobTitle}")
                }
                allConversations = conversations
                val fixedConversations = conversations.map { conversation ->
                    if (conversation.candidateName == "Nasty juice" ||
                        conversation.candidateName == "nasty juice" ||
                        conversation.candidateName == "Company" ||
                        conversation.candidateName == "!!!!!") {
                        val correctName = if (conversation.candidateId == "80f3f99f-3a64-4e8b-a59a-05ba116bff26") {
                            "Shaylin Bhima"
                        } else {
                            "Student"
                        }
                        android.util.Log.d("ConversationsActivity", "Fixing conversation ${conversation.id}: changing candidateName from '${conversation.candidateName}' to '$correctName'")
                        try {
                            db.collection("conversations")
                                .document(conversation.id)
                                .update("candidateName", correctName)
                                .addOnSuccessListener {
                                    android.util.Log.d("ConversationsActivity", "Updated conversation ${conversation.id} with correct name")
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("ConversationsActivity", "Failed to update conversation", e)
                                }
                        } catch (e: Exception) {
                            android.util.Log.e("ConversationsActivity", "Error updating conversation", e)
                        }
                        conversation.copy(candidateName = correctName)
                    }
                    else if (conversation.companyName.isBlank() ||
                             conversation.companyName == "unknown" ||
                             conversation.companyName == "Student") {
                        val correctName = if (conversation.jobTitle.contains(" at ")) {
                            val parts = conversation.jobTitle.split(" at ", limit = 2)
                            parts[1].trim()
                        } else {
                            "Company"
                        }
                        android.util.Log.d("ConversationsActivity", "Fixing conversation ${conversation.id}: changing companyName from '${conversation.companyName}' to '$correctName'")
                        try {
                            db.collection("conversations")
                                .document(conversation.id)
                                .update("companyName", correctName)
                                .addOnSuccessListener {
                                    android.util.Log.d("ConversationsActivity", "Updated conversation ${conversation.id} with correct company name")
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("ConversationsActivity", "Failed to update conversation company name", e)
                                }
                        } catch (e: Exception) {
                            android.util.Log.e("ConversationsActivity", "Error updating conversation company name", e)
                        }
                        conversation.copy(companyName = correctName)
                    } else {
                        conversation
                    }
                }
                if (fixedConversations.isNotEmpty()) {
                    conversationAdapter.submitList(fixedConversations)
                    emptyView.visibility = View.GONE
                    emptyView.text = "No conversations yet"
                } else {
                    conversationAdapter.submitList(emptyList())
                    emptyView.visibility = View.VISIBLE
                    android.util.Log.d("ConversationsActivity", "No conversations found. User type: $userType, userId: $currentUserId")
                    val allConversations = db.collection("conversations")
                        .get()
                        .await()
                    android.util.Log.d("ConversationsActivity", "Total conversations in database: ${allConversations.size()}")
                    if (allConversations.isEmpty) {
                        android.util.Log.d("ConversationsActivity", "No conversations exist in the database")
                    } else {
                        android.util.Log.d("ConversationsActivity", "Conversations exist but none match this user")
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
                conversationAdapter.submitList(emptyList())
                emptyView.visibility = View.VISIBLE
                fixConversationsButton.visibility = View.VISIBLE
            } finally {
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
        refreshHandler.postDelayed(refreshRunnable, refreshInterval)
        loadConversations()
    }
    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
    override fun onDestroy() {
        super.onDestroy()
        conversationsListener?.remove()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}
