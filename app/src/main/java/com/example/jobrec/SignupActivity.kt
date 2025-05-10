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
    private lateinit var yearsOfExperienceInput: AutoCompleteTextView
    private lateinit var certificateInput: AutoCompleteTextView
    private lateinit var expectedSalaryInput: AutoCompleteTextView
    private lateinit var fieldInput: AutoCompleteTextView
    private lateinit var subFieldInput: AutoCompleteTextView

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
        setupDropdowns()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        nameInput = findViewById(R.id.etName)
        surnameInput = findViewById(R.id.etSurname)
        cellNumberInput = findViewById(R.id.etCellNumber)
        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        provinceInput = findViewById(R.id.provinceInput) // Added missing initialization
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
        val province = provinceInput.text.toString().trim()
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

        // Check if the email exists in Firebase Authentication or Firestore
        FirebaseHelper.getInstance().isEmailExists(email) { emailExists ->
            if (emailExists) {
                runOnUiThread {
                    Toast.makeText(this, "This email is already registered. Please use a different email or login.", Toast.LENGTH_LONG).show()
                    emailInput.error = "Email already in use"
                }
                return@isEmailExists
            }

            // Create user object
            val user = User(
                name = name,
                email = email,
                phoneNumber = cellNumber,
                province = province,
                address = address,
                summary = summary,
                skills = skills,
                linkedin = linkedin,
                github = github,
                portfolio = portfolio,
                role = "user", // Set default role
                yearsOfExperience = yearsOfExperienceInput.text.toString().trim(),
                certificate = certificateInput.text.toString().trim(),
                expectedSalary = expectedSalaryInput.text.toString().trim(),
                field = fieldInput.text.toString().trim(),
                subField = subFieldInput.text.toString().trim()
            )

            // Register the user
            FirebaseHelper.getInstance().addUser(user, password) { success, error ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread {
                        // Check if the error is about email already in use
                        if (error?.contains("email address is already in use", ignoreCase = true) == true) {
                            // Show a dialog asking if the user wants to continue with this email
                            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                            builder.setTitle("Account Recovery")
                            builder.setMessage("It looks like you've previously started registration with this email but didn't complete it. Would you like to continue with this email?")

                            builder.setPositiveButton("Yes") { _, _ ->
                                // Use our recovery method to handle this case
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
                                // User doesn't want to use this email
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

        // Check in users collection (lowercase)
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { usersResult ->
                if (!usersResult.isEmpty) {
                    Log.d(TAG, "Email found in users collection")
                    callback(true)
                    return@addOnSuccessListener
                }

                // If not found in users, check in Users collection (uppercase)
                db.collection("Users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { usersCapitalResult ->
                        if (!usersCapitalResult.isEmpty) {
                            Log.d(TAG, "Email found in Users collection")
                            callback(true)
                            return@addOnSuccessListener
                        }

                        // If not found in Users, check in companies collection
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
                                callback(false) // Assume email doesn't exist if there's an error
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking Users collection", e)
                        // Continue to check companies collection
                        db.collection("companies")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { companiesResult ->
                                callback(!companiesResult.isEmpty)
                            }
                            .addOnFailureListener { e2 ->
                                Log.e(TAG, "Error checking companies collection", e2)
                                callback(false) // Assume email doesn't exist if there's an error
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking users collection", e)
                callback(false) // Assume email doesn't exist if there's an error
            }
    }

    private fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists: $email")
        val db = FirebaseFirestore.getInstance()

        // First, log all users in the database for debugging
        logAllUsersAndCompanies()

        // Check if the email exists in Firebase Authentication
        checkEmailExistsInAuth(email) { existsInAuth ->
            if (existsInAuth) {
                Log.d(TAG, "Email found in Firebase Authentication: $email")
                callback(true)
                return@checkEmailExistsInAuth
            }

            Log.d(TAG, "Email not found in Firebase Authentication, checking Firestore")

            // Check in users collection (lowercase)
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { usersResult ->
                    if (!usersResult.isEmpty) {
                        Log.d(TAG, "Email found in users collection")
                        callback(true)
                        return@addOnSuccessListener
                    }

                    // If not found in users, check in Users collection (uppercase)
                    db.collection("Users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { usersCapitalResult ->
                            if (!usersCapitalResult.isEmpty) {
                                Log.d(TAG, "Email found in Users collection")
                                callback(true)
                                return@addOnSuccessListener
                            }

                            // If not found in Users, check in companies collection
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
                                    callback(false) // Assume email doesn't exist if there's an error
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking Users collection", e)
                            // Continue to check companies collection
                            db.collection("companies")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { companiesResult ->
                                    callback(!companiesResult.isEmpty)
                                }
                                .addOnFailureListener { e2 ->
                                    Log.e(TAG, "Error checking companies collection", e2)
                                    callback(false) // Assume email doesn't exist if there's an error
                                }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking users collection", e)
                    callback(false) // Assume email doesn't exist if there's an error
                }
        }
    }

    private fun checkEmailExistsInAuth(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists in Firebase Authentication: $email")

        // Use the fetchSignInMethodsForEmail method to check if the email is registered
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

                        // Additional check for similar emails in Firebase Auth
                        // This is a workaround for potential Firebase Auth inconsistencies
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
                    // Check if the error message indicates the email is already in use
                    val errorMessage = task.exception?.message ?: ""
                    if (errorMessage.contains("email address is already in use", ignoreCase = true)) {
                        Log.w(TAG, "Error suggests email exists despite fetchSignInMethods failure: $errorMessage")
                        callback(true)
                    } else {
                        Log.e(TAG, "Error checking email in Firebase Authentication", task.exception)
                        // If there's an error, we'll be cautious and check Firestore anyway
                        callback(false)
                    }
                }
            }
    }

    private fun checkSimilarEmailsInAuth(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking for similar emails in Firebase Auth: $email")

        // Get all users from Firestore to check for similar emails
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                var foundSimilar = false
                val normalizedEmail = email.lowercase().replace(".", "").replace(" ", "")

                userDocuments.documents.forEach { doc ->
                    val userEmail = doc.getString("email") ?: ""
                    val normalizedUserEmail = userEmail.lowercase().replace(".", "").replace(" ", "")

                    // Check if emails are similar (ignoring dots and case)
                    if (normalizedUserEmail.isNotEmpty() &&
                        (normalizedEmail == normalizedUserEmail ||
                         normalizedEmail.contains(normalizedUserEmail) ||
                         normalizedUserEmail.contains(normalizedEmail))) {
                        Log.d(TAG, "Found similar email: $userEmail for input: $email")
                        foundSimilar = true
                    }
                }

                // If not found in users, also check companies collection
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

    /**
     * Check if an email is already in use by attempting to create a temporary user
     * This is more reliable than fetchSignInMethodsForEmail in some cases
     */
    private fun checkEmailWithDirectAttempt(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Performing direct attempt check for email: $email")

        // Create a temporary random password
        val tempPassword = UUID.randomUUID().toString().substring(0, 8)

        // Try to create a user with this email
        val tempAuth = FirebaseAuth.getInstance()
        tempAuth.createUserWithEmailAndPassword(email, tempPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email is available, delete the temporary user
                    Log.d(TAG, "Email is available (direct check). Deleting temporary user.")
                    val user = tempAuth.currentUser
                    user?.delete()
                        ?.addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Log.d(TAG, "Temporary user deleted successfully")
                            } else {
                                Log.e(TAG, "Failed to delete temporary user", deleteTask.exception)
                            }
                            // Sign out from the temporary auth
                            tempAuth.signOut()
                            callback(false) // Email is not in use
                        }
                } else {
                    // Check if the error is because the email is already in use
                    val errorMessage = task.exception?.message ?: ""
                    val emailInUse = errorMessage.contains("email address is already in use", ignoreCase = true)

                    if (emailInUse) {
                        Log.d(TAG, "Direct check confirms email is already in use: $email")

                        // Check if this email exists in Firestore
                        checkEmailInFirestore(email) { existsInFirestore ->
                            if (!existsInFirestore) {
                                Log.d(TAG, "Email exists in Firebase Auth but not in Firestore. This is a recoverable situation.")
                                // We'll still return true to prevent normal registration flow,
                                // but we'll handle this special case in the registration process
                            }
                            callback(true) // Email is in use in Firebase Auth
                        }
                    } else {
                        Log.e(TAG, "Error in direct email check: $errorMessage")
                        callback(false) // Some other error occurred
                    }
                }
            }
    }

    /**
     * Check for similar emails in the database and log them for debugging
     */
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

                    // Calculate similarity percentage
                    val similarity = calculateSimilarity(normalizedEmail, normalizedUserEmail)

                    if (similarity > 70) { // 70% similarity threshold
                        Log.d(TAG, "Similar email found: $userEmail (${similarity.toInt()}% similar)")
                    }
                }

                // Also check companies
                db.collection("companies")
                    .get()
                    .addOnSuccessListener { companyDocuments ->
                        companyDocuments.documents.forEach { doc ->
                            val companyEmail = doc.getString("email") ?: ""
                            val normalizedCompanyEmail = companyEmail.lowercase().replace(".", "").replace(" ", "")

                            // Calculate similarity percentage
                            val similarity = calculateSimilarity(normalizedEmail, normalizedCompanyEmail)

                            if (similarity > 70) { // 70% similarity threshold
                                Log.d(TAG, "Similar company email found: $companyEmail (${similarity.toInt()}% similar)")
                            }
                        }
                        Log.d(TAG, "======= END OF SIMILAR EMAIL CHECK =======")
                    }
            }
    }

    /**
     * Calculate similarity percentage between two strings
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        // Simple implementation using Levenshtein distance
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)

        return (1.0 - distance.toDouble() / maxLength) * 100
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        // Create a matrix of size (m+1) x (n+1)
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize first row and column
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        // Fill the matrix
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }

    private fun logAllUsersAndCompanies() {
        Log.d(TAG, "======= LOGGING ALL DATABASE USERS AND COMPANIES =======")
        val db = FirebaseFirestore.getInstance()

        // Log all users
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

                // Log all companies after users are logged
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
        // Province options
        val provinces = arrayOf(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal",
            "Limpopo", "Mpumalanga", "Northern Cape", "North West", "Western Cape"
        )
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        provinceInput.setAdapter(provinceAdapter)

        // Years of Experience options
        val yearsOptions = arrayOf("0-1 years", "1-3 years", "3-5 years", "5-10 years", "10+ years")
        val yearsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearsOptions)
        yearsOfExperienceInput.setAdapter(yearsAdapter)

        // Certificate options
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

        // Expected Salary options
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

        // Field options
        val fieldOptions = arrayOf(
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
        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fieldOptions)
        fieldInput.setAdapter(fieldAdapter)

        // Initially disable subfield dropdown
        subFieldInput.isEnabled = false

        // Setup field selection listener to update subfields
        fieldInput.setOnItemClickListener { _, _, _, _ ->
            val selectedField = fieldInput.text.toString()
            updateSubFieldDropdown(selectedField)
        }
    }

    private fun updateSubFieldDropdown(field: String) {
        // Get subcategories for the selected field from FieldCategories
        val subFields = FieldCategories.fields[field] ?: listOf()

        if (subFields.isNotEmpty()) {
            val subFieldAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subFields)
            subFieldInput.setAdapter(subFieldAdapter)
            subFieldInput.isEnabled = true
            subFieldInput.setText("", false) // Clear previous selection
        } else {
            // If no subcategories exist for this field
            subFieldInput.setText("")
            subFieldInput.isEnabled = false
        }
    }
}