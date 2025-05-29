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
import com.example.jobrec.models.FieldCategories
import java.util.UUID
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
    private lateinit var cityInput: AutoCompleteTextView
    private lateinit var summaryInput: TextInputEditText
    private lateinit var skillsInput: AutoCompleteTextView
    private lateinit var linkedinInput: TextInputEditText
    private lateinit var githubInput: TextInputEditText
    private lateinit var portfolioInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var skillsChipGroup: ChipGroup
    private val selectedSkills = mutableListOf<String>()
    private lateinit var referencesContainer: LinearLayout
    private lateinit var experienceContainer: LinearLayout
    private lateinit var educationContainer: LinearLayout
    private lateinit var addReferenceButton: MaterialButton
    private lateinit var addExperienceButton: MaterialButton
    private lateinit var addEducationButton: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SignupActivity"
    private lateinit var yearsOfExperienceInput: AutoCompleteTextView
    private lateinit var certificateInput: AutoCompleteTextView
    private lateinit var expectedSalaryInput: AutoCompleteTextView
    private lateinit var fieldInput: AutoCompleteTextView
    private lateinit var subFieldInput: AutoCompleteTextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        initializeViews()
        setupToolbar()
        setupClickListeners()
        setupSkillsInput()
        setupDropdowns()
    }
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        nameInput = findViewById(R.id.etName)
        surnameInput = findViewById(R.id.etSurname)
        cellNumberInput = findViewById(R.id.etCellNumber)
        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        provinceInput = findViewById(R.id.provinceInput)
        cityInput = findViewById(R.id.cityInput)
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
        yearsOfExperienceInput = findViewById(R.id.yearsOfExperienceInput)
        certificateInput = findViewById(R.id.certificateInput)
        expectedSalaryInput = findViewById(R.id.expectedSalaryInput)
        fieldInput = findViewById(R.id.fieldInput)
        subFieldInput = findViewById(R.id.subFieldInput)
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
        skillsInput.isEnabled = false
    }
    private fun addSkillChip(skill: String) {
        val chip = Chip(this).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedSkills.remove(skill)
                skillsChipGroup.removeView(this)
            }
        }
        skillsChipGroup.addView(chip)
    }
    private fun addReferenceField() {
        val referenceLayout = layoutInflater.inflate(R.layout.item_reference, referencesContainer, false)
        referenceLayout.findViewById<MaterialButton>(R.id.btnRemoveReference).setOnClickListener {
            referencesContainer.removeView(referenceLayout)
        }
        referencesContainer.addView(referenceLayout)
    }
    private fun addExperienceField() {
        val experienceLayout = layoutInflater.inflate(R.layout.item_experience, experienceContainer, false)
        experienceLayout.findViewById<MaterialButton>(R.id.btnRemoveExperience).setOnClickListener {
            experienceContainer.removeView(experienceLayout)
        }
        experienceContainer.addView(experienceLayout)
    }
    private fun addEducationField() {
        val educationLayout = layoutInflater.inflate(R.layout.item_education, educationContainer, false)
        educationLayout.findViewById<MaterialButton>(R.id.btnRemoveEducation).setOnClickListener {
            educationContainer.removeView(educationLayout)
        }
        educationContainer.addView(educationLayout)
    }
    private fun getSkillsList(): List<String> {
        return selectedSkills.toList()
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
        val province = provinceInput.text.toString().trim()
        val city = cityInput.text.toString().trim()
        val summary = summaryInput.text.toString().trim()
        val linkedin = linkedinInput.text.toString().trim()
        val github = githubInput.text.toString().trim()
        val portfolio = portfolioInput.text.toString().trim()
        val skills = getSkillsList()
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseHelper.getInstance().isEmailExists(email) { emailExists ->
            if (emailExists) {
                runOnUiThread {
                    Toast.makeText(this, "This email is already registered. Please use a different email or login.", Toast.LENGTH_LONG).show()
                    emailInput.error = "Email already in use"
                }
                return@isEmailExists
            }
            val user = User(
                name = name,
                email = email,
                phoneNumber = cellNumber,
                province = province,
                city = city,
                summary = summary,
                skills = skills,
                linkedin = linkedin,
                github = github,
                portfolio = portfolio,
                role = "user",
                yearsOfExperience = yearsOfExperienceInput.text.toString().trim(),
                certificate = certificateInput.text.toString().trim(),
                expectedSalary = expectedSalaryInput.text.toString().trim(),
                field = fieldInput.text.toString().trim(),
                subField = subFieldInput.text.toString().trim()
            )
            FirebaseHelper.getInstance().addUser(user, password) { success, error ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread {
                        if (error?.contains("email address is already in use", ignoreCase = true) == true) {
                            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                            builder.setTitle("Account Recovery")
                            builder.setMessage("It looks like you've previously started registration with this email but didn't complete it. Would you like to continue with this email?")
                            builder.setPositiveButton("Yes") { _, _ ->
                                FirebaseHelper.getInstance().recoverOrCreateUser(user, password) { recoverySuccess, recoveryError ->
                                    if (recoverySuccess) {
                                        runOnUiThread {
                                            Toast.makeText(this, "Registration completed successfully", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, HomeActivity::class.java))
                                            finish()
                                        }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this, "Registration failed: $recoveryError", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                            builder.setNegativeButton("No") { _, _ ->
                                emailInput.error = "Please use a different email"
                                emailInput.requestFocus()
                            }
                            builder.show()
                        } else {
                            Toast.makeText(this, "Registration failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    private fun checkEmailInFirestore(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists in Firestore: $email")
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { usersResult ->
                if (!usersResult.isEmpty) {
                    Log.d(TAG, "Email found in users collection")
                    callback(true)
                    return@addOnSuccessListener
                }
                // Check users collection (already checked above)
                // Skip duplicate check for Users collection
                db.collection("companies")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { companiesResult ->
                        val exists = !companiesResult.isEmpty
                        if (exists) {
                            Log.d(TAG, "Email found in companies collection")
                        } else {
                            Log.d(TAG, "Email not found in any Firestore collection")
                        }
                        callback(exists)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking companies collection", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking users collection", e)
                callback(false)
            }
    }
    private fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists: $email")
        val db = FirebaseFirestore.getInstance()
        logAllUsersAndCompanies()
        checkEmailExistsInAuth(email) { existsInAuth ->
            if (existsInAuth) {
                Log.d(TAG, "Email found in Firebase Authentication: $email")
                callback(true)
                return@checkEmailExistsInAuth
            }
            Log.d(TAG, "Email not found in Firebase Authentication, checking Firestore")
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { usersResult ->
                    if (!usersResult.isEmpty) {
                        Log.d(TAG, "Email found in users collection")
                        callback(true)
                        return@addOnSuccessListener
                    }
                    // Check companies collection directly since users collection already checked above
                    db.collection("companies")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { companiesResult ->
                            val exists = !companiesResult.isEmpty
                            if (exists) {
                                Log.d(TAG, "Email found in companies collection")
                            } else {
                                Log.d(TAG, "Email not found in any collection")
                            }
                            callback(exists)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking companies collection", e)
                            callback(false)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking users collection", e)
                    callback(false)
                }
        }
    }
    private fun checkEmailExistsInAuth(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists in Firebase Authentication: $email")
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    val exists = !signInMethods.isNullOrEmpty()
                    if (exists) {
                        Log.d(TAG, "Email exists in Firebase Authentication: $email")
                        Log.d(TAG, "Sign-in methods: ${signInMethods?.joinToString()}")
                        callback(true)
                    } else {
                        Log.d(TAG, "Email does not exist in Firebase Authentication: $email")
                        checkSimilarEmailsInAuth(email) { hasSimilar ->
                            if (hasSimilar) {
                                Log.d(TAG, "Found similar email in Firebase Auth")
                                callback(true)
                            } else {
                                callback(false)
                            }
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: ""
                    if (errorMessage.contains("email address is already in use", ignoreCase = true)) {
                        Log.w(TAG, "Error suggests email exists despite fetchSignInMethods failure: $errorMessage")
                        callback(true)
                    } else {
                        Log.e(TAG, "Error checking email in Firebase Authentication", task.exception)
                        callback(false)
                    }
                }
            }
    }
    private fun checkSimilarEmailsInAuth(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking for similar emails in Firebase Auth: $email")
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                var foundSimilar = false
                val normalizedEmail = email.lowercase().replace(".", "").replace(" ", "")
                userDocuments.documents.forEach { doc ->
                    val userEmail = doc.getString("email") ?: ""
                    val normalizedUserEmail = userEmail.lowercase().replace(".", "").replace(" ", "")
                    if (normalizedUserEmail.isNotEmpty() &&
                        (normalizedEmail == normalizedUserEmail ||
                         normalizedEmail.contains(normalizedUserEmail) ||
                         normalizedUserEmail.contains(normalizedEmail))) {
                        Log.d(TAG, "Found similar email: $userEmail for input: $email")
                        foundSimilar = true
                    }
                }
                if (!foundSimilar) {
                    db.collection("companies")
                        .get()
                        .addOnSuccessListener { companyDocuments ->
                            companyDocuments.documents.forEach { doc ->
                                val companyEmail = doc.getString("email") ?: ""
                                val normalizedCompanyEmail = companyEmail.lowercase().replace(".", "").replace(" ", "")
                                if (normalizedCompanyEmail.isNotEmpty() &&
                                    (normalizedEmail == normalizedCompanyEmail ||
                                     normalizedEmail.contains(normalizedCompanyEmail) ||
                                     normalizedCompanyEmail.contains(normalizedEmail))) {
                                    Log.d(TAG, "Found similar email in companies: $companyEmail for input: $email")
                                    foundSimilar = true
                                }
                            }
                            callback(foundSimilar)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking companies for similar emails", e)
                            callback(false)
                        }
                } else {
                    callback(foundSimilar)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking for similar emails", e)
                callback(false)
            }
    }
    private fun checkEmailWithDirectAttempt(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Performing direct attempt check for email: $email")
        val tempPassword = UUID.randomUUID().toString().substring(0, 8)
        val tempAuth = FirebaseAuth.getInstance()
        tempAuth.createUserWithEmailAndPassword(email, tempPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email is available (direct check). Deleting temporary user.")
                    val user = tempAuth.currentUser
                    user?.delete()
                        ?.addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Log.d(TAG, "Temporary user deleted successfully")
                            } else {
                                Log.e(TAG, "Failed to delete temporary user", deleteTask.exception)
                            }
                            tempAuth.signOut()
                            callback(false)
                        }
                } else {
                    val errorMessage = task.exception?.message ?: ""
                    val emailInUse = errorMessage.contains("email address is already in use", ignoreCase = true)
                    if (emailInUse) {
                        Log.d(TAG, "Direct check confirms email is already in use: $email")
                        checkEmailInFirestore(email) { existsInFirestore ->
                            if (!existsInFirestore) {
                                Log.d(TAG, "Email exists in Firebase Auth but not in Firestore. This is a recoverable situation.")
                            }
                            callback(true)
                        }
                    } else {
                        Log.e(TAG, "Error in direct email check: $errorMessage")
                        callback(false)
                    }
                }
            }
    }
    private fun checkForSimilarEmails(email: String) {
        Log.d(TAG, "Checking for similar emails to: $email")
        val normalizedEmail = email.lowercase().replace(".", "").replace(" ", "")
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                Log.d(TAG, "======= SIMILAR EMAIL CHECK =======")
                Log.d(TAG, "Normalized input email: $normalizedEmail")
                userDocuments.documents.forEach { doc ->
                    val userEmail = doc.getString("email") ?: ""
                    val normalizedUserEmail = userEmail.lowercase().replace(".", "").replace(" ", "")
                    val similarity = calculateSimilarity(normalizedEmail, normalizedUserEmail)
                    if (similarity > 70) {
                        Log.d(TAG, "Similar email found: $userEmail (${similarity.toInt()}% similar)")
                    }
                }
                db.collection("companies")
                    .get()
                    .addOnSuccessListener { companyDocuments ->
                        companyDocuments.documents.forEach { doc ->
                            val companyEmail = doc.getString("email") ?: ""
                            val normalizedCompanyEmail = companyEmail.lowercase().replace(".", "").replace(" ", "")
                            val similarity = calculateSimilarity(normalizedEmail, normalizedCompanyEmail)
                            if (similarity > 70) {
                                Log.d(TAG, "Similar company email found: $companyEmail (${similarity.toInt()}% similar)")
                            }
                        }
                        Log.d(TAG, "======= END OF SIMILAR EMAIL CHECK =======")
                    }
            }
    }
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return (1.0 - distance.toDouble() / maxLength) * 100
    }
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
    private fun logAllUsersAndCompanies() {
        Log.d(TAG, "======= LOGGING ALL DATABASE USERS AND COMPANIES =======")
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                Log.d(TAG, "Total users in database: ${userDocuments.size()}")
                userDocuments.documents.forEach { doc ->
                    val email = doc.getString("email") ?: "no-email"
                    val name = doc.getString("name") ?: "no-name"
                    val id = doc.id
                    Log.d(TAG, "User: ID=$id, Email=$email, Name=$name")
                }
                db.collection("companies")
                    .get()
                    .addOnSuccessListener { companyDocuments ->
                        Log.d(TAG, "Total companies in database: ${companyDocuments.size()}")
                        companyDocuments.documents.forEach { doc ->
                            val email = doc.getString("email") ?: "no-email"
                            val name = doc.getString("companyName") ?: "no-name"
                            val id = doc.id
                            val userId = doc.getString("userId") ?: "no-userId"
                            Log.d(TAG, "Company: ID=$id, Email=$email, Name=$name, UserId=$userId")
                        }
                        Log.d(TAG, "======= END OF DATABASE LOGGING =======")
                    }
            }
    }
    private fun setupDropdowns() {
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceInput.setAdapter(provinceAdapter)
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val yearsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearsOptions)
        yearsOfExperienceInput.setAdapter(yearsAdapter)
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
        val certificateAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, certificateOptions)
        certificateInput.setAdapter(certificateAdapter)
        val salaryOptions = arrayOf(
            "R0 - R10,000",
            "R10,000 - R20,000",
            "R20,000 - R30,000",
            "R30,000 - R40,000",
            "R40,000 - R50,000",
            "R50,000+"
        )
        val salaryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, salaryOptions)
        expectedSalaryInput.setAdapter(salaryAdapter)
        val fieldOptions = FieldCategories.fields.keys.toTypedArray()
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldInput.setAdapter(fieldAdapter)
        subFieldInput.isEnabled = false
        fieldInput.setOnItemClickListener { _, _, _, _ ->
            val selectedField = fieldInput.text.toString()
            updateSubFieldDropdown(selectedField)
            updateSkillsDropdown(selectedField)
        }
    }
    private fun updateSubFieldDropdown(field: String) {
        val subFields = FieldCategories.fields[field] ?: listOf()
        if (subFields.isNotEmpty()) {
            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subFields)
            subFieldInput.setAdapter(subFieldAdapter)
            subFieldInput.isEnabled = true
            subFieldInput.setText("", false)
        } else {
            subFieldInput.setText("")
            subFieldInput.isEnabled = false
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
            skillsChipGroup.removeAllViews()
        }
    }
}