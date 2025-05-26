package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CompanySignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "CompanySignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_signup)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Company Signup"
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            registerCompany()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun registerCompany() {
        // Get all input values
        val companyName = findViewById<TextInputEditText>(R.id.companyNameInput).text.toString()
        val registrationNumber = findViewById<TextInputEditText>(R.id.registrationNumberInput).text.toString()
        val industry = findViewById<TextInputEditText>(R.id.industryInput).text.toString()
        val companySize = findViewById<TextInputEditText>(R.id.companySizeInput).text.toString()
        val location = findViewById<TextInputEditText>(R.id.locationInput).text.toString()
        val website = findViewById<TextInputEditText>(R.id.websiteInput).text.toString()
        val description = findViewById<TextInputEditText>(R.id.descriptionInput).text.toString()
        val contactPersonName = findViewById<TextInputEditText>(R.id.contactPersonNameInput).text.toString()
        val contactPersonEmail = findViewById<TextInputEditText>(R.id.contactPersonEmailInput).text.toString()
        val contactPersonPhone = findViewById<TextInputEditText>(R.id.contactPersonPhoneInput).text.toString()
        val email = findViewById<TextInputEditText>(R.id.emailInput).text.toString()
        val password = findViewById<TextInputEditText>(R.id.passwordInput).text.toString()

        // Validate inputs
        if (companyName.isEmpty() || registrationNumber.isEmpty() || industry.isEmpty() ||
            companySize.isEmpty() || location.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the email exists in Firebase Authentication or Firestore
        FirebaseHelper.getInstance().isEmailExists(email) { emailExists ->
            if (emailExists) {
                runOnUiThread {
                    Toast.makeText(this, "This email is already registered. Please use a different email or login.", Toast.LENGTH_LONG).show()
                    findViewById<TextInputEditText>(R.id.emailInput).error = "Email already in use"
                }
                return@isEmailExists
            }

            // Check if registration number already exists
            checkRegistrationNumberExists(registrationNumber) { regNumberExists ->
                if (regNumberExists) {
                    runOnUiThread {
                        Toast.makeText(this, "This registration number is already registered. Please check and try again.", Toast.LENGTH_LONG).show()
                        findViewById<TextInputEditText>(R.id.registrationNumberInput).error = "Registration number already in use"
                    }
                    return@checkRegistrationNumberExists
                }

                // Create company object
                val company = Company(
                    id = registrationNumber,
                    companyName = companyName,
                    registrationNumber = registrationNumber,
                    industry = industry,
                    companySize = companySize,
                    location = location,
                    website = website,
                    description = description,
                    contactPersonName = contactPersonName,
                    contactPersonEmail = contactPersonEmail,
                    contactPersonPhone = contactPersonPhone,
                    email = email
                )

                // Register the company
                FirebaseHelper.getInstance().addCompany(company, password) { success, error ->
                    if (success) {
                        runOnUiThread {
                            Toast.makeText(this, "Registration successful! Please login to continue.", Toast.LENGTH_LONG).show()

                            // Navigate to login page
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
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
                                    FirebaseHelper.getInstance().recoverOrCreateCompany(company, password) { recoverySuccess, recoveryError ->
                                        if (recoverySuccess) {
                                            runOnUiThread {
                                                Toast.makeText(this, "Registration completed successfully", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, LoginActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
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
                                    findViewById<TextInputEditText>(R.id.emailInput).error = "Please use a different email"
                                    findViewById<TextInputEditText>(R.id.emailInput).requestFocus()
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
    }

    private fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists: $email")

        // First, log all users in the database for debugging
        logAllUsersAndCompanies()

        // First check if the email exists in Firebase Authentication
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
                    } else {
                        Log.e(TAG, "Error in direct email check: $errorMessage")
                    }

                    callback(emailInUse)
                }
            }
    }

    private fun logAllUsersAndCompanies() {
        Log.d(TAG, "======= LOGGING ALL DATABASE USERS AND COMPANIES =======")

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
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching companies", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching users", e)
            }
    }

    private fun checkRegistrationNumberExists(registrationNumber: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if registration number exists: $registrationNumber")

        db.collection("companies")
            .whereEqualTo("registrationNumber", registrationNumber)
            .get()
            .addOnSuccessListener { result ->
                val exists = !result.isEmpty
                if (exists) {
                    Log.d(TAG, "Registration number found in companies collection")
                } else {
                    Log.d(TAG, "Registration number not found in companies collection")
                }
                callback(exists)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking registration number", e)
                callback(false) // Assume registration number doesn't exist if there's an error
            }
    }
}