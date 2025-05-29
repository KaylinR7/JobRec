package com.example.jobrec
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import com.example.jobrec.databinding.ActivityPostJobBinding
import com.example.jobrec.models.FieldCategories
import com.example.jobrec.Job
import com.example.jobrec.Company
import com.example.jobrec.utils.LocationUtils
import kotlinx.coroutines.launch
class PostJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var binding: ActivityPostJobBinding
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var jobFieldInput: AutoCompleteTextView
    private lateinit var jobSpecializationInput: AutoCompleteTextView
    private lateinit var skillsInput: AutoCompleteTextView
    private lateinit var selectedSkillsChipGroup: ChipGroup
    private lateinit var jobTypeInput: AutoCompleteTextView
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var cityInput: AutoCompleteTextView
    private lateinit var salaryRangeInput: AutoCompleteTextView
    private lateinit var experienceInput: AutoCompleteTextView
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var postButton: MaterialButton
    private val selectedSkills = mutableListOf<String>()
    private var selectedProvince: String = ""
    private var selectedCity: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostJobBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Post New Job"
        }
        db = FirebaseFirestore.getInstance()
        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isEmpty()) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initializeViews()
        setupJobTypeDropdown()
        setupLocationDropdowns()
        setupPostButton()
    }
    private fun initializeViews() {
        jobTitleInput = binding.jobTitleInput
        jobFieldInput = binding.jobFieldInput
        jobSpecializationInput = binding.jobSpecializationInput
        skillsInput = binding.skillsInput
        selectedSkillsChipGroup = binding.selectedSkillsChipGroup
        jobTypeInput = binding.jobTypeInput
        provinceInput = binding.provinceInput
        cityInput = binding.cityInput
        salaryRangeInput = binding.salaryRangeInput
        experienceInput = binding.experienceInput
        descriptionInput = binding.descriptionInput
        requirementsInput = binding.requirementsInput
        postButton = binding.postButton
    }
    private fun setupJobTypeDropdown() {
        val fieldOptions = FieldCategories.fields.keys.toTypedArray()
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        jobFieldInput.setAdapter(fieldAdapter)
        jobSpecializationInput.isEnabled = false
        skillsInput.isEnabled = false
        jobFieldInput.setOnItemClickListener { _, _, position, _ ->
            val selectedField = fieldOptions[position]
            updateSpecializationDropdown(selectedField)
            updateSkillsDropdown(selectedField)
        }
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        jobTypeInput.setAdapter(typeAdapter)
        // Location dropdowns are now handled by setupLocationDropdowns()
        val salaryOptions = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryOptions)
        salaryRangeInput.setAdapter(salaryAdapter)
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearsOptions)
        experienceInput.setAdapter(experienceAdapter)
    }

    private fun setupLocationDropdowns() {
        // Test location data first
        val testResult = LocationUtils.testLocationData()
        android.util.Log.d("PostJobActivity", "Location test result:\n$testResult")

        LocationUtils.setupCascadingLocationSpinners(
            context = this,
            provinceSpinner = provinceInput,
            citySpinner = cityInput
        ) { province, city ->
            selectedProvince = province
            selectedCity = city
            android.util.Log.d("PostJobActivity", "Location selected: $province, $city")
            Toast.makeText(this, "Selected: $city, $province", Toast.LENGTH_SHORT).show()
        }

        // Add test functionality - you can remove this later
        provinceInput.setOnLongClickListener {
            testLocationDropdowns()
            true
        }
    }

    private fun testLocationDropdowns() {
        Toast.makeText(this, "Testing location dropdowns...", Toast.LENGTH_SHORT).show()

        // Simulate selecting Gauteng
        provinceInput.setText("Gauteng", false)

        // Manually trigger the province selection
        val cities = com.example.jobrec.models.LocationData.getCitiesForProvince("Gauteng")
        android.util.Log.d("PostJobActivity", "Test: Found ${cities.size} cities for Gauteng")
        android.util.Log.d("PostJobActivity", "Test: Cities = ${cities.take(5)}")

        if (cities.isNotEmpty()) {
            val cityAdapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                cities.toMutableList()
            )
            cityInput.setAdapter(cityAdapter)
            cityInput.isEnabled = true
            cityInput.setText("", false)
            cityInput.threshold = 1

            Toast.makeText(this, "City dropdown should now have ${cities.size} cities", Toast.LENGTH_LONG).show()
        }
    }
    private fun updateSpecializationDropdown(field: String) {
        val subFields = FieldCategories.fields[field] ?: listOf()
        if (subFields.isNotEmpty()) {
            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subFields)
            jobSpecializationInput.setAdapter(subFieldAdapter)
            jobSpecializationInput.isEnabled = true
            jobSpecializationInput.setText("", false)
        } else {
            jobSpecializationInput.setText("")
            jobSpecializationInput.isEnabled = false
        }
    }

    private fun updateSkillsDropdown(field: String) {
        val skills = FieldCategories.skills[field] ?: listOf()
        if (skills.isNotEmpty()) {
            val skillsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, skills)
            skillsInput.setAdapter(skillsAdapter)
            skillsInput.isEnabled = true
            skillsInput.setText("", false)

            skillsInput.setOnItemClickListener { _, _, position, _ ->
                val selectedSkill = skills[position]
                if (!selectedSkills.contains(selectedSkill)) {
                    selectedSkills.add(selectedSkill)
                    addSkillChip(selectedSkill)
                    skillsInput.setText("", false)
                }
            }
        } else {
            skillsInput.setText("")
            skillsInput.isEnabled = false
            selectedSkills.clear()
            selectedSkillsChipGroup.removeAllViews()
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
    private fun setupPostButton() {
        postButton.setOnClickListener {
            if (validateInputs()) {
                db.collection("companies")
                    .document(companyId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val company = document.toObject(Company::class.java)
                            company?.let { postJob(it) }
                        } else {
                            Toast.makeText(this, "Company profile not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error loading company profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    private fun validateInputs(): Boolean {
        var isValid = true
        if (jobTitleInput.text.isNullOrBlank()) {
            jobTitleInput.error = "Job title is required"
            isValid = false
        }
        if (jobFieldInput.text.isNullOrBlank()) {
            jobFieldInput.error = "Job field is required"
            isValid = false
        }
        if (jobSpecializationInput.isEnabled && jobSpecializationInput.text.isNullOrBlank()) {
            jobSpecializationInput.error = "Specialization is required"
            isValid = false
        }
        if (jobTypeInput.text.isNullOrBlank()) {
            jobTypeInput.error = "Job type is required"
            isValid = false
        }
        if (!LocationUtils.isValidLocationSelection(selectedProvince, selectedCity)) {
            Toast.makeText(this, "Please select both province and city", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (salaryRangeInput.text.isNullOrBlank()) {
            salaryRangeInput.error = "Salary range is required"
            isValid = false
        }
        if (experienceInput.text.isNullOrBlank()) {
            experienceInput.error = "Experience level is required"
            isValid = false
        }
        if (descriptionInput.text.isNullOrBlank()) {
            descriptionInput.error = "Job description is required"
            isValid = false
        }
        if (requirementsInput.text.isNullOrBlank()) {
            requirementsInput.error = "Requirements are required"
            isValid = false
        }
        return isValid
    }
    private fun postJob(company: Company) {
        val jobData = hashMapOf(
            "title" to jobTitleInput.text.toString(),
            "companyId" to companyId,
            "companyName" to company.companyName,
            "jobField" to jobFieldInput.text.toString(),
            "specialization" to jobSpecializationInput.text.toString(),
            "province" to selectedProvince,
            "city" to selectedCity,
            "salary" to salaryRangeInput.text.toString(),
            "type" to jobTypeInput.text.toString(),
            "experienceLevel" to experienceInput.text.toString(),
            "description" to descriptionInput.text.toString(),
            "requirements" to requirementsInput.text.toString(),
            "requiredSkills" to selectedSkills,
            "postedDate" to com.google.firebase.Timestamp.now(),
            "status" to "active"
        )
        postButton.isEnabled = false
        postButton.text = "Posting..."
        db.collection("jobs")
            .add(jobData)
            .addOnSuccessListener { documentReference ->
                val jobId = documentReference.id
                db.collection("jobs").document(jobId)
                    .update("id", jobId)
                    .addOnSuccessListener {
                        val job = Job(
                            id = jobId,
                            title = jobTitleInput.text.toString(),
                            companyId = companyId,
                            companyName = company.companyName,
                            jobField = jobFieldInput.text.toString(),
                            specialization = jobSpecializationInput.text.toString(),
                            province = selectedProvince,
                            city = selectedCity,
                            salary = salaryRangeInput.text.toString(),
                            type = jobTypeInput.text.toString(),
                            jobType = jobTypeInput.text.toString(),
                            experienceLevel = experienceInput.text.toString(),
                            description = descriptionInput.text.toString(),
                            requirements = requirementsInput.text.toString(),
                            requiredSkills = selectedSkills.toList(),
                            postedDate = com.google.firebase.Timestamp.now(),
                            status = "active"
                        )



                        Toast.makeText(this, "Job posted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        postButton.isEnabled = true
                        postButton.text = "Post Job"
                        Toast.makeText(this, "Error updating job ID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                postButton.isEnabled = true
                postButton.text = "Post Job"
                Toast.makeText(this, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}