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
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Company Profile"
        }
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        db = FirebaseFirestore.getInstance()
        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isEmpty()) {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                companyId = currentUserId
            } else {
                Toast.makeText(this, "Error: Company ID not found and user not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }
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
        db.collection("companies")
            .document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val existingCompany = document.toObject(Company::class.java)
                    val updatedCompany = existingCompany?.copy(
                        companyName = companyNameInput.text.toString(),
                        industry = industryInput.text.toString(),
                        registrationNumber = registrationNumberInput.text.toString(),
                        companySize = companySizeInput.text.toString(),
                        location = locationInput.text.toString(),
                        website = websiteInput.text.toString(),
                        description = descriptionInput.text.toString(),
                        contactPersonName = contactPersonNameInput.text.toString(),
                        contactPersonEmail = contactPersonEmailInput.text.toString(),
                        contactPersonPhone = contactPersonPhoneInput.text.toString()
                    ) ?: Company(
                        id = companyId,
                        companyName = companyNameInput.text.toString(),
                        industry = industryInput.text.toString(),
                        registrationNumber = registrationNumberInput.text.toString(),
                        companySize = companySizeInput.text.toString(),
                        location = locationInput.text.toString(),
                        website = websiteInput.text.toString(),
                        description = descriptionInput.text.toString(),
                        contactPersonName = contactPersonNameInput.text.toString(),
                        contactPersonEmail = contactPersonEmailInput.text.toString(),
                        contactPersonPhone = contactPersonPhoneInput.text.toString(),
                        userId = companyId
                    )
                    val companyData = mapOf(
                        "companyName" to updatedCompany.companyName,
                        "industry" to updatedCompany.industry,
                        "registrationNumber" to updatedCompany.registrationNumber,
                        "companySize" to updatedCompany.companySize,
                        "location" to updatedCompany.location,
                        "website" to updatedCompany.website,
                        "description" to updatedCompany.description,
                        "contactPersonName" to updatedCompany.contactPersonName,
                        "contactPersonEmail" to updatedCompany.contactPersonEmail,
                        "contactPersonPhone" to updatedCompany.contactPersonPhone,
                        "email" to (existingCompany?.email ?: ""),
                        "profileImageUrl" to (existingCompany?.profileImageUrl ?: ""),
                        "status" to (existingCompany?.status ?: "active"),
                        "userId" to (existingCompany?.userId ?: companyId)
                    )
                    db.collection("companies")
                        .document(companyId)
                        .set(companyData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Company profile updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating company profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    val newCompany = Company(
                        id = companyId,
                        companyName = companyNameInput.text.toString(),
                        industry = industryInput.text.toString(),
                        registrationNumber = registrationNumberInput.text.toString(),
                        companySize = companySizeInput.text.toString(),
                        location = locationInput.text.toString(),
                        website = websiteInput.text.toString(),
                        description = descriptionInput.text.toString(),
                        contactPersonName = contactPersonNameInput.text.toString(),
                        contactPersonEmail = contactPersonEmailInput.text.toString(),
                        contactPersonPhone = contactPersonPhoneInput.text.toString(),
                        userId = companyId
                    )
                    val companyData = mapOf(
                        "companyName" to newCompany.companyName,
                        "industry" to newCompany.industry,
                        "registrationNumber" to newCompany.registrationNumber,
                        "companySize" to newCompany.companySize,
                        "location" to newCompany.location,
                        "website" to newCompany.website,
                        "description" to newCompany.description,
                        "contactPersonName" to newCompany.contactPersonName,
                        "contactPersonEmail" to newCompany.contactPersonEmail,
                        "contactPersonPhone" to newCompany.contactPersonPhone,
                        "email" to newCompany.email,
                        "profileImageUrl" to newCompany.profileImageUrl,
                        "status" to newCompany.status,
                        "userId" to newCompany.userId
                    )
                    db.collection("companies")
                        .document(companyId)
                        .set(companyData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Company profile created successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error creating company profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving company data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}