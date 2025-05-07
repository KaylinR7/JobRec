package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore

class CompanyProfileActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String

    // UI elements
    private lateinit var companyLogo: ImageView
    private lateinit var companyNameText: TextView
    private lateinit var companyIndustryText: TextView
    private lateinit var companyDescriptionText: TextView
    private lateinit var companyLocationText: TextView
    private lateinit var companyEmailText: TextView
    private lateinit var companyPhoneText: TextView
    private lateinit var editProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_profile)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isEmpty()) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Company Profile"
        }

        // Initialize UI elements
        initializeViews()
        
        // Load company data
        loadCompanyData()

        // Set up button click listeners
        setupButtonListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, CompanyDashboardActivity::class.java)
        intent.putExtra("companyId", companyId)
        startActivity(intent)
        finish()
        return true
    }

    private fun initializeViews() {
        companyLogo = findViewById(R.id.companyLogo)
        companyNameText = findViewById(R.id.companyNameText)
        companyIndustryText = findViewById(R.id.companyIndustryText)
        companyDescriptionText = findViewById(R.id.companyDescriptionText)
        companyLocationText = findViewById(R.id.companyLocationText)
        companyEmailText = findViewById(R.id.companyEmailText)
        companyPhoneText = findViewById(R.id.companyPhoneText)
        editProfileButton = findViewById(R.id.editProfileButton)
    }

    private fun loadCompanyData() {
        db.collection("companies")
            .document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val company = document.toObject(Company::class.java)
                    company?.let { displayCompanyData(it) }
                } else {
                    Log.e("CompanyProfileActivity", "No company found with ID: $companyId")
                    Toast.makeText(this, "Company data not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CompanyProfileActivity", "Error loading company data", e)
                Toast.makeText(this, "Error loading company data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayCompanyData(company: Company) {
        // Set company logo if available
        if (company.profileImageUrl.isNotEmpty()) {
            // TODO: Load company logo using Glide or Picasso
        }

        // Set text fields
        companyNameText.text = company.companyName
        companyIndustryText.text = company.industry
        companyDescriptionText.text = company.description
        companyLocationText.text = company.location
        companyEmailText.text = company.contactPersonEmail
        companyPhoneText.text = company.contactPersonPhone
    }

    private fun setupButtonListeners() {
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditCompanyProfileActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }
    }
} 