package com.example.jobrec
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jobrec.models.FieldCategories
import com.example.jobrec.utils.LocationUtils
class EditJobActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var jobId: String
    private lateinit var companyId: String

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var titleInput: TextInputEditText
    private lateinit var companyInput: TextInputEditText
    private lateinit var fieldInput: AutoCompleteTextView
    private lateinit var specializationInput: AutoCompleteTextView
    private lateinit var typeInput: AutoCompleteTextView
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var cityInput: AutoCompleteTextView
    private lateinit var salaryRangeInput: AutoCompleteTextView
    private lateinit var experienceInput: AutoCompleteTextView
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    // Selected values
    private var selectedProvince: String = ""
    private var selectedCity: String = ""

    private val TAG = "EditJobActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_job)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Get intent data
        jobId = intent.getStringExtra("jobId") ?: run {
            Toast.makeText(this, "Error: Job ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        companyId = intent.getStringExtra("companyId") ?: run {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Job"
        }

        // Initialize views and setup
        initializeViews()
        setupDropdowns()
        setupLocationDropdowns()
        setupClickListeners()
        loadJobData()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initializeViews() {
        titleInput = findViewById(R.id.titleInput)
        companyInput = findViewById(R.id.companyInput)
        fieldInput = findViewById(R.id.fieldInput)
        specializationInput = findViewById(R.id.specializationInput)
        typeInput = findViewById(R.id.typeInput)
        provinceInput = findViewById(R.id.provinceInput)
        cityInput = findViewById(R.id.cityInput)
        salaryRangeInput = findViewById(R.id.salaryRangeInput)
        experienceInput = findViewById(R.id.experienceInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        requirementsInput = findViewById(R.id.requirementsInput)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setupDropdowns() {
        // Setup job type dropdown
        val jobTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Remote")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jobTypes)
        typeInput.setAdapter(typeAdapter)
        typeInput.setOnClickListener { typeInput.showDropDown() }

        // Setup salary range dropdown
        val salaryRanges = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryRanges)
        salaryRangeInput.setAdapter(salaryAdapter)
        salaryRangeInput.setOnClickListener { salaryRangeInput.showDropDown() }

        // Setup experience level dropdown
        val experienceLevels = arrayOf("Entry Level", "Mid Level", "Senior Level", "Executive")
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        experienceInput.setAdapter(experienceAdapter)
        experienceInput.setOnClickListener { experienceInput.showDropDown() }

        // Setup field dropdown
        val fieldOptions = FieldCategories.fields.keys.toTypedArray()
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldInput.setAdapter(fieldAdapter)
        fieldInput.setOnClickListener { fieldInput.showDropDown() }

        // Setup field selection listener
        fieldInput.setOnItemClickListener { _, _, _, _ ->
            val selectedField = fieldInput.text.toString()
            updateSpecializationDropdown(selectedField)
        }

        // Initially disable specialization
        specializationInput.isEnabled = false
    }

    private fun updateSpecializationDropdown(field: String) {
        val specializations = FieldCategories.fields[field] ?: listOf()
        if (specializations.isNotEmpty()) {
            val specializationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specializations)
            specializationInput.setAdapter(specializationAdapter)
            specializationInput.isEnabled = true
            specializationInput.setText("", false)
            specializationInput.setOnClickListener { specializationInput.showDropDown() }
        } else {
            specializationInput.setText("")
            specializationInput.isEnabled = false
        }
    }

    private fun setupLocationDropdowns() {
        LocationUtils.setupCascadingLocationSpinners(
            context = this,
            provinceSpinner = provinceInput,
            citySpinner = cityInput
        ) { province, city ->
            selectedProvince = province
            selectedCity = city
            Log.d(TAG, "Location selected: $city, $province")
        }
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveJob()
        }
    }

    private fun loadJobData() {
        db.collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(TAG, "Loading job data: ${document.data}")

                    // Load basic fields
                    titleInput.setText(document.getString("title") ?: "")
                    companyInput.setText(document.getString("companyName") ?: "")
                    descriptionInput.setText(document.getString("description") ?: "")
                    requirementsInput.setText(document.getString("requirements") ?: "")

                    // Load dropdown fields
                    val jobField = document.getString("jobField") ?: ""
                    if (jobField.isNotEmpty()) {
                        fieldInput.setText(jobField, false)
                        updateSpecializationDropdown(jobField)

                        val specialization = document.getString("specialization") ?: ""
                        if (specialization.isNotEmpty()) {
                            specializationInput.setText(specialization, false)
                        }
                    }

                    val jobType = document.getString("jobType") ?: document.getString("type") ?: ""
                    if (jobType.isNotEmpty()) {
                        typeInput.setText(jobType, false)
                    }

                    val salary = document.getString("salary") ?: ""
                    if (salary.isNotEmpty()) {
                        salaryRangeInput.setText(salary, false)
                    }

                    val experienceLevel = document.getString("experienceLevel") ?: ""
                    if (experienceLevel.isNotEmpty()) {
                        experienceInput.setText(experienceLevel, false)
                    }

                    // Load location data
                    val province = document.getString("province") ?: ""
                    val city = document.getString("city") ?: ""

                    if (province.isNotEmpty() || city.isNotEmpty()) {
                        // Reinitialize dropdowns with loaded data
                        LocationUtils.setupCascadingLocationSpinners(
                            context = this@EditJobActivity,
                            provinceSpinner = provinceInput,
                            citySpinner = cityInput,
                            selectedProvince = province,
                            selectedCity = city
                        ) { selectedProv, selectedCit ->
                            selectedProvince = selectedProv
                            selectedCity = selectedCit
                            Log.d(TAG, "Location loaded: $selectedCit, $selectedProv")
                        }
                    }

                } else {
                    Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading job", e)
                Toast.makeText(this, "Error loading job: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    private fun saveJob() {
        // Get all input values
        val title = titleInput.text.toString().trim()
        val companyName = companyInput.text.toString().trim()
        val jobField = fieldInput.text.toString().trim()
        val specialization = specializationInput.text.toString().trim()
        val jobType = typeInput.text.toString().trim()
        val salaryRange = salaryRangeInput.text.toString().trim()
        val experienceLevel = experienceInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val requirements = requirementsInput.text.toString().trim()

        // Validate required fields
        if (title.isEmpty()) {
            titleInput.error = "Job title is required"
            return
        }
        if (companyName.isEmpty()) {
            companyInput.error = "Company name is required"
            return
        }
        if (jobField.isEmpty()) {
            fieldInput.error = "Job field is required"
            return
        }
        if (jobType.isEmpty()) {
            typeInput.error = "Job type is required"
            return
        }
        if (!LocationUtils.isValidLocationSelection(selectedProvince, selectedCity)) {
            Toast.makeText(this, "Please select both province and city", Toast.LENGTH_SHORT).show()
            return
        }
        if (salaryRange.isEmpty()) {
            salaryRangeInput.error = "Salary range is required"
            return
        }
        if (experienceLevel.isEmpty()) {
            experienceInput.error = "Experience level is required"
            return
        }
        if (description.isEmpty()) {
            descriptionInput.error = "Job description is required"
            return
        }
        if (requirements.isEmpty()) {
            requirementsInput.error = "Requirements are required"
            return
        }
        // Create updated job data
        val jobData = mapOf(
            "title" to title,
            "companyName" to companyName,
            "jobField" to jobField,
            "specialization" to specialization,
            "jobType" to jobType,
            "type" to jobType, // Keep both for compatibility
            "province" to selectedProvince,
            "city" to selectedCity,
            "salary" to salaryRange,
            "experienceLevel" to experienceLevel,
            "description" to description,
            "requirements" to requirements,
            "companyId" to companyId,
            "updatedDate" to com.google.firebase.Timestamp.now(),
            "status" to "active"
        )

        Log.d(TAG, "Saving job with data: $jobData")

        // Update the job in Firestore
        db.collection("jobs")
            .document(jobId)
            .update(jobData)
            .addOnSuccessListener {
                Log.d(TAG, "Job updated successfully")
                Toast.makeText(this, "Job updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating job", e)
                Toast.makeText(this, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}