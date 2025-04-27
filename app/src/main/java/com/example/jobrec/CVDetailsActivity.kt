package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CVDetailsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var educationAdapter: EducationAdapter
    private lateinit var experienceAdapter: ExperienceAdapter
    
    // User data from SignupActivity
    private lateinit var name: String
    private lateinit var surname: String
    private lateinit var email: String
    private lateinit var phoneNumber: String
    private lateinit var address: String
    private lateinit var idNumber: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cv_details)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "CV Details"
        }

        // Get user data from intent
        name = intent.getStringExtra("name") ?: ""
        surname = intent.getStringExtra("surname") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        address = intent.getStringExtra("address") ?: ""
        idNumber = intent.getStringExtra("idNumber") ?: ""
        password = intent.getStringExtra("password") ?: ""

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize RecyclerViews
        val educationRecyclerView = findViewById<RecyclerView>(R.id.educationRecyclerView)
        val experienceRecyclerView = findViewById<RecyclerView>(R.id.experienceRecyclerView)

        educationAdapter = EducationAdapter()
        experienceAdapter = ExperienceAdapter()

        educationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CVDetailsActivity)
            adapter = educationAdapter
        }

        experienceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CVDetailsActivity)
            adapter = experienceAdapter
        }

        // Set up click listeners
        findViewById<Button>(R.id.addEducationButton).setOnClickListener {
            educationAdapter.addNewEducation()
        }

        findViewById<Button>(R.id.addExperienceButton).setOnClickListener {
            experienceAdapter.addNewExperience()
        }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            saveCVDetailsAndRegisterUser()
        }
    }

    private fun saveCVDetailsAndRegisterUser() {
        val summary = findViewById<TextInputEditText>(R.id.summaryInput).text.toString()
        val skills = findViewById<TextInputEditText>(R.id.skillsInput).text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        Log.d("CVDetailsActivity", "Saving CV details for user: $idNumber")
        Log.d("CVDetailsActivity", "Name: $name, Email: $email")

        // Convert adapter education list to FirebaseHelper Education list
        val educationList = educationAdapter.getEducationList().map { adapterEducation ->
            Education(
                institution = adapterEducation.institution,
                degree = adapterEducation.degree,
                fieldOfStudy = "", // Not available in adapter
                startDate = adapterEducation.startDate,
                endDate = adapterEducation.endDate,
                description = "" // Not available in adapter
            )
        }

        // Convert adapter experience list to FirebaseHelper Experience list
        val experienceList = experienceAdapter.getExperienceList().map { adapterExperience ->
            Experience(
                company = adapterExperience.company,
                position = adapterExperience.position,
                startDate = adapterExperience.startDate,
                endDate = adapterExperience.endDate,
                description = adapterExperience.description
            )
        }

        // Create user object with all information
        val user = User(
            idNumber = idNumber,
            name = name,
            surname = surname,
            email = email,
            phoneNumber = phoneNumber,
            address = address,
            summary = summary,
            skills = skills,
            education = educationList,
            experience = experienceList
        )

        Log.d("CVDetailsActivity", "Created user object with ID: ${user.idNumber}")

        // Register user with Firebase
        val firebaseHelper = FirebaseHelper.getInstance()
        firebaseHelper.addUser(user, password) { success, errorMessage ->
            runOnUiThread {
                if (success) {
                    Log.d("CVDetailsActivity", "Registration successful, navigating to LoginActivity")
                    Toast.makeText(this, "Registration successful! Please login to continue.", Toast.LENGTH_LONG).show()
                    
                    // Navigate to login page
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("CVDetailsActivity", "Registration failed: $errorMessage")
                    Toast.makeText(this, "Registration failed: ${errorMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}