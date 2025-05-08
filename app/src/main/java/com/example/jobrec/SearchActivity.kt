package com.example.jobrec

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivitySearchBinding
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var jobsAdapter: RecentJobsAdapter

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

    // Salary ranges
    private val salaryRanges = arrayOf(
        "R0 - R10,000",
        "R10,001 - R20,000",
        "R20,001 - R30,000",
        "R30,001 - R40,000",
        "R40,001 - R50,000",
        "R50,001+"
    )

    // Experience levels
    private val experienceLevels = arrayOf(
        "Entry Level (0-2 years)",
        "Junior (2-4 years)",
        "Mid-Level (4-6 years)",
        "Senior (6-8 years)",
        "Expert (8+ years)"
    )

    // Job Fields (comprehensive list)
    private val jobFields = arrayOf(
        // Information Technology
        "Software Development",
        "Web Development",
        "Mobile Development",
        "Data Science",
        "Machine Learning",
        "Artificial Intelligence",
        "Cloud Computing",
        "DevOps",
        "Network Engineering",
        "Cybersecurity",
        "Database Administration",
        "IT Support",
        "System Administration",
        "UI/UX Design",
        "Quality Assurance",

        // Business & Finance
        "Accounting",
        "Banking",
        "Financial Analysis",
        "Investment Banking",
        "Insurance",
        "Auditing",
        "Tax Consulting",
        "Business Administration",
        "Project Management",
        "Business Analysis",
        "Risk Management",
        "Compliance",

        // Healthcare & Medical
        "Nursing",
        "Medical Practice",
        "Pharmacy",
        "Physiotherapy",
        "Occupational Therapy",
        "Medical Administration",
        "Healthcare Management",
        "Medical Research",
        "Mental Health",
        "Public Health",

        // Engineering
        "Civil Engineering",
        "Mechanical Engineering",
        "Electrical Engineering",
        "Chemical Engineering",
        "Industrial Engineering",
        "Aerospace Engineering",
        "Biomedical Engineering",
        "Environmental Engineering",
        "Structural Engineering",
        "Mining Engineering",

        // Education & Training
        "Teaching",
        "Educational Administration",
        "Curriculum Development",
        "Special Education",
        "Early Childhood Education",
        "Higher Education",
        "Educational Technology",
        "Training & Development",
        "Student Services",

        // Marketing & Communications
        "Digital Marketing",
        "Content Marketing",
        "Social Media Marketing",
        "Public Relations",
        "Brand Management",
        "Market Research",
        "Advertising",
        "Copywriting",
        "Media Relations",
        "Event Management",

        // Sales & Retail
        "Sales Management",
        "Retail Management",
        "Business Development",
        "Account Management",
        "Customer Service",
        "Merchandising",
        "E-commerce",
        "Sales Operations",
        "Channel Sales",

        // Human Resources
        "HR Management",
        "Recruitment",
        "Talent Acquisition",
        "Employee Relations",
        "Compensation & Benefits",
        "HR Operations",
        "Learning & Development",
        "HR Analytics",

        // Legal
        "Law Practice",
        "Corporate Law",
        "Criminal Law",
        "Family Law",
        "Intellectual Property Law",
        "Legal Administration",
        "Compliance & Regulatory",
        "Contract Law",

        // Creative & Design
        "Graphic Design",
        "Interior Design",
        "Fashion Design",
        "Industrial Design",
        "Architecture",
        "Animation",
        "Video Production",
        "Photography",
        "Art Direction",

        // Science & Research
        "Biology",
        "Chemistry",
        "Physics",
        "Environmental Science",
        "Laboratory Research",
        "Scientific Research",
        "Biotechnology",
        "Pharmaceutical Research",

        // Hospitality & Tourism
        "Hotel Management",
        "Restaurant Management",
        "Tourism Management",
        "Event Planning",
        "Culinary Arts",
        "Travel Services",
        "Customer Experience",

        // Manufacturing & Production
        "Production Management",
        "Quality Control",
        "Supply Chain",
        "Operations Management",
        "Manufacturing Engineering",
        "Process Improvement",
        "Inventory Management",

        // Transportation & Logistics
        "Logistics Management",
        "Supply Chain Management",
        "Transportation Management",
        "Fleet Management",
        "Warehouse Operations",
        "Distribution Management",

        // Construction & Real Estate
        "Construction Management",
        "Real Estate Development",
        "Property Management",
        "Architecture",
        "Urban Planning",
        "Building Inspection",
        "Construction Engineering",

        // Agriculture & Forestry
        "Agricultural Management",
        "Farm Management",
        "Agricultural Science",
        "Forestry Management",
        "Environmental Conservation",
        "Horticulture",

        // Government & Public Service
        "Public Administration",
        "Policy Analysis",
        "Urban Planning",
        "Social Services",
        "Public Safety",
        "Community Development",

        // Non-Profit & Social Services
        "Non-Profit Management",
        "Social Work",
        "Community Outreach",
        "Program Management",
        "Grant Writing",
        "Advocacy"
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

        // Initialize RecyclerView
        setupRecyclerView()

        // Setup dropdowns
        setupDropdowns()

        // Setup search button
        setupSearchButton()
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
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val selectedField = (binding.jobFieldLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedProvince = (binding.provinceLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedSalary = (binding.salaryRangeLayout.editText as? AutoCompleteTextView)?.text.toString()
        val selectedExperience = (binding.experienceLayout.editText as? AutoCompleteTextView)?.text.toString()

        // Build the query
        var query = db.collection("jobs").whereEqualTo("status", "active")

        // Add filters if they are selected
        if (selectedField.isNotEmpty()) {
            query = query.whereEqualTo("jobField", selectedField)
        }
        if (selectedProvince.isNotEmpty()) {
            query = query.whereEqualTo("province", selectedProvince)
        }
        if (selectedExperience.isNotEmpty()) {
            query = query.whereEqualTo("experienceLevel", selectedExperience)
        }

        // Execute the query
        query.get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { doc ->
                    try {
                        val job = doc.toObject(Job::class.java).copy(id = doc.id)
                        // Filter by salary range if selected
                        if (selectedSalary.isNotEmpty() && !isJobInSalaryRange(job.salary, selectedSalary)) {
                            null
                        } else {
                            job
                        }
                    } catch (e: Exception) {
                        Log.e("SearchActivity", "Error converting document to Job object: ${e.message}")
                        null
                    }
                }
                jobsAdapter.submitList(jobs)
                if (jobs.isEmpty()) {
                    Toast.makeText(this, "No jobs found matching your criteria", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error searching jobs: ${e.message}")
                Toast.makeText(this, "Error searching jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isJobInSalaryRange(jobSalary: String, selectedRange: String): Boolean {
        // Convert salary strings to numbers and compare
        val jobSalaryNum = jobSalary.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
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