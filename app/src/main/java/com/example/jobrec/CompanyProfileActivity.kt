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
    private lateinit var companyLogoImage: ImageView
    private lateinit var companyNameText: TextView
    private lateinit var industryText: TextView
    private lateinit var registrationNumberText: TextView
    private lateinit var companySizeText: TextView
    private lateinit var locationText: TextView
    private lateinit var websiteText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var contactPersonNameText: TextView
    private lateinit var contactPersonEmailText: TextView
    private lateinit var contactPersonPhoneText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var postJobButton: Button

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
        companyLogoImage = findViewById(R.id.companyLogoImage)
        companyNameText = findViewById(R.id.companyNameText)
        industryText = findViewById(R.id.industryText)
        registrationNumberText = findViewById(R.id.registrationNumberText)
        companySizeText = findViewById(R.id.companySizeText)
        locationText = findViewById(R.id.locationText)
        websiteText = findViewById(R.id.websiteText)
        descriptionText = findViewById(R.id.descriptionText)
        contactPersonNameText = findViewById(R.id.contactPersonNameText)
        contactPersonEmailText = findViewById(R.id.contactPersonEmailText)
        contactPersonPhoneText = findViewById(R.id.contactPersonPhoneText)
        editProfileButton = findViewById(R.id.editProfileButton)
        postJobButton = findViewById(R.id.postJobButton)
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
        industryText.text = company.industry
        registrationNumberText.text = "Registration Number: ${company.registrationNumber}"
        companySizeText.text = "Company Size: ${company.companySize}"
        locationText.text = "Location: ${company.location}"
        websiteText.text = "Website: ${company.website}"
        descriptionText.text = "Description: ${company.description}"
        contactPersonNameText.text = "Contact Person: ${company.contactPersonName}"
        contactPersonEmailText.text = "Email: ${company.contactPersonEmail}"
        contactPersonPhoneText.text = "Phone: ${company.contactPersonPhone}"
    }

    private fun setupButtonListeners() {
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditCompanyProfileActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }

        postJobButton.setOnClickListener {
            val intent = Intent(this, PostJobActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }
    }
} 