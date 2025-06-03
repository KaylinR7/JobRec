package com.example.jobrec
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jobrec.adapters.PaginatedApplicationsAdapter
import com.example.jobrec.utils.*
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
class UserApplicationsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UserApplicationsActivity"
        private const val CACHE_KEY_PREFIX = "user_applications"
    }

    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var applicationsAdapter: PaginatedApplicationsAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Performance optimization components
    private lateinit var cacheManager: CacheManager
    private lateinit var paginationHelper: PaginationHelper<Application>
    private lateinit var loadingStateManager: PerformanceUtils.LoadingStateManager

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private var currentStatusIndex = 0

    private val statuses = arrayOf("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWING", "OFFERED", "REJECTED")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_applications)

        val timer = PerformanceUtils.startTimer("UserApplicationsActivity onCreate")

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeComponents()
        setupRecyclerView()
        setupSwipeRefresh()
        setupTabs()
        loadApplications(0)

        timer.end()
    }

    private fun initializeComponents() {
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Initialize performance optimization components
        cacheManager = CacheManager.getInstance(this)

        loadingStateManager = PerformanceUtils.LoadingStateManager(
            progressBar = findViewById(R.id.progressBar),
            contentView = applicationsRecyclerView,
            emptyStateView = findViewById(R.id.emptyStateLayout),
            swipeRefreshLayout = swipeRefreshLayout
        )
    }

    private fun setupRecyclerView() {
        applicationsAdapter = PaginatedApplicationsAdapter(
            onApplicationClick = { application -> showApplicationDetails(application) },
            onLoadMore = { loadMoreApplications() }
        )

        val layoutManager = LinearLayoutManager(this)
        applicationsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = applicationsAdapter
        }

        // Optimize RecyclerView performance
        PerformanceUtils.RecyclerViewOptimizer.optimizeRecyclerView(applicationsRecyclerView)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshApplications()
        }
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentStatusIndex = tab.position
                loadApplications(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                refreshApplications()
            }
        })

        statuses.forEach { status ->
            tabLayout.addTab(tabLayout.newTab().setText(status))
        }
    }
    private fun loadApplications(statusIndex: Int) {
        currentStatusIndex = statusIndex
        val status = statuses[statusIndex]
        val cacheKey = "${CACHE_KEY_PREFIX}_${userId}_$status"

        // Initialize pagination helper for this status
        paginationHelper = PaginationHelperFactory.createApplicationsPaginationHelper(
            pageSize = 20,
            cacheKey = cacheKey,
            cacheManager = cacheManager
        )

        loadingStateManager.showLoading()

        lifecycleScope.launch {
            try {
                val timer = PerformanceUtils.startTimer("Applications load - $status")

                val query = buildApplicationsQuery(status)
                val result = paginationHelper.loadFirstPage(query)

                handleApplicationsResult(result, isRefresh = false)
                timer.end()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading applications", e)
                loadingStateManager.showEmpty()
                Toast.makeText(this@UserApplicationsActivity, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMoreApplications() {
        lifecycleScope.launch {
            try {
                applicationsAdapter.setLoadingState(true)

                val status = statuses[currentStatusIndex]
                val query = buildApplicationsQuery(status)
                val result = paginationHelper.loadNextPage(query)

                handleApplicationsResult(result, isRefresh = false)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more applications", e)
                applicationsAdapter.setLoadingState(false)
                Toast.makeText(this@UserApplicationsActivity, "Error loading more applications", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshApplications() {
        lifecycleScope.launch {
            try {
                val timer = PerformanceUtils.startTimer("Applications refresh")

                val status = statuses[currentStatusIndex]
                val query = buildApplicationsQuery(status)
                val result = paginationHelper.refresh(query)

                handleApplicationsResult(result, isRefresh = true)
                timer.end()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error refreshing applications", e)
                loadingStateManager.hideLoading()
                Toast.makeText(this@UserApplicationsActivity, "Error refreshing applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildApplicationsQuery(status: String): Query {
        return db.collection("applications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", status)
            .orderBy("appliedDate", Query.Direction.DESCENDING)
    }

    private fun handleApplicationsResult(result: PaginationHelper.PaginationResult<Application>, isRefresh: Boolean) {
        if (result.isFirstPage || isRefresh) {
            // First page or refresh - replace all items
            applicationsAdapter.submitList(result.items) {
                loadingStateManager.hideLoading()
                if (result.items.isEmpty()) {
                    loadingStateManager.showEmpty()
                } else {
                    loadingStateManager.showContent()
                }
            }
        } else {
            // Additional page - append items
            val currentList = applicationsAdapter.currentList.toMutableList()
            currentList.addAll(result.items)
            applicationsAdapter.submitList(currentList) {
                applicationsAdapter.setLoadingState(false)
            }
        }

        applicationsAdapter.setHasMoreData(result.hasMore)

        android.util.Log.d(TAG, "Loaded ${result.items.size} applications, total: ${result.totalLoaded}, hasMore: ${result.hasMore}")
    }

    private fun showApplicationDetails(application: Application) {
        val dialog = UserApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }

    override fun onResume() {
        super.onResume()
        // Log memory usage for monitoring
        PerformanceUtils.logMemoryUsage(this, TAG)
    }
}