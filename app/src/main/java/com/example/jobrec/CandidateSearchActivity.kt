package com.example.jobrec

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import com.example.jobrec.models.FieldCategories
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

class CandidateSearchActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var searchInput: TextInputEditText
    private lateinit var skillsFilter: AutoCompleteTextView
    private lateinit var selectedSkillsChipGroup: ChipGroup
    private lateinit var educationFilter: AutoCompleteTextView
    private lateinit var experienceFilter: AutoCompleteTextView
    private lateinit var locationFilter: AutoCompleteTextView
    private lateinit var fieldFilter: AutoCompleteTextView
    private lateinit var subFieldFilter: AutoCompleteTextView
    private lateinit var applyFiltersButton: MaterialButton
    private lateinit var clearFiltersButton: MaterialButton
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsAdapter: CandidateSearchAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var filterButton: MaterialButton
    private lateinit var searchFab: FloatingActionButton
    private lateinit var resultsCountText: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lists to store filter options
    private val selectedSkills = mutableListOf<String>()
    private val allSkills = mutableMapOf<String, Int>() // skill name to count
    private val allLocations = mutableMapOf<String, Int>() // location to count
    private val allFields = mutableMapOf<String, Int>() // field to count
    private val allEducationLevels = mutableMapOf<String, Int>() // education level to count

    // List to store all users for counting filter options
    private val allUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidate_search)

        // Initialize views
        initializeViews()
        setupToolbar()

        // Load filter options from database
        loadFilterOptionsFromDatabase()

        // Setup UI components
        setupRecyclerView()
        setupFilterButton()
        setupSearchFab()
        setupApplyFiltersButton()
        setupClearFiltersButton()
    }

    private fun setupFilterButton() {
        filterButton.setOnClickListener {
            // Open the drawer from the right side
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupSearchFab() {
        searchFab.setOnClickListener {
            performSearch()
        }
    }

    private fun setupApplyFiltersButton() {
        applyFiltersButton.setOnClickListener {
            // Close drawer and perform search
            drawerLayout.closeDrawer(GravityCompat.END)
            performSearch()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawerLayout)

        // Initialize filter button and search FAB
        filterButton = findViewById(R.id.filterButton)
        searchFab = findViewById(R.id.searchFab)

        // Initialize results count text
        resultsCountText = findViewById(R.id.resultsCountText)

        // Initialize drawer filter controls
        skillsFilter = findViewById(R.id.skillsFilter)
        selectedSkillsChipGroup = findViewById(R.id.selectedSkillsChipGroup)
        educationFilter = findViewById(R.id.educationFilter)
        experienceFilter = findViewById(R.id.experienceFilter)
        locationFilter = findViewById(R.id.locationFilter)
        fieldFilter = findViewById(R.id.fieldFilter)
        subFieldFilter = findViewById(R.id.subFieldFilter)

        // Initialize drawer buttons
        applyFiltersButton = findViewById(R.id.applyFiltersButton)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)

        // Initialize results recycler view
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

    private fun loadFilterOptionsFromDatabase() {
        // Clear previous data
        allUsers.clear()
        allSkills.clear()
        allLocations.clear()
        allFields.clear()
        allEducationLevels.clear()

        // Load all users to extract filter options - using simple query
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { document ->
                    try {
                        val user = document.toObject(User::class.java).copy(id = document.id)
                        // Only process users with role="user"
                        if (user.role == "user") {
                            allUsers.add(user)

                            // Count skills
                            user.skills.forEach { skill ->
                                allSkills[skill] = allSkills.getOrDefault(skill, 0) + 1
                            }

                            // Count locations (using province instead of address)
                            if (user.province.isNotEmpty()) {
                                allLocations[user.province] = allLocations.getOrDefault(user.province, 0) + 1
                            }

                            // Count fields/industries
                            if (user.field.isNotEmpty()) {
                                allFields[user.field] = allFields.getOrDefault(user.field, 0) + 1
                            }

                            // Count education levels
                            user.education.forEach { education ->
                                if (education.degree.isNotEmpty()) {
                                    allEducationLevels[education.degree] =
                                        allEducationLevels.getOrDefault(education.degree, 0) + 1
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip this user if there's an error
                    }
                }

                // Setup filter dropdowns with the data
                setupFilters()
            }
            .addOnFailureListener {
                // If loading fails, set up filters with default values
                setupFilters()
            }
    }

    private fun setupFilters() {
        // Setup skills dropdown with custom adapter showing counts
        val skillsOptions = allSkills.entries
            .map { FilterOption(it.key, it.value) }
            .sortedBy { it.name }
        val skillsAdapter = FilterDropdownAdapter(this, skillsOptions)
        skillsFilter.setAdapter(skillsAdapter)

        // Setup skills selection
        skillsFilter.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedOption = skillsAdapter.getItem(position)
            val selectedSkill = selectedOption?.name ?: return@OnItemClickListener

            if (!selectedSkills.contains(selectedSkill)) {
                selectedSkills.add(selectedSkill)
                addSkillChip(selectedSkill)
                skillsFilter.setText("") // Clear the input field
            }
        }

        // Setup education level dropdown with custom adapter
        // Standard certificate options from signup
        val certificateOptions = arrayOf(
            "None",
            "High School Diploma",
            "Associate's Degree",
            "Bachelor's Degree",
            "Master's Degree",
            "PhD",
            "Professional Certification",
            "Technical Certification"
        )

        // Count users by education level
        val educationCounts = certificateOptions.associateWith { certType ->
            allUsers.count { user ->
                user.education.any { edu ->
                    edu.degree.contains(certType, ignoreCase = true)
                }
            }
        }

        val defaultEducationLevels = listOf(FilterOption("Any", allUsers.size))
        val educationOptions = defaultEducationLevels + educationCounts.entries
            .map { FilterOption(it.key, it.value) }
            .sortedBy { it.name }

        val educationAdapter = FilterDropdownAdapter(this, educationOptions)
        educationFilter.setAdapter(educationAdapter)

        // Setup experience level dropdown with custom adapter
        // Standard years of experience options from signup
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")

        // Count users by experience level
        val yearsCounts = mutableMapOf<String, Int>()
        for (option in yearsOptions) {
            yearsCounts[option] = allUsers.count { user ->
                user.yearsOfExperience == option
            }
        }

        // Use the exact same years of experience options as in signup
        val experienceOptions = listOf(FilterOption("Any", allUsers.size)) +
            yearsOptions.map { option ->
                FilterOption(option, yearsCounts.getOrDefault(option, 0))
            }
        val experienceAdapter = FilterDropdownAdapter(this, experienceOptions)
        experienceFilter.setAdapter(experienceAdapter)

        // Setup location dropdown with custom adapter
        // Add standard provinces
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )

        // Create a combined map of all locations
        val allLocationsCombined = mutableMapOf<String, Int>()

        // Add counts from database
        allLocationsCombined.putAll(allLocations)

        // Add standard provinces with 0 count if not already in the map
        for (province in provinces) {
            if (!allLocationsCombined.containsKey(province)) {
                allLocationsCombined[province] = 0
            }
        }

        val locationOptions = allLocationsCombined.entries
            .map { FilterOption(it.key, it.value) }
            .sortedBy { it.name }
        val locationsAdapter = FilterDropdownAdapter(this, locationOptions)
        locationFilter.setAdapter(locationsAdapter)

        // Setup field/industry dropdown with custom adapter
        // Standard field options - get directly from FieldCategories to ensure consistency
        val standardFields = FieldCategories.fields.keys.toTypedArray()

        // Create a combined map of all fields
        val allFieldsCombined = mutableMapOf<String, Int>()

        // Add counts from database
        allFieldsCombined.putAll(allFields)

        // Add standard fields with 0 count if not already in the map
        for (field in standardFields) {
            if (!allFieldsCombined.containsKey(field)) {
                allFieldsCombined[field] = 0
            }
        }

        val fieldOptions = allFieldsCombined.entries
            .map { FilterOption(it.key, it.value) }
            .sortedBy { it.name }
        val fieldsAdapter = FilterDropdownAdapter(this, fieldOptions)
        fieldFilter.setAdapter(fieldsAdapter)

        // Initially disable subfield dropdown
        subFieldFilter.isEnabled = false

        // Setup field selection listener to update subfields
        fieldFilter.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = fieldsAdapter.getItem(position)
            val selectedField = selectedOption?.name ?: return@setOnItemClickListener
            updateSubFieldDropdown(selectedField)
        }
    }

    private fun addSkillChip(skill: String) {
        val chip = Chip(this).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedSkills.remove(skill)
                selectedSkillsChipGroup.removeView(this)
            }
        }
        selectedSkillsChipGroup.addView(chip)
    }

    private fun setupRecyclerView() {
        resultsAdapter = CandidateSearchAdapter { candidate ->
            // Show candidate profile dialog
            showCandidateProfileDialog(candidate)
        }
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CandidateSearchActivity)
            adapter = resultsAdapter
            addItemDecoration(SpacingItemDecoration(16)) // Add spacing between items
        }
    }

    private fun showCandidateProfileDialog(candidate: User) {
        // Create a dialog to show candidate details
        val view = createCandidateProfileView(candidate)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("${candidate.name} ${candidate.surname}")
            .setView(view)
            .setPositiveButton("Contact") { dialog, which ->
                // Contact the candidate
                contactCandidate(candidate)
            }
            .setNegativeButton("Close", null)
            .create()

        dialog.show()
    }

    private fun createCandidateProfileView(candidate: User): View {
        val view = layoutInflater.inflate(R.layout.dialog_candidate_profile, null)

        // Set candidate details
        val nameText = view.findViewById<TextView>(R.id.nameText)
        nameText.text = "${candidate.name} ${candidate.surname}"

        val emailText = view.findViewById<TextView>(R.id.emailText)
        emailText.text = candidate.email

        val phoneText = view.findViewById<TextView>(R.id.phoneText)
        phoneText.text = candidate.phoneNumber

        val locationText = view.findViewById<TextView>(R.id.locationText)
        locationText.text = candidate.province

        val summaryText = view.findViewById<TextView>(R.id.summaryText)
        summaryText.text = candidate.summary

        // Set skills
        val skillsText = view.findViewById<TextView>(R.id.skillsText)
        if (candidate.skills.isNotEmpty()) {
            skillsText.text = candidate.skills.joinToString(", ")
        } else {
            skillsText.text = "No skills specified"
        }

        // Set education
        val educationText = view.findViewById<TextView>(R.id.educationText)
        if (candidate.education.isNotEmpty()) {
            val educationList = candidate.education.joinToString("\n") { edu ->
                "${edu.degree} at ${edu.institution} (${edu.fieldOfStudy})"
            }
            educationText.text = educationList
        } else {
            educationText.text = "No education specified"
        }

        // Set experience
        val experienceText = view.findViewById<TextView>(R.id.experienceText)
        if (candidate.experience.isNotEmpty()) {
            val experienceList = candidate.experience.joinToString("\n\n") { exp ->
                "${exp.position} at ${exp.company}\n" +
                "${exp.startDate} - ${exp.endDate}\n" +
                exp.description
            }
            experienceText.text = experienceList
        } else {
            experienceText.text = "No experience specified"
        }

        // Set field and specialization
        val fieldText = view.findViewById<TextView>(R.id.fieldText)
        if (candidate.field.isNotEmpty()) {
            val fieldInfo = if (candidate.subField.isNotEmpty()) {
                "${candidate.field} - ${candidate.subField}"
            } else {
                candidate.field
            }
            fieldText.text = fieldInfo
        } else {
            fieldText.text = "Not specified"
        }

        // Set years of experience
        val yearsOfExperienceText = view.findViewById<TextView>(R.id.yearsOfExperienceText)
        yearsOfExperienceText.text = if (candidate.yearsOfExperience.isNotEmpty()) {
            candidate.yearsOfExperience
        } else {
            "Not specified"
        }

        // Set certificate
        val certificateText = view.findViewById<TextView>(R.id.certificateText)
        certificateText.text = if (candidate.certificate.isNotEmpty()) {
            candidate.certificate
        } else {
            "Not specified"
        }

        // Set expected salary
        val expectedSalaryText = view.findViewById<TextView>(R.id.expectedSalaryText)
        expectedSalaryText.text = if (candidate.expectedSalary.isNotEmpty()) {
            candidate.expectedSalary
        } else {
            "Not specified"
        }

        // Set social links
        val socialLinksText = view.findViewById<TextView>(R.id.socialLinksText)
        val socialLinks = mutableListOf<String>()

        if (candidate.linkedin.isNotEmpty()) {
            socialLinks.add("LinkedIn: ${candidate.linkedin}")
        }

        if (candidate.github.isNotEmpty()) {
            socialLinks.add("GitHub: ${candidate.github}")
        }

        if (candidate.portfolio.isNotEmpty()) {
            socialLinks.add("Portfolio: ${candidate.portfolio}")
        }

        socialLinksText.text = if (socialLinks.isNotEmpty()) {
            socialLinks.joinToString("\n")
        } else {
            "No social links provided"
        }

        return view
    }

    private fun contactCandidate(candidate: User) {
        // Show options to contact the candidate
        val options = arrayOf("Email", "Phone")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Contact ${candidate.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Email
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${candidate.email}")
                            putExtra(Intent.EXTRA_SUBJECT, "Job Opportunity")
                        }
                        startActivity(intent)
                    }
                    1 -> { // Phone
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${candidate.phoneNumber}")
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun setupClearFiltersButton() {
        clearFiltersButton.setOnClickListener {
            clearFilters()
        }
    }

    private fun clearFilters() {
        // Clear all filters
        searchInput.setText("")
        educationFilter.setText("")
        experienceFilter.setText("")
        locationFilter.setText("")
        fieldFilter.setText("")
        subFieldFilter.setText("")
        subFieldFilter.isEnabled = false

        // Clear selected skills
        selectedSkills.clear()
        selectedSkillsChipGroup.removeAllViews()

        // Clear results
        resultsAdapter.submitList(emptyList())
        resultsCountText.visibility = View.GONE

        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateSubFieldDropdown(field: String) {
        // Get subcategories for the selected field from FieldCategories
        val subFields = FieldCategories.fields[field] ?: listOf()

        if (subFields.isNotEmpty()) {
            // Create a list of FilterOption objects with counts
            val subFieldOptions = listOf(FilterOption("Any", allUsers.size)) +
                subFields.map { subField ->
                    // Count users with this subfield
                    val count = allUsers.count { user ->
                        user.subField.equals(subField, ignoreCase = true)
                    }
                    FilterOption(subField, count)
                }

            val subFieldAdapter = FilterDropdownAdapter(this, subFieldOptions)
            subFieldFilter.setAdapter(subFieldAdapter)
            subFieldFilter.isEnabled = true
            subFieldFilter.setText("", false) // Clear previous selection
        } else {
            // If no subcategories exist for this field
            subFieldFilter.setText("")
            subFieldFilter.isEnabled = false
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
        val education = educationFilter.text.toString().trim()
        val experience = experienceFilter.text.toString().trim()
        val location = locationFilter.text.toString().trim()
        val field = fieldFilter.text.toString().trim()
        val subField = subFieldFilter.text.toString().trim()

        // Super simple query approach - just get all users
        // This is the simplest possible query with no complex filters
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                // Convert documents to User objects and filter by role in memory
                val allUsers = documents.mapNotNull { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        // Only include regular users, not companies or admins
                        if (user.role == "user") user.copy(id = document.id) else null
                    } catch (e: Exception) {
                        null
                    }
                }

                // Apply all filters in memory
                val filteredCandidates = allUsers.filter { candidate ->
                    var matches = true

                    // Filter by skills
                    if (selectedSkills.isNotEmpty()) {
                        matches = matches && selectedSkills.all { skill ->
                            candidate.skills.any { it.contains(skill, ignoreCase = true) }
                        }
                    }

                    // Filter by location (using province instead of address)
                    if (location.isNotEmpty() && location != "Any") {
                        matches = matches && candidate.province.contains(location, ignoreCase = true)
                    }

                    // Filter by field/industry
                    if (field.isNotEmpty() && field != "Any") {
                        matches = matches && candidate.field.contains(field, ignoreCase = true)

                        // Filter by specialization (only if field matches)
                        if (matches && subField.isNotEmpty() && subField != "Any") {
                            matches = matches && candidate.subField.contains(subField, ignoreCase = true)
                        }
                    }

                    // Filter by education level
                    if (education.isNotEmpty() && education != "Any") {
                        matches = matches && candidate.education.any { edu ->
                            edu.degree.contains(education, ignoreCase = true)
                        }
                    }

                    // Filter by experience level
                    if (experience.isNotEmpty() && experience != "Any") {
                        if (candidate.yearsOfExperience.isNotEmpty()) {
                            matches = matches && candidate.yearsOfExperience == experience
                        } else {
                            // Calculate experience from entries
                            val candidateExperience = candidate.experience.sumOf { exp ->
                                calculateExperienceYears(exp).toInt()
                            }
                            matches = matches && when (experience) {
                                "0-1 years" -> candidateExperience <= 1
                                "1-3 years" -> candidateExperience in 1..3
                                "3-5 years" -> candidateExperience in 3..5
                                "5-10 years" -> candidateExperience in 5..10
                                "10+ years" -> candidateExperience > 10
                                else -> true
                            }
                        }
                    }

                    // Filter by search query
                    if (searchQuery.isNotEmpty()) {
                        matches = matches && (
                            candidate.name.contains(searchQuery, ignoreCase = true) ||
                            candidate.surname.contains(searchQuery, ignoreCase = true) ||
                            candidate.skills.any { it.contains(searchQuery, ignoreCase = true) } ||
                            candidate.field.contains(searchQuery, ignoreCase = true) ||
                            candidate.summary.contains(searchQuery, ignoreCase = true)
                        )
                    }

                    matches
                }

                // Update UI with filtered results
                resultsAdapter.submitList(filteredCandidates)

                // Update results count text
                resultsCountText.text = "${filteredCandidates.size} candidates found"
                resultsCountText.visibility = View.VISIBLE

                // Show appropriate message
                if (filteredCandidates.isEmpty()) {
                    Toast.makeText(this, "No candidates found matching your criteria", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Found ${filteredCandidates.size} candidates", Toast.LENGTH_SHORT).show()
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

    override fun onBackPressed() {
        // Close drawer if open, otherwise go back
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}