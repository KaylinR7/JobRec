package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivitySearchBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.jobrec.models.FieldCategories

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var jobsAdapter: RecentJobsAdapter

    private lateinit var jobTypeChipGroup: ChipGroup
    private lateinit var noResultsView: View
    private lateinit var drawerLayout: DrawerLayout

    // South African provinces
    private val provinces = arrayOf(
        "Eastern Cape",
        "Free State",
        "Gauteng",
        "KwaZulu-Natal",
        "Limpopo",
        "Mpumalanga",
        "Northern Cape",
        "North West",
        "Western Cape"
    )

    // Salary ranges - same as in signup
    private val salaryRanges = arrayOf(
        "R0 - R10,000",
        "R10,000 - R20,000",
        "R20,000 - R30,000",
        "R30,000 - R40,000",
        "R40,000 - R50,000",
        "R50,000+"
    )

    // Experience levels - same as in signup
    private val experienceLevels = arrayOf(
        "0-1 years",
        "1-3 years",
        "3-5 years",
        "5-10 years",
        "10+ years"
    )

    // Job Fields - get directly from FieldCategories to ensure consistency
    private val jobFields = FieldCategories.fields.keys.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search Jobs"

        // Initialize drawer layout
        drawerLayout = binding.drawerLayout

        // Initialize views
        jobTypeChipGroup = findViewById(R.id.jobTypeChipGroup)

        // Initialize "no results" view
        noResultsView = layoutInflater.inflate(R.layout.layout_no_search_results, binding.root, false)

        // Initialize RecyclerView
        setupRecyclerView()

        // Setup dropdowns
        setupDropdowns()

        // Setup search input
        setupSearchInput()

        // Setup job type chips
        setupJobTypeChips()

        // Setup filter button
        setupFilterButton()

        // Setup search FAB
        setupSearchFab()

        // Setup apply filters button
        findViewById<View>(R.id.applyFiltersButton).setOnClickListener {
            // Close drawer and perform search
            drawerLayout.closeDrawer(GravityCompat.END)
            performSearch()
        }

        // Setup clear filters button
        findViewById<View>(R.id.clearFiltersButton).setOnClickListener {
            clearFilters()
        }
    }

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            // Open the drawer from the right side
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupSearchFab() {
        binding.searchFab.setOnClickListener {
            performSearch()
        }
    }

    private fun setupSearchInput() {
        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Optional: Auto-search as user types (with debounce)
                // You could add a delay here to avoid too many searches
            }
        })
    }

    private fun setupJobTypeChips() {
        // Get chip references from the drawer layout
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)

        // Set click listeners for job type chips
        chipFullTime.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        chipPartTime.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        chipContract.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        chipRemote.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
    }

    private fun clearFilters() {
        // Clear all filters
        binding.searchInput.text?.clear()

        // Clear drawer dropdowns
        val jobFieldDropdown = findViewById<AutoCompleteTextView>(R.id.jobFieldDropdown)
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)
        val provinceDropdown = findViewById<AutoCompleteTextView>(R.id.provinceDropdown)
        val salaryRangeDropdown = findViewById<AutoCompleteTextView>(R.id.salaryRangeDropdown)
        val experienceDropdown = findViewById<AutoCompleteTextView>(R.id.experienceDropdown)

        jobFieldDropdown.text?.clear()
        jobSpecializationDropdown.text?.clear()
        provinceDropdown.text?.clear()
        salaryRangeDropdown.text?.clear()
        experienceDropdown.text?.clear()

        // Disable specialization dropdown
        jobSpecializationDropdown.isEnabled = false

        // Uncheck all chips
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)

        chipFullTime.isChecked = false
        chipPartTime.isChecked = false
        chipContract.isChecked = false
        chipRemote.isChecked = false

        // Clear results
        jobsAdapter.submitList(emptyList())
        binding.resultsCountText.visibility = View.GONE

        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        jobsAdapter = RecentJobsAdapter { job ->
            // Handle job click
            val intent = android.content.Intent(this, JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = jobsAdapter
        }
    }

    private fun setupDropdowns() {
        // Get drawer dropdowns
        val provinceDropdown = findViewById<AutoCompleteTextView>(R.id.provinceDropdown)
        val salaryRangeDropdown = findViewById<AutoCompleteTextView>(R.id.salaryRangeDropdown)
        val experienceDropdown = findViewById<AutoCompleteTextView>(R.id.experienceDropdown)
        val jobFieldDropdown = findViewById<AutoCompleteTextView>(R.id.jobFieldDropdown)
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)

        // Setup Province dropdown
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceDropdown.setAdapter(provinceAdapter)

        // Setup Salary Range dropdown
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryRanges)
        salaryRangeDropdown.setAdapter(salaryAdapter)

        // Setup Experience Level dropdown
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        experienceDropdown.setAdapter(experienceAdapter)

        // Setup Job Field dropdown
        val jobFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobFields)
        jobFieldDropdown.setAdapter(jobFieldAdapter)

        // Initially disable specialization dropdown
        jobSpecializationDropdown.isEnabled = false

        // Setup field selection listener to update specializations
        jobFieldDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedField = jobFields[position]
            updateSpecializationDropdown(selectedField)
        }
    }

    private fun updateSpecializationDropdown(field: String) {
        // Get subcategories for the selected field from FieldCategories
        val subFields = FieldCategories.fields[field] ?: listOf()
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)

        if (subFields.isNotEmpty()) {
            // Create a list with "Any" option first, then all subcategories
            val options = listOf("Any") + subFields

            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
            jobSpecializationDropdown.setAdapter(subFieldAdapter)
            jobSpecializationDropdown.isEnabled = true
            jobSpecializationDropdown.setText("", false) // Clear previous selection
        } else {
            // If no subcategories exist for this field
            jobSpecializationDropdown.setText("")
            jobSpecializationDropdown.isEnabled = false
        }
    }

    // We don't need this method anymore as we're using the search FAB
    // and the apply filters button in the drawer

    private fun performSearch() {
        // Show loading state
        binding.searchResultsRecyclerView.visibility = View.GONE

        // Get search parameters
        val searchText = binding.searchInput.text.toString().trim()

        // Get drawer filter values
        val jobFieldDropdown = findViewById<AutoCompleteTextView>(R.id.jobFieldDropdown)
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)
        val provinceDropdown = findViewById<AutoCompleteTextView>(R.id.provinceDropdown)
        val salaryRangeDropdown = findViewById<AutoCompleteTextView>(R.id.salaryRangeDropdown)
        val experienceDropdown = findViewById<AutoCompleteTextView>(R.id.experienceDropdown)

        val selectedField = jobFieldDropdown.text.toString()
        val selectedSpecialization = jobSpecializationDropdown.text.toString()
        val selectedProvince = provinceDropdown.text.toString()
        val selectedSalary = salaryRangeDropdown.text.toString()
        val selectedExperience = experienceDropdown.text.toString()

        // Get selected job types
        val selectedJobTypes = mutableListOf<String>()
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)

        if (chipFullTime.isChecked) selectedJobTypes.add("Full-time")
        if (chipPartTime.isChecked) selectedJobTypes.add("Part-time")
        if (chipContract.isChecked) selectedJobTypes.add("Contract")
        if (chipRemote.isChecked) selectedJobTypes.add("Remote")

        // Even simpler approach: Get all jobs and filter status in memory
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                // Convert documents to Job objects and filter active jobs
                val allJobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java).copy(id = doc.id)
                        // Only include active jobs
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        Log.e("SearchActivity", "Error converting document to Job object: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } // Sort in memory

                // Apply all filters in memory
                val filteredJobs = allJobs.filter { job ->
                    var matches = true

                    // Filter by job field
                    if (selectedField.isNotEmpty() && selectedField != "Any") {
                        matches = matches && job.jobField.equals(selectedField, ignoreCase = true)

                        // Filter by specialization (only if field matches)
                        if (matches && selectedSpecialization.isNotEmpty() && selectedSpecialization != "Any") {
                            matches = matches && job.specialization.equals(selectedSpecialization, ignoreCase = true)
                        }
                    }

                    // Filter by province
                    if (selectedProvince.isNotEmpty() && selectedProvince != "Any") {
                        matches = matches && job.province.equals(selectedProvince, ignoreCase = true)
                    }

                    // Filter by experience level
                    if (selectedExperience.isNotEmpty() && selectedExperience != "Any") {
                        matches = matches && job.experienceLevel.equals(selectedExperience, ignoreCase = true)
                    }

                    // Filter by salary range
                    if (selectedSalary.isNotEmpty() && selectedSalary != "Any") {
                        matches = matches && isJobInSalaryRange(job.salary, selectedSalary)
                    }

                    // Filter by job type
                    if (selectedJobTypes.isNotEmpty()) {
                        matches = matches && selectedJobTypes.contains(job.type)
                    }

                    // Filter by search text
                    if (searchText.isNotEmpty()) {
                        matches = matches && (
                            job.title.contains(searchText, ignoreCase = true) ||
                            job.description.contains(searchText, ignoreCase = true) ||
                            job.companyName.contains(searchText, ignoreCase = true)
                        )
                    }

                    matches
                }

                // Update UI with filtered results
                updateSearchResults(filteredJobs)
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error searching jobs: ${e.message}")
                Toast.makeText(this, "Error searching jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.searchResultsRecyclerView.visibility = View.VISIBLE
            }
    }

    private fun updateSearchResults(jobs: List<Job>) {
        // Show results count
        binding.resultsCountText.text = "${jobs.size} jobs found"
        binding.resultsCountText.visibility = View.VISIBLE

        // Update adapter
        jobsAdapter.submitList(jobs)

        // Show appropriate view based on results
        if (jobs.isEmpty()) {
            // Show no results view
            if (binding.searchResultsRecyclerView.parent == binding.root) {
                (binding.searchResultsRecyclerView.parent as ViewGroup).addView(noResultsView)
            }
            binding.searchResultsRecyclerView.visibility = View.GONE
        } else {
            // Show results
            if (noResultsView.parent != null) {
                (noResultsView.parent as ViewGroup).removeView(noResultsView)
            }
            binding.searchResultsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun isJobInSalaryRange(jobSalary: String, selectedRange: String): Boolean {
        // Convert salary strings to numbers and compare
        val jobSalaryNum = jobSalary.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

        // Handle "R50,000+" case
        if (selectedRange.contains("+")) {
            val minSalary = selectedRange.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            return jobSalaryNum >= minSalary
        }

        // Handle normal range case "R10,000 - R20,000"
        val rangeParts = selectedRange.split("-")
        if (rangeParts.size == 2) {
            val minSalary = rangeParts[0].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            val maxSalary = rangeParts[1].replace(Regex("[^0-9]"), "").toIntOrNull() ?: Int.MAX_VALUE
            return jobSalaryNum in minSalary..maxSalary
        }

        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        // Close drawer if open, otherwise go back
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}