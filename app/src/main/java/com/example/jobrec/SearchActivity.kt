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

    // Job Fields - same as in signup
    private val jobFields = arrayOf(
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

        // Initialize views
        jobTypeChipGroup = binding.jobTypeChipGroup

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

        // Setup search button
        setupSearchButton()

        // Setup clear filters button
        setupClearFiltersButton()
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
        // Set click listeners for job type chips
        binding.chipFullTime.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        binding.chipPartTime.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        binding.chipContract.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
        binding.chipRemote.setOnCheckedChangeListener { _, _ ->
            // Optional: Auto-search when chip selection changes
        }
    }

    private fun setupClearFiltersButton() {
        binding.clearFiltersButton.setOnClickListener {
            // Clear all filters
            binding.searchInput.text?.clear()
            binding.jobFieldDropdown.text?.clear()
            binding.jobSpecializationDropdown.text?.clear()
            binding.provinceDropdown.text?.clear()
            binding.salaryRangeDropdown.text?.clear()
            binding.experienceDropdown.text?.clear()

            // Disable specialization dropdown
            binding.jobSpecializationDropdown.isEnabled = false

            // Uncheck all chips
            binding.chipFullTime.isChecked = false
            binding.chipPartTime.isChecked = false
            binding.chipContract.isChecked = false
            binding.chipRemote.isChecked = false

            // Clear results
            jobsAdapter.submitList(emptyList())
            binding.resultsCountText.visibility = View.GONE

            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
        }
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
        // Setup Province dropdown
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        (binding.provinceLayout.editText as? AutoCompleteTextView)?.setAdapter(provinceAdapter)

        // Setup Salary Range dropdown
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryRanges)
        (binding.salaryRangeLayout.editText as? AutoCompleteTextView)?.setAdapter(salaryAdapter)

        // Setup Experience Level dropdown
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        (binding.experienceLayout.editText as? AutoCompleteTextView)?.setAdapter(experienceAdapter)

        // Setup Job Field dropdown
        val jobFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobFields)
        (binding.jobFieldLayout.editText as? AutoCompleteTextView)?.setAdapter(jobFieldAdapter)

        // Initially disable specialization dropdown
        binding.jobSpecializationDropdown.isEnabled = false

        // Setup field selection listener to update specializations
        binding.jobFieldDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedField = jobFields[position]
            updateSpecializationDropdown(selectedField)
        }
    }

    private fun updateSpecializationDropdown(field: String) {
        // Get subcategories for the selected field from FieldCategories
        val subFields = FieldCategories.fields[field] ?: listOf()

        if (subFields.isNotEmpty()) {
            // Create a list with "Any" option first, then all subcategories
            val options = listOf("Any") + subFields

            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
            binding.jobSpecializationDropdown.setAdapter(subFieldAdapter)
            binding.jobSpecializationDropdown.isEnabled = true
            binding.jobSpecializationDropdown.setText("", false) // Clear previous selection
        } else {
            // If no subcategories exist for this field
            binding.jobSpecializationDropdown.setText("")
            binding.jobSpecializationDropdown.isEnabled = false
        }
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        // Show loading state
        binding.searchResultsRecyclerView.visibility = View.GONE

        // Get search parameters
        val searchText = binding.searchInput.text.toString().trim()
        val selectedField = (binding.jobFieldLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedSpecialization = (binding.jobSpecializationLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedProvince = (binding.provinceLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedSalary = (binding.salaryRangeLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedExperience = (binding.experienceLayout.editText as? AutoCompleteTextView)?.text.toString()

        // Get selected job types
        val selectedJobTypes = mutableListOf<String>()
        if (binding.chipFullTime.isChecked) selectedJobTypes.add("Full-time")
        if (binding.chipPartTime.isChecked) selectedJobTypes.add("Part-time")
        if (binding.chipContract.isChecked) selectedJobTypes.add("Contract")
        if (binding.chipRemote.isChecked) selectedJobTypes.add("Remote")

        // Simplified approach: Get all active jobs and filter in memory
        db.collection("jobs")
            .whereEqualTo("status", "active")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Convert documents to Job objects
                val allJobs = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Job::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("SearchActivity", "Error converting document to Job object: ${e.message}")
                        null
                    }
                }

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
}