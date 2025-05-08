package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

class SignupActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var cellNumberInput: TextInputEditText
    private lateinit var provinceInput: AutoCompleteTextView
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var addressInput: TextInputEditText
    private lateinit var summaryInput: TextInputEditText
    private lateinit var skillsInput: TextInputEditText
    private lateinit var linkedinInput: TextInputEditText
    private lateinit var githubInput: TextInputEditText
    private lateinit var portfolioInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var referencesContainer: LinearLayout
    private lateinit var experienceContainer: LinearLayout
    private lateinit var educationContainer: LinearLayout
    private lateinit var addReferenceButton: MaterialButton
    private lateinit var addExperienceButton: MaterialButton
    private lateinit var addEducationButton: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()
        setupToolbar()
        setupClickListeners()
        setupSkillsInput()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        nameInput = findViewById(R.id.etName)
        surnameInput = findViewById(R.id.etSurname)
        cellNumberInput = findViewById(R.id.etCellNumber)
        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        addressInput = findViewById(R.id.etAddress)
        summaryInput = findViewById(R.id.etSummary)
        skillsInput = findViewById(R.id.etSkills)
        linkedinInput = findViewById(R.id.etLinkedin)
        githubInput = findViewById(R.id.etGithub)
        portfolioInput = findViewById(R.id.etPortfolio)
        registerButton = findViewById(R.id.btnRegister)
        skillsChipGroup = findViewById(R.id.skillsChipGroup)
        referencesContainer = findViewById(R.id.referencesContainer)
        experienceContainer = findViewById(R.id.experienceContainer)
        educationContainer = findViewById(R.id.educationContainer)
        addReferenceButton = findViewById(R.id.btnAddReference)
        addExperienceButton = findViewById(R.id.btnAddExperience)
        addEducationButton = findViewById(R.id.btnAddEducation)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            registerUser()
        }

        addReferenceButton.setOnClickListener {
            addReferenceField()
        }

        addExperienceButton.setOnClickListener {
            addExperienceField()
        }

        addEducationButton.setOnClickListener {
            addEducationField()
        }
    }

    private fun setupSkillsInput() {
        skillsInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val skill = skillsInput.text.toString().trim()
                if (skill.isNotEmpty()) {
                    addSkillChip(skill)
                    skillsInput.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addSkillChip(skill: String) {
        val chip = Chip(this).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                skillsChipGroup.removeView(this)
            }
        }
        skillsChipGroup.addView(chip)
    }

    private fun addReferenceField() {
        val referenceLayout = layoutInflater.inflate(R.layout.item_reference, referencesContainer, false)
        
        // Set up remove button
        referenceLayout.findViewById<MaterialButton>(R.id.btnRemoveReference).setOnClickListener {
            referencesContainer.removeView(referenceLayout)
        }
        
        referencesContainer.addView(referenceLayout)
    }

    private fun addExperienceField() {
        val experienceLayout = layoutInflater.inflate(R.layout.item_experience, experienceContainer, false)
        
        // Set up remove button
        experienceLayout.findViewById<MaterialButton>(R.id.btnRemoveExperience).setOnClickListener {
            experienceContainer.removeView(experienceLayout)
        }
        
        experienceContainer.addView(experienceLayout)
    }

    private fun addEducationField() {
        val educationLayout = layoutInflater.inflate(R.layout.item_education, educationContainer, false)
        
        // Set up remove button
        educationLayout.findViewById<MaterialButton>(R.id.btnRemoveEducation).setOnClickListener {
            educationContainer.removeView(educationLayout)
        }
        
        educationContainer.addView(educationLayout)
    }

    private fun getSkillsList(): List<String> {
        val skills = mutableListOf<String>()
        for (i in 0 until skillsChipGroup.childCount) {
            val chip = skillsChipGroup.getChildAt(i) as? Chip
            chip?.text?.toString()?.let { skills.add(it) }
        }
        return skills
    }

    private fun getReferencesList(): List<Map<String, String>> {
        val references = mutableListOf<Map<String, String>>()
        for (i in 0 until referencesContainer.childCount) {
            val referenceView = referencesContainer.getChildAt(i)
            val name = referenceView.findViewById<TextInputEditText>(R.id.etReferenceName).text.toString()
            val position = referenceView.findViewById<TextInputEditText>(R.id.etReferencePosition).text.toString()
            val company = referenceView.findViewById<TextInputEditText>(R.id.etReferenceCompany).text.toString()
            val email = referenceView.findViewById<TextInputEditText>(R.id.etReferenceEmail).text.toString()
            val phone = referenceView.findViewById<TextInputEditText>(R.id.etReferencePhone).text.toString()
            
            if (name.isNotEmpty() || position.isNotEmpty() || company.isNotEmpty() || email.isNotEmpty() || phone.isNotEmpty()) {
                references.add(mapOf(
                    "name" to name,
                    "position" to position,
                    "company" to company,
                    "email" to email,
                    "phone" to phone
                ))
            }
        }
        return references
    }

    private fun getExperienceList(): List<Map<String, String>> {
        val experiences = mutableListOf<Map<String, String>>()
        for (i in 0 until experienceContainer.childCount) {
            val experienceView = experienceContainer.getChildAt(i)
            val title = experienceView.findViewById<TextInputEditText>(R.id.etExperienceTitle).text.toString()
            val company = experienceView.findViewById<TextInputEditText>(R.id.etExperienceCompany).text.toString()
            val startDate = experienceView.findViewById<TextInputEditText>(R.id.etExperienceStartDate).text.toString()
            val endDate = experienceView.findViewById<TextInputEditText>(R.id.etExperienceEndDate).text.toString()
            val description = experienceView.findViewById<TextInputEditText>(R.id.etExperienceDescription).text.toString()
            
            if (title.isNotEmpty() || company.isNotEmpty() || startDate.isNotEmpty() || endDate.isNotEmpty() || description.isNotEmpty()) {
                experiences.add(mapOf(
                    "title" to title,
                    "company" to company,
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "description" to description
                ))
            }
        }
        return experiences
    }

    private fun getEducationList(): List<Map<String, String>> {
        val educations = mutableListOf<Map<String, String>>()
        for (i in 0 until educationContainer.childCount) {
            val educationView = educationContainer.getChildAt(i)
            val institution = educationView.findViewById<TextInputEditText>(R.id.etEducationInstitution).text.toString()
            val degree = educationView.findViewById<TextInputEditText>(R.id.etEducationDegree).text.toString()
            val startDate = educationView.findViewById<TextInputEditText>(R.id.etEducationStartDate).text.toString()
            val endDate = educationView.findViewById<TextInputEditText>(R.id.etEducationEndDate).text.toString()
            val description = educationView.findViewById<TextInputEditText>(R.id.etEducationDescription).text.toString()
            
            if (institution.isNotEmpty() || degree.isNotEmpty() || startDate.isNotEmpty() || endDate.isNotEmpty() || description.isNotEmpty()) {
                educations.add(mapOf(
                    "institution" to institution,
                    "degree" to degree,
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "description" to description
                ))
            }
        }
        return educations
    }

    private fun registerUser() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val cellNumber = cellNumberInput.text.toString().trim()
        val address = addressInput.text.toString().trim()
        val summary = summaryInput.text.toString().trim()
        val linkedin = linkedinInput.text.toString().trim()
        val github = githubInput.text.toString().trim()
        val portfolio = portfolioInput.text.toString().trim()
        val skills = getSkillsList()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create user object
        val user = User(
            name = name,
            email = email,
            phoneNumber = cellNumber,
            address = address,
            summary = summary,
            skills = skills,
            linkedin = linkedin,
            github = github,
            portfolio = portfolio,
            role = "user" // Set default role
        )

        // Add user to Firebase
        FirebaseHelper.getInstance().addUser(user, password) { success, error ->
            if (success) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Registration failed: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}