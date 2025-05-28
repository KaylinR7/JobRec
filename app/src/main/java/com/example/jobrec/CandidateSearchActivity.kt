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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.jobrec.adapters.CertificateBadgeAdapter
import com.example.jobrec.adapters.CertificateBadge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.example.jobrec.utils.ImageUtils
import de.hdodenhof.circleimageview.CircleImageView
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
    private val selectedSkills = mutableListOf<String>()
    private val allSkills = mutableMapOf<String, Int>()
    private val allLocations = mutableMapOf<String, Int>()
    private val allFields = mutableMapOf<String, Int>()
    private val allEducationLevels = mutableMapOf<String, Int>()
    private val allUsers = mutableListOf<User>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidate_search)
        initializeViews()
        setupToolbar()
        loadFilterOptionsFromDatabase()
        setupRecyclerView()
        setupFilterButton()
        setupSearchFab()
        setupApplyFiltersButton()
        setupClearFiltersButton()
    }
    private fun setupFilterButton() {
        filterButton.setOnClickListener {
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
            drawerLayout.closeDrawer(GravityCompat.END)
            performSearch()
        }
    }
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)
        drawerLayout = findViewById(R.id.drawerLayout)
        filterButton = findViewById(R.id.filterButton)
        searchFab = findViewById(R.id.searchFab)
        resultsCountText = findViewById(R.id.resultsCountText)
        skillsFilter = findViewById(R.id.skillsFilter)
        selectedSkillsChipGroup = findViewById(R.id.selectedSkillsChipGroup)
        educationFilter = findViewById(R.id.educationFilter)
        experienceFilter = findViewById(R.id.experienceFilter)
        locationFilter = findViewById(R.id.locationFilter)
        fieldFilter = findViewById(R.id.fieldFilter)
        subFieldFilter = findViewById(R.id.subFieldFilter)
        applyFiltersButton = findViewById(R.id.applyFiltersButton)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)
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
        allUsers.clear()
        allSkills.clear()
        allLocations.clear()
        allFields.clear()
        allEducationLevels.clear()
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { document ->
                    try {
                        val user = document.toObject(User::class.java).copy(id = document.id)
                        if (user.role == "user") {
                            allUsers.add(user)
                            user.skills.forEach { skill ->
                                allSkills[skill] = allSkills.getOrDefault(skill, 0) + 1
                            }
                            if (user.province.isNotEmpty()) {
                                allLocations[user.province] = allLocations.getOrDefault(user.province, 0) + 1
                            }
                            if (user.field.isNotEmpty()) {
                                allFields[user.field] = allFields.getOrDefault(user.field, 0) + 1
                            }
                            user.education.forEach { education ->
                                if (education.degree.isNotEmpty()) {
                                    allEducationLevels[education.degree] =
                                        allEducationLevels.getOrDefault(education.degree, 0) + 1
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                setupFilters()
            }
            .addOnFailureListener {
                setupFilters()
            }
    }
    private fun setupFilters() {
        val skillsOptions = allSkills.entries
            .map { FilterOption(it.key, it.value) }
            .sortedBy { it.name }
        val skillsAdapter = FilterDropdownAdapter(this, skillsOptions)
        skillsFilter.setAdapter(skillsAdapter)
        skillsFilter.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedOption = skillsAdapter.getItem(position)
            val selectedSkill = selectedOption?.name ?: return@OnItemClickListener
            if (!selectedSkills.contains(selectedSkill)) {
                selectedSkills.add(selectedSkill)
                addSkillChip(selectedSkill)
                skillsFilter.setText("")
            }
        }
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
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val yearsCounts = mutableMapOf<String, Int>()
        for (option in yearsOptions) {
            yearsCounts[option] = allUsers.count { user ->
                user.yearsOfExperience == option
            }
        }
        val experienceOptions = listOf(FilterOption("Any", allUsers.size)) +
            yearsOptions.map { option ->
                FilterOption(option, yearsCounts.getOrDefault(option, 0))
            }
        val experienceAdapter = FilterDropdownAdapter(this, experienceOptions)
        experienceFilter.setAdapter(experienceAdapter)
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )
        val allLocationsCombined = mutableMapOf<String, Int>()
        allLocationsCombined.putAll(allLocations)
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
        val standardFields = FieldCategories.fields.keys.toTypedArray()
        val allFieldsCombined = mutableMapOf<String, Int>()
        allFieldsCombined.putAll(allFields)
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
        subFieldFilter.isEnabled = false
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
            showCandidateProfileDialog(candidate)
        }
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CandidateSearchActivity)
            adapter = resultsAdapter
            addItemDecoration(SpacingItemDecoration(16))
        }
    }
    private fun showCandidateProfileDialog(candidate: User) {

        val view = createCandidateProfileView(candidate)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("${candidate.name} ${candidate.surname}")
            .setView(view)
            .setPositiveButton("Contact") { dialog, which ->
                contactCandidate(candidate)
            }
            .setNegativeButton("Close", null)
            .create()
        dialog.show()
    }
    private fun createCandidateProfileView(candidate: User): View {
        val view = layoutInflater.inflate(R.layout.dialog_candidate_profile, null)

        // Load profile image
        val profileImage = view.findViewById<CircleImageView>(R.id.profileImage)
        ImageUtils.loadProfileImage(
            context = this,
            imageView = profileImage,
            user = candidate,
            isCircular = true
        )

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
        val skillsText = view.findViewById<TextView>(R.id.skillsText)
        if (candidate.skills.isNotEmpty()) {
            skillsText.text = candidate.skills.joinToString(", ")
        } else {
            skillsText.text = "No skills specified"
        }

        // Setup certificate badges
        val certificateBadgesRecyclerView = view.findViewById<RecyclerView>(R.id.certificateBadgesRecyclerView)
        val noCertificatesText = view.findViewById<TextView>(R.id.noCertificatesText)

        if (candidate.certificates.isNotEmpty()) {
            val badges = candidate.certificates.map { cert ->
                CertificateBadge(
                    name = cert["name"] ?: "",
                    issuer = cert["issuer"] ?: "",
                    year = cert["year"] ?: "",
                    description = cert["description"] ?: ""
                )
            }.filter { it.name.isNotEmpty() }

            if (badges.isNotEmpty()) {
                val badgeAdapter = CertificateBadgeAdapter { badge ->
                    // Show badge details in a dialog
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(badge.name)
                        .setMessage(buildString {
                            append("Issuer: ${badge.issuer}\n")
                            append("Year: ${badge.year}")
                            if (badge.description.isNotEmpty()) {
                                append("\n\nDescription: ${badge.description}")
                            }
                        })
                        .setPositiveButton("OK", null)
                        .show()
                }
                certificateBadgesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                certificateBadgesRecyclerView.adapter = badgeAdapter
                badgeAdapter.submitList(badges)
                certificateBadgesRecyclerView.visibility = View.VISIBLE
                noCertificatesText.visibility = View.GONE
            } else {
                certificateBadgesRecyclerView.visibility = View.GONE
                noCertificatesText.visibility = View.VISIBLE
            }
        } else {
            certificateBadgesRecyclerView.visibility = View.GONE
            noCertificatesText.visibility = View.VISIBLE
        }

        val educationText = view.findViewById<TextView>(R.id.educationText)
        if (candidate.education.isNotEmpty()) {
            val educationList = candidate.education.joinToString("\n") { edu ->
                "${edu.degree} at ${edu.institution} (${edu.fieldOfStudy})"
            }
            educationText.text = educationList
        } else {
            educationText.text = "No education specified"
        }
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
        val yearsOfExperienceText = view.findViewById<TextView>(R.id.yearsOfExperienceText)
        yearsOfExperienceText.text = if (candidate.yearsOfExperience.isNotEmpty()) {
            candidate.yearsOfExperience
        } else {
            "Not specified"
        }
        val certificateText = view.findViewById<TextView>(R.id.certificateText)
        certificateText.text = if (candidate.certificate.isNotEmpty()) {
            candidate.certificate
        } else {
            "Not specified"
        }
        val expectedSalaryText = view.findViewById<TextView>(R.id.expectedSalaryText)
        expectedSalaryText.text = if (candidate.expectedSalary.isNotEmpty()) {
            candidate.expectedSalary
        } else {
            "Not specified"
        }
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
        val options = arrayOf("Email", "Phone")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Contact ${candidate.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${candidate.email}")
                            putExtra(Intent.EXTRA_SUBJECT, "Job Opportunity")
                        }
                        startActivity(intent)
                    }
                    1 -> {
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
        searchInput.setText("")
        educationFilter.setText("")
        experienceFilter.setText("")
        locationFilter.setText("")
        fieldFilter.setText("")
        subFieldFilter.setText("")
        subFieldFilter.isEnabled = false
        selectedSkills.clear()
        selectedSkillsChipGroup.removeAllViews()
        resultsAdapter.submitList(emptyList())
        resultsCountText.visibility = View.GONE
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }
    private fun updateSubFieldDropdown(field: String) {
        val subFields = FieldCategories.fields[field] ?: listOf()
        if (subFields.isNotEmpty()) {
            val subFieldOptions = listOf(FilterOption("Any", allUsers.size)) +
                subFields.map { subField ->
                    val count = allUsers.count { user ->
                        user.subField.equals(subField, ignoreCase = true)
                    }
                    FilterOption(subField, count)
                }
            val subFieldAdapter = FilterDropdownAdapter(this, subFieldOptions)
            subFieldFilter.setAdapter(subFieldAdapter)
            subFieldFilter.isEnabled = true
            subFieldFilter.setText("", false)
        } else {
            subFieldFilter.setText("")
            subFieldFilter.isEnabled = false
        }
    }
    private fun calculateExperienceYears(experience: Experience): Long {
        val startDate = try {
            Date(experience.startDate.toLong())
        } catch (e: NumberFormatException) {
            Date()
        }
        val endDate = try {
            if (experience.endDate.isNotEmpty()) {
                Date(experience.endDate.toLong())
            } else {
                Date()
            }
        } catch (e: NumberFormatException) {
            Date()
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
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val allUsers = documents.mapNotNull { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        if (user.role == "user") user.copy(id = document.id) else null
                    } catch (e: Exception) {
                        null
                    }
                }
                val filteredCandidates = allUsers.filter { candidate ->
                    var matches = true
                    if (selectedSkills.isNotEmpty()) {
                        matches = matches && selectedSkills.all { skill ->
                            candidate.skills.any { it.contains(skill, ignoreCase = true) }
                        }
                    }
                    if (location.isNotEmpty() && location != "Any") {
                        matches = matches && candidate.province.contains(location, ignoreCase = true)
                    }
                    if (field.isNotEmpty() && field != "Any") {
                        matches = matches && candidate.field.contains(field, ignoreCase = true)
                        if (matches && subField.isNotEmpty() && subField != "Any") {
                            matches = matches && candidate.subField.contains(subField, ignoreCase = true)
                        }
                    }
                    if (education.isNotEmpty() && education != "Any") {
                        matches = matches && candidate.education.any { edu ->
                            edu.degree.contains(education, ignoreCase = true)
                        }
                    }
                    if (experience.isNotEmpty() && experience != "Any") {
                        if (candidate.yearsOfExperience.isNotEmpty()) {
                            matches = matches && candidate.yearsOfExperience == experience
                        } else {
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
                resultsAdapter.submitList(filteredCandidates)
                resultsCountText.text = "${filteredCandidates.size} candidates found"
                resultsCountText.visibility = View.VISIBLE
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
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}