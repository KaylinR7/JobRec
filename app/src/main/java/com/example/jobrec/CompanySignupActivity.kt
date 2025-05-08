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

        // Register with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save company data to Firestore
                    db.collection("companies")
                        .document(registrationNumber)
                        .set(company)
                        .addOnSuccessListener {
                            Log.d("CompanySignupActivity", "Company registered successfully")
                            Toast.makeText(this, "Registration successful! Please login to continue.", Toast.LENGTH_LONG).show()
                            
                            // Navigate to login page
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CompanySignupActivity", "Error saving company data", e)
                            Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Log.e("CompanySignupActivity", "Registration failed", task.exception)
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
} 