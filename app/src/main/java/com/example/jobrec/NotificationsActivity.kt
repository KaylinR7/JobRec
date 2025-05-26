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
        notificationRepository = NotificationRepository(this)
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
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
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
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
        lastVisibleNotification = null
        allNotificationsLoaded = false
        notifications.clear()
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                notificationRepository.fixNotificationTimestamps()
                notificationRepository.cleanupDuplicateNotifications()
                loadMoreNotifications(true)
            } catch (e: Exception) {
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
                val result = notificationRepository.getNotifications(
                    pageSize = PAGE_SIZE,
                    lastDocument = if (isFirstLoad) null else lastVisibleNotification
                )
                val newNotifications = result.first
                lastVisibleNotification = result.second
                if (newNotifications.size < PAGE_SIZE) {
                    allNotificationsLoaded = true
                }
                if (isFirstLoad) {
                    notifications.clear()
                }
                val newUniqueNotifications = newNotifications.filter { newNotification ->
                    !notifications.any {
                        it.title == newNotification.title &&
                        it.message == newNotification.message &&
                        it.jobId == newNotification.jobId
                    }
                }
                notifications.addAll(newUniqueNotifications)
                val uniqueNotifications = notifications
                    .groupBy { Triple(it.title, it.message, it.jobId) }
                    .map { (_, group) -> group.maxByOrNull { it.timestamp.seconds } ?: group.first() }
                notifications.clear()
                notifications.addAll(uniqueNotifications)
                if (notifications.isNotEmpty()) {
                    val finalNotifications = notifications
                        .sortedByDescending { it.timestamp.seconds }
                        .distinctBy { Triple(it.title, it.message, it.jobId) } 
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
                swipeRefreshLayout.isRefreshing = false
                isLoadingMore = false
            }
        }
    }
    private fun handleNotificationClick(notification: NotificationRepository.Notification) {
        lifecycleScope.launch {
            try {
                notificationRepository.markAsRead(notification.id)
            } catch (e: Exception) {
            }
        }
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
                notificationRepository.deleteNotification(notification.id)
                val currentList = notifications.toMutableList()
                currentList.removeIf { it.id == notification.id }
                notifications.clear()
                notifications.addAll(currentList)
                val finalNotifications = notifications
                    .sortedByDescending { it.timestamp.seconds }
                    .distinctBy { Triple(it.title, it.message, it.jobId) }
                notificationsAdapter.submitList(ArrayList(finalNotifications))
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
                swipeRefreshLayout.isRefreshing = true
                for (notification in testNotifications) {
                    notificationRepository.deleteNotification(notification.id)
                }
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