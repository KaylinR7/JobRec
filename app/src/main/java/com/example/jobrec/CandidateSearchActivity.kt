package com.example.jobrec

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

class CandidateSearchActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var searchInput: TextInputEditText
    private lateinit var skillsFilter: TextInputEditText
    private lateinit var educationFilter: AutoCompleteTextView
    private lateinit var experienceFilter: AutoCompleteTextView
    private lateinit var locationFilter: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsAdapter: CandidateSearchAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidate_search)

        // Initialize views
        initializeViews()
        setupToolbar()
        setupFilters()
        setupRecyclerView()
        setupSearchButton()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)
        skillsFilter = findViewById(R.id.skillsFilter)
        educationFilter = findViewById(R.id.educationFilter)
        experienceFilter = findViewById(R.id.experienceFilter)
        locationFilter = findViewById(R.id.locationFilter)
        searchButton = findViewById(R.id.searchButton)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Search Candidates"
        }
    }

    private fun setupFilters() {
        // Setup education level dropdown
        val educationLevels = arrayOf("Any", "High School", "Diploma", "Bachelor's", "Master's", "PhD")
        val educationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, educationLevels)
        educationFilter.setAdapter(educationAdapter)

        // Setup experience level dropdown
        val experienceLevels = arrayOf("Any", "Entry Level", "Mid Level", "Senior Level", "Executive")
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        experienceFilter.setAdapter(experienceAdapter)
    }

    private fun setupRecyclerView() {
        resultsAdapter = CandidateSearchAdapter { candidate ->
            // Handle candidate profile view
            // TODO: Implement profile view navigation
        }
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CandidateSearchActivity)
            adapter = resultsAdapter
        }
    }

    private fun setupSearchButton() {
        searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun calculateExperienceYears(experience: Experience): Long {
        val startDate = try {
            Date(experience.startDate.toLong())
        } catch (e: NumberFormatException) {
            Date() // Fallback to current date if parsing fails
        }
        
        val endDate = try {
            if (experience.endDate.isNotEmpty()) {
                Date(experience.endDate.toLong())
            } else {
                Date() // Use current date if end date is empty
            }
        } catch (e: NumberFormatException) {
            Date() // Fallback to current date if parsing fails
        }
        
        val diffInMillis = endDate.time - startDate.time
        return TimeUnit.MILLISECONDS.toDays(diffInMillis) / 365
    }

    private fun performSearch() {
        val searchQuery = searchInput.text.toString().trim()
        val skills = skillsFilter.text.toString().trim()
        val education = educationFilter.text.toString().trim()
        val experience = experienceFilter.text.toString().trim()
        val location = locationFilter.text.toString().trim()

        var query = db.collection("Users")
            .whereEqualTo("userType", "candidate")

        // Apply filters
        if (skills.isNotEmpty()) {
            query = query.whereArrayContains("skills", skills)
        }

        if (education != "Any" && education.isNotEmpty()) {
            query = query.whereEqualTo("highestEducation", education)
        }

        if (location.isNotEmpty()) {
            query = query.whereEqualTo("location", location)
        }

        // Execute search
        query.get()
            .addOnSuccessListener { documents ->
                val candidates = documents.mapNotNull { document ->
                    document.toObject(User::class.java)
                }.filter { candidate ->
                    // Filter by experience level
                    if (experience != "Any" && experience.isNotEmpty()) {
                        val candidateExperience = candidate.experience.maxByOrNull { exp ->
                            calculateExperienceYears(exp)
                        }?.let { exp ->
                            calculateExperienceYears(exp)
                        } ?: 0L
                        when (experience) {
                            "Entry Level" -> candidateExperience <= 2
                            "Mid Level" -> candidateExperience in 3..5
                            "Senior Level" -> candidateExperience in 6..10
                            "Executive" -> candidateExperience > 10
                            else -> true
                        }
                    } else true
                }.filter { candidate ->
                    // Filter by search query
                    if (searchQuery.isNotEmpty()) {
                        candidate.name.contains(searchQuery, ignoreCase = true) ||
                        candidate.surname.contains(searchQuery, ignoreCase = true) ||
                        candidate.skills.any { it.contains(searchQuery, ignoreCase = true) }
                    } else true
                }

                resultsAdapter.submitList(candidates)
                if (candidates.isEmpty()) {
                    Toast.makeText(this, "No candidates found matching your criteria", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error searching candidates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 