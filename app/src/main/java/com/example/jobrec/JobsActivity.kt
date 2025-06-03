package com.example.jobrec
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.jobrec.adapters.PaginatedJobsAdapter
import com.example.jobrec.utils.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.ArrayAdapter
import kotlinx.coroutines.launch
class JobsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "JobsActivity"
        private const val CACHE_KEY_ALL_JOBS = "all_jobs"
        private const val CACHE_KEY_FILTERED_JOBS = "filtered_jobs"
    }

    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var jobsAdapter: PaginatedJobsAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fieldFilterInput: MaterialAutoCompleteTextView

    // Performance optimization components
    private lateinit var cacheManager: CacheManager
    private lateinit var paginationHelper: PaginationHelper<Job>
    private lateinit var loadingStateManager: PerformanceUtils.LoadingStateManager
    private lateinit var searchDebouncer: PerformanceUtils.Debouncer

    private val db = FirebaseFirestore.getInstance()
    private var currentSearchQuery = ""
    private var currentFieldFilter = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)

        val timer = PerformanceUtils.startTimer("JobsActivity onCreate")

        initializeComponents()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        setupFieldFilter()
        loadJobs()

        timer.end()
    }

    private fun initializeComponents() {
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        fieldFilterInput = findViewById(R.id.fieldFilterInput)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Initialize performance optimization components
        cacheManager = CacheManager.getInstance(this)
        searchDebouncer = PerformanceUtils.Debouncer(300L, lifecycleScope)

        loadingStateManager = PerformanceUtils.LoadingStateManager(
            progressBar = findViewById(R.id.progressBar),
            contentView = jobsRecyclerView,
            emptyStateView = findViewById(R.id.emptyStateLayout),
            swipeRefreshLayout = swipeRefreshLayout
        )

        paginationHelper = PaginationHelperFactory.createJobsPaginationHelper(
            pageSize = 20,
            cacheKey = CACHE_KEY_ALL_JOBS,
            cacheManager = cacheManager
        )
    }

    private fun setupRecyclerView() {
        jobsAdapter = PaginatedJobsAdapter(
            onJobClick = { job -> openJobDetails(job) },
            onLoadMore = { loadMoreJobs() }
        )

        val layoutManager = LinearLayoutManager(this)
        jobsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = jobsAdapter
        }

        // Optimize RecyclerView performance
        PerformanceUtils.RecyclerViewOptimizer.optimizeRecyclerView(jobsRecyclerView)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshJobs()
        }
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchDebouncer.debounce {
                    performSearch()
                }
            }
        })

        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            performSearch()
        }
    }
    private fun setupFieldFilter() {
        val fieldOptions = arrayOf(
            "Information Technology",
            "Healthcare",
            "Law",
            "Education",
            "Engineering",
            "Business",
            "Finance",
            "Marketing",
            "Sales",
            "Customer Service",
            "Manufacturing",
            "Construction",
            "Transportation",
            "Hospitality",
            "Other"
        )
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldFilterInput.setAdapter(fieldAdapter)
        fieldFilterInput.setOnItemClickListener { _, _, position, _ ->
            performSearch()
        }
    }
    private fun loadJobs() {
        loadingStateManager.showLoading()

        lifecycleScope.launch {
            try {
                val timer = PerformanceUtils.startTimer("Initial jobs load")

                val query = buildJobsQuery()
                val result = paginationHelper.loadFirstPage(query)

                handleJobsResult(result, isRefresh = false)
                timer.end()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading initial data", e)
                loadingStateManager.showEmpty()
                Toast.makeText(this@JobsActivity, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMoreJobs() {
        lifecycleScope.launch {
            try {
                jobsAdapter.setLoadingState(true)

                val query = buildJobsQuery()
                val result = paginationHelper.loadNextPage(query)

                handleJobsResult(result, isRefresh = false)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more jobs", e)
                jobsAdapter.setLoadingState(false)
                Toast.makeText(this@JobsActivity, "Error loading more jobs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshJobs() {
        lifecycleScope.launch {
            try {
                val timer = PerformanceUtils.startTimer("Jobs refresh")

                val query = buildJobsQuery()
                val result = paginationHelper.refresh(query)

                handleJobsResult(result, isRefresh = true)
                timer.end()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error refreshing jobs", e)
                loadingStateManager.hideLoading()
                Toast.makeText(this@JobsActivity, "Error refreshing jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch() {
        val searchQuery = searchEditText.text.toString().trim()
        val fieldFilter = fieldFilterInput.text.toString().trim()

        if (searchQuery == currentSearchQuery && fieldFilter == currentFieldFilter) {
            return // No change in search criteria
        }

        currentSearchQuery = searchQuery
        currentFieldFilter = fieldFilter

        // Reset pagination for new search
        paginationHelper.reset()

        lifecycleScope.launch {
            try {
                loadingStateManager.showLoading()

                val query = buildJobsQuery()
                val result = paginationHelper.loadFirstPage(query)

                handleJobsResult(result, isRefresh = true)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error performing search", e)
                loadingStateManager.showEmpty()
                Toast.makeText(this@JobsActivity, "Error searching jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildJobsQuery(): Query {
        return db.collection("jobs")
            .whereEqualTo("status", "active")
            .orderBy("postedDate", Query.Direction.DESCENDING)
    }

    private fun handleJobsResult(result: PaginationHelper.PaginationResult<Job>, isRefresh: Boolean) {
        if (result.isFirstPage || isRefresh) {
            // First page or refresh - replace all items
            jobsAdapter.submitList(result.items) {
                loadingStateManager.hideLoading()
                if (result.items.isEmpty()) {
                    loadingStateManager.showEmpty()
                } else {
                    loadingStateManager.showContent()
                }
            }
        } else {
            // Additional page - append items
            val currentList = jobsAdapter.currentList.toMutableList()
            currentList.addAll(result.items)
            jobsAdapter.submitList(currentList) {
                jobsAdapter.setLoadingState(false)
            }
        }

        jobsAdapter.setHasMoreData(result.hasMore)

        android.util.Log.d(TAG, "Loaded ${result.items.size} jobs, total: ${result.totalLoaded}, hasMore: ${result.hasMore}")
    }

    private fun openJobDetails(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("jobId", job.id)
            putExtra("job", job)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchDebouncer.cancel()
    }

    override fun onResume() {
        super.onResume()
        // Log memory usage for monitoring
        PerformanceUtils.logMemoryUsage(this, TAG)
    }
}