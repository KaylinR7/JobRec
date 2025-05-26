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
    private val salaryRanges = arrayOf(
        "R0 - R10,000",
        "R10,000 - R20,000",
        "R20,000 - R30,000",
        "R30,000 - R40,000",
        "R40,000 - R50,000",
        "R50,000+"
    )
    private val experienceLevels = arrayOf(
        "0-1 years",
        "1-3 years",
        "3-5 years",
        "5-10 years",
        "10+ years"
    )
    private val jobFields = FieldCategories.fields.keys.toTypedArray()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search Jobs"
        drawerLayout = binding.drawerLayout
        jobTypeChipGroup = findViewById(R.id.jobTypeChipGroup)
        noResultsView = layoutInflater.inflate(R.layout.layout_no_search_results, binding.root, false)
        setupRecyclerView()
        setupDropdowns()
        setupSearchInput()
        setupJobTypeChips()
        setupFilterButton()
        setupSearchFab()
        findViewById<View>(R.id.applyFiltersButton).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            performSearch()
        }
        findViewById<View>(R.id.clearFiltersButton).setOnClickListener {
            clearFilters()
        }
    }
    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
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
            }
        })
    }
    private fun setupJobTypeChips() {
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)
        chipFullTime.setOnCheckedChangeListener { _, _ ->
        }
        chipPartTime.setOnCheckedChangeListener { _, _ ->
        }
        chipContract.setOnCheckedChangeListener { _, _ ->
        }
        chipRemote.setOnCheckedChangeListener { _, _ ->
        }
    }
    private fun clearFilters() {
        binding.searchInput.text?.clear()
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
        jobSpecializationDropdown.isEnabled = false
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)
        chipFullTime.isChecked = false
        chipPartTime.isChecked = false
        chipContract.isChecked = false
        chipRemote.isChecked = false
        jobsAdapter.submitList(emptyList())
        binding.resultsCountText.visibility = View.GONE
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }
    private fun setupRecyclerView() {
        jobsAdapter = RecentJobsAdapter { job ->
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
        val provinceDropdown = findViewById<AutoCompleteTextView>(R.id.provinceDropdown)
        val salaryRangeDropdown = findViewById<AutoCompleteTextView>(R.id.salaryRangeDropdown)
        val experienceDropdown = findViewById<AutoCompleteTextView>(R.id.experienceDropdown)
        val jobFieldDropdown = findViewById<AutoCompleteTextView>(R.id.jobFieldDropdown)
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceDropdown.setAdapter(provinceAdapter)
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryRanges)
        salaryRangeDropdown.setAdapter(salaryAdapter)
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        experienceDropdown.setAdapter(experienceAdapter)
        val jobFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobFields)
        jobFieldDropdown.setAdapter(jobFieldAdapter)
        jobSpecializationDropdown.isEnabled = false
        jobFieldDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedField = jobFields[position]
            updateSpecializationDropdown(selectedField)
        }
    }
    private fun updateSpecializationDropdown(field: String) {
        val subFields = FieldCategories.fields[field] ?: listOf()
        val jobSpecializationDropdown = findViewById<AutoCompleteTextView>(R.id.jobSpecializationDropdown)
        if (subFields.isNotEmpty()) {
            val options = listOf("Any") + subFields
            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
            jobSpecializationDropdown.setAdapter(subFieldAdapter)
            jobSpecializationDropdown.isEnabled = true
            jobSpecializationDropdown.setText("", false) 
        } else {
            jobSpecializationDropdown.setText("")
            jobSpecializationDropdown.isEnabled = false
        }
    }
    private fun performSearch() {
        binding.searchResultsRecyclerView.visibility = View.GONE
        val searchText = binding.searchInput.text.toString().trim()
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
        val selectedJobTypes = mutableListOf<String>()
        val chipFullTime = findViewById<Chip>(R.id.chipFullTime)
        val chipPartTime = findViewById<Chip>(R.id.chipPartTime)
        val chipContract = findViewById<Chip>(R.id.chipContract)
        val chipRemote = findViewById<Chip>(R.id.chipRemote)
        if (chipFullTime.isChecked) selectedJobTypes.add("Full-time")
        if (chipPartTime.isChecked) selectedJobTypes.add("Part-time")
        if (chipContract.isChecked) selectedJobTypes.add("Contract")
        if (chipRemote.isChecked) selectedJobTypes.add("Remote")
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                val allJobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java).copy(id = doc.id)
                        if (job.status == "active") job else null
                    } catch (e: Exception) {
                        Log.e("SearchActivity", "Error converting document to Job object: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.postedDate.toDate() } 
                val filteredJobs = allJobs.filter { job ->
                    var matches = true
                    if (selectedField.isNotEmpty() && selectedField != "Any") {
                        matches = matches && job.jobField.equals(selectedField, ignoreCase = true)
                        if (matches && selectedSpecialization.isNotEmpty() && selectedSpecialization != "Any") {
                            matches = matches && job.specialization.equals(selectedSpecialization, ignoreCase = true)
                        }
                    }
                    if (selectedProvince.isNotEmpty() && selectedProvince != "Any") {
                        matches = matches && job.province.equals(selectedProvince, ignoreCase = true)
                    }
                    if (selectedExperience.isNotEmpty() && selectedExperience != "Any") {
                        matches = matches && job.experienceLevel.equals(selectedExperience, ignoreCase = true)
                    }
                    if (selectedSalary.isNotEmpty() && selectedSalary != "Any") {
                        matches = matches && isJobInSalaryRange(job.salary, selectedSalary)
                    }
                    if (selectedJobTypes.isNotEmpty()) {
                        matches = matches && selectedJobTypes.contains(job.type)
                    }
                    if (searchText.isNotEmpty()) {
                        matches = matches && (
                            job.title.contains(searchText, ignoreCase = true) ||
                            job.description.contains(searchText, ignoreCase = true) ||
                            job.companyName.contains(searchText, ignoreCase = true)
                        )
                    }
                    matches
                }
                updateSearchResults(filteredJobs)
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error searching jobs: ${e.message}")
                Toast.makeText(this, "Error searching jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.searchResultsRecyclerView.visibility = View.VISIBLE
            }
    }
    private fun updateSearchResults(jobs: List<Job>) {
        binding.resultsCountText.text = "${jobs.size} jobs found"
        binding.resultsCountText.visibility = View.VISIBLE
        jobsAdapter.submitList(jobs)
        if (jobs.isEmpty()) {
            if (binding.searchResultsRecyclerView.parent == binding.root) {
                (binding.searchResultsRecyclerView.parent as ViewGroup).addView(noResultsView)
            }
            binding.searchResultsRecyclerView.visibility = View.GONE
        } else {
            if (noResultsView.parent != null) {
                (noResultsView.parent as ViewGroup).removeView(noResultsView)
            }
            binding.searchResultsRecyclerView.visibility = View.VISIBLE
        }
    }
    private fun isJobInSalaryRange(jobSalary: String, selectedRange: String): Boolean {
        val jobSalaryNum = jobSalary.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        if (selectedRange.contains("+")) {
            val minSalary = selectedRange.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            return jobSalaryNum >= minSalary
        }
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
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}