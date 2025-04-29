package com.example.jobrec

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = "Student Registration"
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Initialize Firebase - matching CompanySignupActivity pattern
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Register button
        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            registerStudent()
        }
    }

    private fun registerStudent() {
        // Get data from input fields
        val name = findViewById<TextInputEditText>(R.id.etName).text.toString()
        val surname = findViewById<TextInputEditText>(R.id.etSurname).text.toString()
        val cellNumber = findViewById<TextInputEditText>(R.id.etCellNumber).text.toString()
        val email = findViewById<TextInputEditText>(R.id.etEmail).text.toString()
        val password = findViewById<TextInputEditText>(R.id.etPassword).text.toString()
        val address = findViewById<TextInputEditText>(R.id.etAddress).text.toString()

        // Collecting list values from the form
        val skills = findViewById<TextInputEditText>(R.id.etSkills).text.toString().split(",").map { it.trim() }
        val hobbies = findViewById<TextInputEditText>(R.id.etHobbies).text.toString().split(",").map { it.trim() }

        // Get new fields
        val achievements = findViewById<TextInputEditText>(R.id.etAchievements).text.toString()
        val linkedin = findViewById<TextInputEditText>(R.id.etLinkedin).text.toString()
        val github = findViewById<TextInputEditText>(R.id.etGithub).text.toString()
        val portfolio = findViewById<TextInputEditText>(R.id.etPortfolio).text.toString()

        // Validate inputs
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create student object
        val student = User(
            name = name,
            surname = surname,
            phoneNumber = cellNumber,
            email = email,
            address = address,
            skills = skills,
            hobbies = hobbies,
            achievements = achievements,
            linkedin = linkedin,
            github = github,
            portfolio = portfolio,
            role = "student"
        )

        // Register with Firebase Auth first
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save student data to Firestore
                    firestore.collection("Users")
                        .document(auth.currentUser?.uid ?: "") // Using UID as document ID
                        .set(student)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                            finish() // Return to previous activity
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save student data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}