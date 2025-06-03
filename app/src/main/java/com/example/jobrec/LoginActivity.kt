package com.example.jobrec
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.TextView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jobrec.utils.VerificationStateManager
class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var userTypeRadioGroup: android.widget.RadioGroup
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "LoginActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.signupButton)
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup)
        // Temporarily disabled animations for debugging
        // val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        // val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        // findViewById<View>(R.id.emailInput).startAnimation(slideUp)
        // findViewById<View>(R.id.passwordInput).startAnimation(slideUp)
        // findViewById<View>(R.id.loginButton).startAnimation(slideUp)
        // findViewById<View>(R.id.userTypeRadioGroup).startAnimation(fadeIn)
        // findViewById<View>(R.id.signupButton).startAnimation(fadeIn)
        // Check for pending verification first
        val verificationStateManager = VerificationStateManager.getInstance(this)
        Log.d(TAG, "Checking for pending verification...")
        if (verificationStateManager.hasPendingVerification()) {
            val verificationData = verificationStateManager.getPendingVerificationData()
            Log.d(TAG, "Found pending verification for: ${verificationData?.email}")
            if (verificationData != null) {
                Log.d(TAG, "Redirecting to EmailVerificationActivity")
                // Navigate to verification screen
                val intent = Intent(this, EmailVerificationActivity::class.java)
                intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, verificationData.email)
                intent.putExtra(EmailVerificationActivity.EXTRA_USER_TYPE, verificationData.userType)
                intent.putExtra(EmailVerificationActivity.EXTRA_USER_NAME, verificationData.userName)
                startActivity(intent)
                return
            }
        } else {
            Log.d(TAG, "No pending verification found")
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserTypeAndRedirect(currentUser.email ?: "")
        }
        loginButton.setOnClickListener {
            Log.d(TAG, "LOGIN BUTTON CLICKED!")
            Toast.makeText(this, "Login button clicked!", Toast.LENGTH_SHORT).show()

            // Clear any stuck verification state before login
            VerificationStateManager.getInstance(this).clearPendingVerification()

            loginUser()
        }
        signupButton.setOnClickListener {
            Log.d(TAG, "SIGNUP BUTTON CLICKED!")
            Toast.makeText(this, "Signup button clicked!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UnifiedSignupActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }

        findViewById<TextView>(R.id.forgotPasswordText).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Temporary debug button - long press on forgot password to clear verification state
        findViewById<TextView>(R.id.forgotPasswordText).setOnLongClickListener {
            Log.d(TAG, "CLEARING VERIFICATION STATE (DEBUG)")
            Toast.makeText(this, "Clearing verification state...", Toast.LENGTH_SHORT).show()
            VerificationStateManager.getInstance(this).clearPendingVerification()
            recreate() // Restart the activity
            true
        }
    }
    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val selectedUserType = when (userTypeRadioGroup.checkedRadioButtonId) {
            R.id.studentRadioButton -> "student"
            R.id.companyRadioButton -> "company"
            R.id.adminRadioButton -> "admin"
            else -> "student"
        }
        if (selectedUserType == "admin") {
            if (email == "admin@careerworx.com" && password == "admin123") {
                // Authenticate admin with Firebase Auth
                Toast.makeText(this, "Logging in as admin...", Toast.LENGTH_SHORT).show()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Log.d(TAG, "Admin authenticated successfully")
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Admin authentication failed", e)
                        // If admin user doesn't exist in Firebase Auth, create it
                        if (e.message?.contains("no user record", ignoreCase = true) == true ||
                            e.message?.contains("incorrect, malformed or has expired", ignoreCase = true) == true ||
                            e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            Log.d(TAG, "Admin user doesn't exist in Firebase Auth, creating it...")
                            createAdminUser(email, password)
                        } else {
                            Toast.makeText(this, "Admin authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                return
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (validateInput(email, password)) {
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            // Try to login first
            FirebaseHelper.getInstance().checkUser(email, password) { success, userType, error ->
                if (success) {
                    val actualUserType = userType ?: "unknown"

                    // Login successful - now check if verification is needed
                    Log.d(TAG, "Login successful for $email, checking verification status")
                    FirebaseHelper.getInstance().getCurrentUserVerificationStatus(email) { needsVerification, userType, verificationCode ->
                        runOnUiThread {
                            Log.d(TAG, "Verification status: needsVerification=$needsVerification, userType=$userType")
                            if (needsVerification) {
                                Toast.makeText(this, "Login successful! Please verify your email to continue using the app.", Toast.LENGTH_LONG).show()

                                // Save verification state
                                val verificationStateManager = VerificationStateManager.getInstance(this)
                                val userTypeForVerification = if (actualUserType == "user") "student" else actualUserType
                                verificationStateManager.setPendingVerification(email, userTypeForVerification, "User")

                                // Navigate to email verification
                                val intent = Intent(this, EmailVerificationActivity::class.java)
                                intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
                                intent.putExtra(EmailVerificationActivity.EXTRA_USER_TYPE, userTypeForVerification)
                                intent.putExtra(EmailVerificationActivity.EXTRA_USER_NAME, "User") // We'll get the actual name later
                                intent.putExtra("fromLogin", true)
                                startActivity(intent)
                                // DO NOT finish() here - keep login activity alive so user can return
                            } else {
                                // User is verified, proceed to main app
                                Log.d(TAG, "User is verified, proceeding to main app")
                                proceedToMainApp(actualUserType, email, selectedUserType)
                            }
                        }
                    }

                    // This code is now handled by proceedToMainApp function above
                } else {
                    // If login failed, check if it's a test account and try to create it
                    if (isTestAccount(email, password)) {
                        runOnUiThread {
                            Toast.makeText(this, "Creating test account...", Toast.LENGTH_SHORT).show()
                        }
                        createTestAccount(email, password, selectedUserType)
                    } else {
                        runOnUiThread {
                            val errorMessage = when {
                                error?.contains("password is invalid", ignoreCase = true) == true ->
                                    "Incorrect password. Please try again."
                                error?.contains("no user record", ignoreCase = true) == true ->
                                    "This email is not registered. Please sign up first."
                                error?.contains("blocked", ignoreCase = true) == true ->
                                    "This account has been temporarily blocked due to too many failed login attempts. Please try again later."
                                else -> "Login failed: $error"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                            if (errorMessage.contains("password", ignoreCase = true)) {
                                passwordInput.error = "Incorrect password"
                            } else if (errorMessage.contains("email", ignoreCase = true)) {
                                emailInput.error = "Email not registered"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun proceedToMainApp(actualUserType: String, email: String, selectedUserType: String) {
        when {
            selectedUserType == "student" && (actualUserType == "user" || actualUserType == "unknown") -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
            selectedUserType == "company" && (actualUserType == "company" || actualUserType == "unknown") -> {
                val intent = Intent(this, CompanyDashboardActivityNew::class.java)
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this, "This account is not registered as a ${selectedUserType}. Please select the correct user type.", Toast.LENGTH_LONG).show()
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
                    } else {
                        Log.d(TAG, "Email does not exist in Firebase Authentication: $email")
                    }
                    callback(exists)
                } else {
                    Log.e(TAG, "Error checking email in Firebase Authentication", task.exception)
                    callback(false)
                }
            }
    }
    private fun checkUserExistsInFirestore(email: String) {
        Log.d(TAG, "Checking if user exists in Firestore with email: $email")
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { userDocuments ->
                if (!userDocuments.isEmpty) {
                    Log.d(TAG, "User found in 'users' collection with email: $email")
                    userDocuments.documents.forEach { doc ->
                        Log.d(TAG, "User document ID: ${doc.id}")
                        Log.d(TAG, "User data: ${doc.data}")
                    }
                } else {
                    Log.d(TAG, "User NOT found in 'users' collection with email: $email")
                    db.collection("companies")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { companyDocuments ->
                            if (!companyDocuments.isEmpty) {
                                Log.d(TAG, "User found in 'companies' collection with email: $email")
                                companyDocuments.documents.forEach { doc ->
                                    Log.d(TAG, "Company document ID: ${doc.id}")
                                    Log.d(TAG, "Company data: ${doc.data}")
                                }
                            } else {
                                Log.d(TAG, "User NOT found in 'companies' collection with email: $email")
                                Log.e(TAG, "User does not exist in Firestore with email: $email")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking companies collection", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking users collection", e)
            }
    }
    private fun logAllUsersAndCompanies() {
        Log.d(TAG, "======= LOGGING ALL DATABASE USERS AND COMPANIES =======")
        db.collection("users")
            .get()
            .addOnSuccessListener { userDocuments ->
                Log.d(TAG, "Total users in database: ${userDocuments.size()}")
                userDocuments.documents.forEach { doc ->
                    val email = doc.getString("email") ?: "no-email"
                    val name = doc.getString("name") ?: "no-name"
                    val id = doc.id
                    Log.d(TAG, "User: ID=$id, Email=$email, Name=$name")
                    doc.data?.forEach { (key, value) ->
                        Log.d(TAG, "  - $key: $value")
                    }
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
                            doc.data?.forEach { (key, value) ->
                                Log.d(TAG, "  - $key: $value")
                            }
                        }
                        Log.d(TAG, "======= END OF USERS AND COMPANIES LOGGING =======")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching companies", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching users", e)
            }
    }
    private fun logAllConversations() {
        Log.d(TAG, "======= LOGGING ALL CONVERSATIONS =======")
        db.collection("conversations")
            .get()
            .addOnSuccessListener { conversationDocuments ->
                Log.d(TAG, "Total conversations in database: ${conversationDocuments.size()}")
                conversationDocuments.documents.forEach { doc ->
                    val id = doc.id
                    val companyId = doc.getString("companyId") ?: "no-companyId"
                    val candidateId = doc.getString("candidateId") ?: "no-candidateId"
                    val companyName = doc.getString("companyName") ?: "no-companyName"
                    val candidateName = doc.getString("candidateName") ?: "no-candidateName"
                    Log.d(TAG, "Conversation: ID=$id")
                    Log.d(TAG, "  - Company: ID=$companyId, Name=$companyName")
                    Log.d(TAG, "  - Candidate: ID=$candidateId, Name=$candidateName")
                    doc.data?.forEach { (key, value) ->
                        if (key !in listOf("companyId", "candidateId", "companyName", "candidateName")) {
                            Log.d(TAG, "  - $key: $value")
                        }
                    }
                    db.collection("messages")
                        .whereEqualTo("conversationId", id)
                        .get()
                        .addOnSuccessListener { messageDocuments ->
                            Log.d(TAG, "  - Total messages: ${messageDocuments.size()}")
                            if (messageDocuments.size() > 0) {
                                messageDocuments.documents.forEach { messageDoc ->
                                    val messageId = messageDoc.id
                                    val senderId = messageDoc.getString("senderId") ?: "no-senderId"
                                    val receiverId = messageDoc.getString("receiverId") ?: "no-receiverId"
                                    val content = messageDoc.getString("content") ?: "no-content"
                                    Log.d(TAG, "    Message: ID=$messageId")
                                    Log.d(TAG, "      - Sender: $senderId")
                                    Log.d(TAG, "      - Receiver: $receiverId")
                                    Log.d(TAG, "      - Content: $content")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching messages for conversation $id", e)
                        }
                }
                Log.d(TAG, "======= END OF CONVERSATIONS LOGGING =======")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching conversations", e)
            }
    }
    private fun checkUserTypeAndRedirect(email: String) {
        Log.d(TAG, "Checking user type for email: $email")
        Log.d(TAG, "Current Firebase Auth user ID: ${auth.currentUser?.uid}")
        val userEmail = email.lowercase()
        Log.d(TAG, "Looking for company with email (lowercase): $userEmail")
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Total companies in database: ${documents.size()}")
                val companyDoc = documents.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }
                if (companyDoc != null) {
                    val registrationNumber = companyDoc.getString("registrationNumber")
                    val companyName = companyDoc.getString("companyName") ?: "unknown"
                    val userId = companyDoc.getString("userId")
                    if (registrationNumber != null) {
                        Log.d(TAG, "User is a company, redirecting to CompanyDashboardActivity")
                        Log.d(TAG, "Company details - Registration Number: $registrationNumber, Name: $companyName, UserId: $userId")
                        if (userId == null) {
                            val currentUserId = auth.currentUser?.uid
                            if (currentUserId != null) {
                                db.collection("companies")
                                    .document(registrationNumber)
                                    .update("userId", currentUserId)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Updated company document with userId: $currentUserId")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to update company with userId", e)
                                    }
                            }
                        }
                        val intent = Intent(this, CompanyDashboardActivityNew::class.java)
                        intent.putExtra("companyId", registrationNumber)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e(TAG, "Registration number not found in company document")
                        Toast.makeText(this, "Error: Company data incomplete", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    Log.d(TAG, "Not a company, checking if it's a regular user")
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { userDocuments ->
                            Log.d(TAG, "User query result size: ${userDocuments.size()}")
                            if (!userDocuments.isEmpty) {
                                val userDoc = userDocuments.documents[0]
                                val userId = auth.currentUser?.uid
                                val userName = userDoc.getString("name") ?: "unknown"
                                Log.d(TAG, "User is a job seeker, redirecting to HomeActivity")
                                Log.d(TAG, "User details - ID: $userId, Name: $userName, DocID: ${userDoc.id}")
                                val intent = Intent(this, HomeActivity::class.java)
                                intent.putExtra("userId", userId)
                                startActivity(intent)
                                finish()
                            } else {
                                Log.e(TAG, "User not found in any collection")
                                Log.e(TAG, "Firebase Auth user exists but no matching Firestore document")
                                Log.e(TAG, "Firebase Auth user ID: ${auth.currentUser?.uid}")
                                Log.e(TAG, "Firebase Auth user email: ${auth.currentUser?.email}")
                                logAllUsersAndCompanies()
                                Toast.makeText(this, "User not found in database. Please try again or contact support.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking user collection", e)
                            Log.e(TAG, "Error details: ${e.message}")
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking company collection", e)
                Log.e(TAG, "Error details: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    private fun createAdminUser(email: String, password: String) {
        Log.d(TAG, "Creating admin user in Firebase Auth")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "Admin user created successfully in Firebase Auth")
                val userId = authResult.user?.uid
                if (userId != null) {
                    Log.d(TAG, "Admin user ID: $userId")
                    Toast.makeText(this, "Admin user created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
                    finish()
                } else {
                    Log.e(TAG, "Failed to get admin user ID after creation")
                    Toast.makeText(this, "Error: Failed to get admin user ID", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating admin user in Firebase Auth", e)
                Toast.makeText(this, "Error creating admin user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }
        return true
    }

    private fun isTestAccount(email: String, password: String): Boolean {
        val testAccounts = mapOf(
            "john.doe@example.com" to "password123",
            "jane.smith@example.com" to "password123",
            "hr@techcorp.com" to "password123",
            "hr@businesssolutions.com" to "password123"
        )
        return testAccounts[email.lowercase()] == password
    }

    private fun createTestAccount(email: String, password: String, userType: String) {
        Log.d(TAG, "Creating test account for $email as $userType")

        when (userType) {
            "student" -> {
                val user = when (email.lowercase()) {
                    "john.doe@example.com" -> User(
                        id = "",
                        name = "John",
                        surname = "Doe",
                        email = email,
                        field = "Technology",
                        subField = "Software Development",
                        education = listOf(),
                        experience = listOf(),
                        skills = listOf(),
                        yearsOfExperience = "2-3 years",
                        province = "Western Cape",
                        city = "Cape Town"
                    )
                    "jane.smith@example.com" -> User(
                        id = "",
                        name = "Jane",
                        surname = "Smith",
                        email = email,
                        field = "Business",
                        subField = "Marketing",
                        education = listOf(),
                        experience = listOf(),
                        skills = listOf(),
                        yearsOfExperience = "1-2 years",
                        province = "Gauteng",
                        city = "Johannesburg"
                    )
                    else -> return
                }

                FirebaseHelper.getInstance().addUser(user, password) { success, error ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Test student account created! Please login again.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to create test account: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            "company" -> {
                val company = when (email.lowercase()) {
                    "hr@techcorp.com" -> Company(
                        id = "REG001",
                        companyName = "TechCorp",
                        registrationNumber = "REG001",
                        industry = "Technology",
                        companySize = "50-100",
                        location = "Cape Town",
                        website = "https://techcorp.com",
                        description = "Leading technology company",
                        contactPersonName = "HR Manager",
                        contactPersonEmail = email,
                        contactPersonPhone = "+27123456789",
                        email = email
                    )
                    "hr@businesssolutions.com" -> Company(
                        id = "REG002",
                        companyName = "BusinessSolutions",
                        registrationNumber = "REG002",
                        industry = "Consulting",
                        companySize = "20-50",
                        location = "Johannesburg",
                        website = "https://businesssolutions.com",
                        description = "Business consulting firm",
                        contactPersonName = "HR Manager",
                        contactPersonEmail = email,
                        contactPersonPhone = "+27123456789",
                        email = email
                    )
                    else -> return
                }

                FirebaseHelper.getInstance().addCompany(company, password) { success, error ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Test company account created! Please login again.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to create test account: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}