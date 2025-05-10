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

        // Log registration attempt
        Log.d(TAG, "Attempting to register company: $email, registration number: $registrationNumber")

        // First check if the email already exists
        checkEmailExists(email) { emailExists ->
            if (emailExists) {
                Log.e(TAG, "Email already exists: $email")
                runOnUiThread {
                    Toast.makeText(this, "This email is already registered. Please use a different email or login.", Toast.LENGTH_LONG).show()
                    findViewById<TextInputEditText>(R.id.emailInput).error = "Email already in use"
                }
                return@checkEmailExists
            }

            // Check if registration number already exists
            checkRegistrationNumberExists(registrationNumber) { regNumberExists ->
                if (regNumberExists) {
                    Log.e(TAG, "Registration number already exists: $registrationNumber")
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

                // Log company object
                Log.d(TAG, "Company object created: $company")

                // Register with Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Firebase Auth account created successfully")

                            // Save company data to Firestore
                            db.collection("companies")
                                .document(registrationNumber)
                                .set(company)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Company registered successfully")
                                    Toast.makeText(this, "Registration successful! Please login to continue.", Toast.LENGTH_LONG).show()

                                    // Navigate to login page
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error saving company data", e)
                                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Log.e(TAG, "Registration failed", task.exception)
                            // Check if the error is about email already in use
                            if (task.exception?.message?.contains("email address is already in use", ignoreCase = true) == true) {
                                Toast.makeText(this, "This email is already registered. Please use a different email or login.", Toast.LENGTH_LONG).show()
                                findViewById<TextInputEditText>(R.id.emailInput).error = "Email already in use"
                            } else {
                                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
        }
    }

    private fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Checking if email exists: $email")

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