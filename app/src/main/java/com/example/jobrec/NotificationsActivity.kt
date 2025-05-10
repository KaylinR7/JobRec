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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()
        setupToolbar()
        setupRecyclerView()

        // Load notifications
        loadNotifications()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            },
            onApplyClick = { notification ->
                if (notification.jobId != null) {
                    val intent = Intent(this, JobDetailsActivity::class.java).apply {
                        putExtra("jobId", notification.jobId)
                    }
                    startActivity(intent)
                }
            }
        )

        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationsAdapter
        }
    }

    private fun loadNotifications() {
        val currentUser = auth.currentUser ?: return

        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val notificationsSnapshot = db.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val notifications = notificationsSnapshot.documents.mapNotNull { doc ->
                    try {
                        Notification(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "",
                            jobId = doc.getString("jobId"),
                            timestamp = doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                            read = doc.getBoolean("read") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Update UI
                if (notifications.isNotEmpty()) {
                    notificationsAdapter.submitList(notifications)
                    emptyView.visibility = View.GONE
                    notificationsRecyclerView.visibility = View.VISIBLE
                } else {
                    emptyView.visibility = View.VISIBLE
                    notificationsRecyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Hide loading indicator
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        // Mark notification as read
        db.collection("notifications")
            .document(notification.id)
            .update("read", true)

        // Handle click based on notification type
        when (notification.type) {
            "new_job" -> {
                if (notification.jobId != null) {
                    val intent = Intent(this, JobDetailsActivity::class.java).apply {
                        putExtra("jobId", notification.jobId)
                    }
                    startActivity(intent)
                }
            }
            "application_status" -> {
                startActivity(Intent(this, MyApplicationsActivity::class.java))
            }
            "new_message" -> {
                startActivity(Intent(this, ConversationsActivity::class.java))
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

    data class Notification(
        val id: String,
        val title: String,
        val message: String,
        val type: String,
        val jobId: String? = null,
        val timestamp: com.google.firebase.Timestamp,
        val read: Boolean = false
    )
}