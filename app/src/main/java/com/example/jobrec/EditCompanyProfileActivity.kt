package com.example.jobrec

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class EditCompanyProfileActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String

    // UI elements
    private lateinit var companyNameInput: TextInputEditText
    private lateinit var industryInput: TextInputEditText
    private lateinit var registrationNumberInput: TextInputEditText
    private lateinit var companySizeInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var websiteInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var contactPersonNameInput: TextInputEditText
    private lateinit var contactPersonEmailInput: TextInputEditText
    private lateinit var contactPersonPhoneInput: TextInputEditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_company_profile)

        // Set up toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Company Profile"
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: ""
        
        if (companyId.isEmpty()) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI elements
        initializeViews()
        setupButtons()
        loadCompanyData()
    }

    private fun initializeViews() {
        companyNameInput = findViewById(R.id.companyNameInput)
        industryInput = findViewById(R.id.industryInput)
        registrationNumberInput = findViewById(R.id.registrationNumberInput)
        companySizeInput = findViewById(R.id.companySizeInput)
        locationInput = findViewById(R.id.locationInput)
        websiteInput = findViewById(R.id.websiteInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        contactPersonNameInput = findViewById(R.id.contactPersonNameInput)
        contactPersonEmailInput = findViewById(R.id.contactPersonEmailInput)
        contactPersonPhoneInput = findViewById(R.id.contactPersonPhoneInput)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setupButtons() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                updateCompanyProfile()
            }
        }
    }

    private fun loadCompanyData() {
        db.collection("companies")
            .document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val company = document.toObject(Company::class.java)
                    company?.let { fillFormWithCompanyData(it) }
                } else {
                    Toast.makeText(this, "Error: Company not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading company: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun fillFormWithCompanyData(company: Company) {
        companyNameInput.setText(company.companyName)
        industryInput.setText(company.industry)
        registrationNumberInput.setText(company.registrationNumber)
        companySizeInput.setText(company.companySize)
        locationInput.setText(company.location)
        websiteInput.setText(company.website)
        descriptionInput.setText(company.description)
        contactPersonNameInput.setText(company.contactPersonName)
        contactPersonEmailInput.setText(company.contactPersonEmail)
        contactPersonPhoneInput.setText(company.contactPersonPhone)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (companyNameInput.text.isNullOrBlank()) {
            companyNameInput.error = "Company name is required"
            isValid = false
        }

        if (industryInput.text.isNullOrBlank()) {
            industryInput.error = "Industry is required"
            isValid = false
        }

        if (registrationNumberInput.text.isNullOrBlank()) {
            registrationNumberInput.error = "Registration number is required"
            isValid = false
        }

        if (companySizeInput.text.isNullOrBlank()) {
            companySizeInput.error = "Company size is required"
            isValid = false
        }

        if (locationInput.text.isNullOrBlank()) {
            locationInput.error = "Location is required"
            isValid = false
        }

        if (websiteInput.text.isNullOrBlank()) {
            websiteInput.error = "Website is required"
            isValid = false
        }

        if (descriptionInput.text.isNullOrBlank()) {
            descriptionInput.error = "Description is required"
            isValid = false
        }

        if (contactPersonNameInput.text.isNullOrBlank()) {
            contactPersonNameInput.error = "Contact person name is required"
            isValid = false
        }

        if (contactPersonEmailInput.text.isNullOrBlank()) {
            contactPersonEmailInput.error = "Contact person email is required"
            isValid = false
        }

        if (contactPersonPhoneInput.text.isNullOrBlank()) {
            contactPersonPhoneInput.error = "Contact person phone is required"
            isValid = false
        }

        return isValid
    }

    private fun updateCompanyProfile() {
        val companyUpdates = hashMapOf<String, Any>(
            "companyName" to companyNameInput.text.toString(),
            "industry" to industryInput.text.toString(),
            "registrationNumber" to registrationNumberInput.text.toString(),
            "companySize" to companySizeInput.text.toString(),
            "location" to locationInput.text.toString(),
            "website" to websiteInput.text.toString(),
            "description" to descriptionInput.text.toString(),
            "contactPersonName" to contactPersonNameInput.text.toString(),
            "contactPersonEmail" to contactPersonEmailInput.text.toString(),
            "contactPersonPhone" to contactPersonPhoneInput.text.toString()
        )

        db.collection("companies")
            .document(companyId)
            .update(companyUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Company profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating company profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 