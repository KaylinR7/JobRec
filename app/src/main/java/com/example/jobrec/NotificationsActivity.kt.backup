package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.Menu
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
import com.example.jobrec.repositories.NotificationRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var notificationRepository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Initialize repository
        notificationRepository = NotificationRepository(this)

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
        println("NotificationsActivity: Setting up RecyclerView")

        notificationsAdapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                println("NotificationsActivity: Notification clicked: ${notification.id}")
                handleNotificationClick(notification)
            },
            onApplyClick = { notification ->
                println("NotificationsActivity: Apply clicked for notification: ${notification.id}")
                if (notification.jobId != null) {
                    val intent = Intent(this, JobDetailsActivity::class.java).apply {
                        putExtra("jobId", notification.jobId)
                    }
                    startActivity(intent)
                }
            },
            onDeleteClick = { notification ->
                println("NotificationsActivity: Delete clicked for notification: ${notification.id}")
                deleteNotification(notification)
            }
        )

        val layoutManager = LinearLayoutManager(this@NotificationsActivity)
        notificationsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = notificationsAdapter

            println("NotificationsActivity: RecyclerView adapter set")

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Load more when user scrolls to the end
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && !isLoadingMore
                        && !allNotificationsLoaded) {
                        loadMoreNotifications()
                    }
                }
            })
        }
    }

    private val PAGE_SIZE = 20
    private var lastVisibleNotification: DocumentSnapshot? = null
    private var isLoadingMore = false
    private var allNotificationsLoaded = false
    private val notifications = mutableListOf<NotificationRepository.Notification>()

    private fun loadNotifications() {
        // Reset pagination state
        lastVisibleNotification = null
        allNotificationsLoaded = false
        notifications.clear()

        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true

        // First fix any invalid timestamps and clean up duplicates
        lifecycleScope.launch {
            try {
                // Fix invalid timestamps
                notificationRepository.fixNotificationTimestamps()

                // Clean up duplicate notifications
                notificationRepository.cleanupDuplicateNotifications()

                // Load first page
                loadMoreNotifications(true)
            } catch (e: Exception) {
                // Continue with loading notifications even if cleanup fails
                loadMoreNotifications(true)
            }
        }
    }

    private fun loadMoreNotifications(isFirstLoad: Boolean = false) {
        if (isLoadingMore) {
            return
        }

        if (allNotificationsLoaded && !isFirstLoad) {
            return
        }

        isLoadingMore = true

        lifecycleScope.launch {
            try {
                // Get notifications using the repository
                val result = notificationRepository.getNotifications(
                    pageSize = PAGE_SIZE,
                    lastDocument = if (isFirstLoad) null else lastVisibleNotification
                )

                val newNotifications = result.first
                lastVisibleNotification = result.second

                // Update pagination state
                if (newNotifications.size < PAGE_SIZE) {
                    allNotificationsLoaded = true
                }

                // Update the list
                if (isFirstLoad) {
                    notifications.clear()
                }

                // Add only notifications that aren't already in the list (by content)
                val newUniqueNotifications = newNotifications.filter { newNotification ->
                    !notifications.any {
                        it.title == newNotification.title &&
                        it.message == newNotification.message &&
                        it.jobId == newNotification.jobId
                    }
                }

                notifications.addAll(newUniqueNotifications)

                // Deduplicate the entire list again to be sure
                val uniqueNotifications = notifications
                    .groupBy { Triple(it.title, it.message, it.jobId) }
                    .map { (_, group) -> group.maxByOrNull { it.timestamp.seconds } ?: group.first() }

                notifications.clear()
                notifications.addAll(uniqueNotifications)



                // Update UI
                if (notifications.isNotEmpty()) {
                    // Create a new list to ensure DiffUtil detects changes
                    // Sort by timestamp and deduplicate by content
                    val finalNotifications = notifications
                        .sortedByDescending { it.timestamp.seconds }
                        .distinctBy { Triple(it.title, it.message, it.jobId) } // Ensure no duplicates by content

                    notificationsAdapter.submitList(ArrayList(finalNotifications))
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
                isLoadingMore = false
            }
        }
    }

    private fun handleNotificationClick(notification: NotificationRepository.Notification) {
        // Mark notification as read
        lifecycleScope.launch {
            try {
                notificationRepository.markAsRead(notification.id)
            } catch (e: Exception) {
                // Ignore errors
            }
        }

        // Handle click based on notification type
        when (notification.type) {
            NotificationRepository.TYPE_NEW_JOB -> {
                if (notification.jobId != null) {
                    val intent = Intent(this, JobDetailsActivity::class.java).apply {
                        putExtra("jobId", notification.jobId)
                    }
                    startActivity(intent)
                }
            }
            NotificationRepository.TYPE_APPLICATION_STATUS -> {
                startActivity(Intent(this, MyApplicationsActivity::class.java))
            }
            NotificationRepository.TYPE_NEW_MESSAGE -> {
                startActivity(Intent(this, ConversationsActivity::class.java))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_delete_test_notifications -> {
                deleteAllTestNotifications()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteNotification(notification: NotificationRepository.Notification) {
        lifecycleScope.launch {
            try {
                // Delete the notification from Firestore
                notificationRepository.deleteNotification(notification.id)

                // Remove from our local list
                val currentList = notifications.toMutableList()
                currentList.removeIf { it.id == notification.id }

                // Update the adapter with the new list
                notifications.clear()
                notifications.addAll(currentList)

                val finalNotifications = notifications
                    .sortedByDescending { it.timestamp.seconds }
                    .distinctBy { Triple(it.title, it.message, it.jobId) }

                notificationsAdapter.submitList(ArrayList(finalNotifications))

                // Show empty view if needed
                if (notifications.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    notificationsRecyclerView.visibility = View.GONE
                }

                Toast.makeText(this@NotificationsActivity, "Notification deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error deleting notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add a method to delete all test notifications
    private fun deleteAllTestNotifications() {
        lifecycleScope.launch {
            try {
                val testNotifications = notifications.filter { notification ->
                    notification.title.contains("Test", ignoreCase = true) ||
                    notification.message.contains("Test", ignoreCase = true) ||
                    notification.message.isEmpty() ||
                    notification.message.isBlank() ||
                    notification.title.contains("test notification", ignoreCase = true)
                }

                if (testNotifications.isEmpty()) {
                    Toast.makeText(this@NotificationsActivity, "No test notifications found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show loading indicator
                swipeRefreshLayout.isRefreshing = true

                for (notification in testNotifications) {
                    notificationRepository.deleteNotification(notification.id)
                }

                // Reload notifications after deletion
                loadNotifications()

                Toast.makeText(
                    this@NotificationsActivity,
                    "${testNotifications.size} test notifications deleted",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error deleting test notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
}