package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class CompanyProfileActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyId: String
    
    // Profile views
    private lateinit var companyLogo: CircleImageView
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

    // Analytics views
    private lateinit var totalApplicationsText: TextView
    private lateinit var activeJobsText: TextView
    private lateinit var viewDetailedAnalyticsButton: com.google.android.material.button.MaterialButton
    private lateinit var editProfileButton: com.google.android.material.button.MaterialButton
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_profile)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        Log.d("CompanyProfile", "Starting CompanyProfileActivity")
        
        // Get company ID from intent or find it using the current user's email
        companyId = intent.getStringExtra("companyId") ?: run {
            val currentUser = auth.currentUser
            Log.d("CompanyProfile", "Current user email: ${currentUser?.email}")
            
            if (currentUser != null) {
                // Query companies collection to find the company with matching email
                db.collection("companies")
                    .whereEqualTo("email", currentUser.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("CompanyProfile", "Found ${documents.size()} documents")
                        if (!documents.isEmpty) {
                            // Get the registration number from the document
                            val registrationNumber = documents.documents[0].getString("registrationNumber")
                            if (registrationNumber != null) {
                                companyId = registrationNumber
                                Log.d("CompanyProfile", "Found company ID (registration number): $companyId")
                                loadCompanyData()
                            } else {
                                Log.e("CompanyProfile", "Registration number not found in document")
                                Toast.makeText(this, "Error: Company data incomplete", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } else {
                            Log.e("CompanyProfile", "No company found for email: ${currentUser.email}")
                            Toast.makeText(this, "Error: Company not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CompanyProfile", "Error finding company", e)
                        Toast.makeText(this, "Error finding company: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                return@run ""
            } else {
                Log.e("CompanyProfile", "No user logged in")
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return@run ""
            }
        }

        Log.d("CompanyProfile", "Using company ID from intent: $companyId")

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Company Profile"

        // Initialize views
        initializeViews()
        setupBottomNavigation()

        // Load company data if we have the companyId directly
        if (companyId.isNotEmpty()) {
            loadCompanyData()
        }
    }

    private fun initializeViews() {
        // Profile views
        companyLogo = findViewById(R.id.companyLogo)
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

        // Analytics views
        totalApplicationsText = findViewById(R.id.totalApplicationsText)
        activeJobsText = findViewById(R.id.activeJobsText)
        viewDetailedAnalyticsButton = findViewById(R.id.viewDetailedAnalyticsButton)
        editProfileButton = findViewById(R.id.editProfileButton)

        // Set up click listeners
        viewDetailedAnalyticsButton.setOnClickListener {
            val intent = Intent(this, CompanyAnalyticsActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }

        editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditCompanyProfileActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_profile -> {
                    // Already on profile, do nothing
                    true
                }
                R.id.navigation_analytics -> {
                    val intent = Intent(this, CompanyAnalyticsActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCompanyData() {
        Log.d("CompanyProfile", "Loading data for company ID: $companyId")
        
        db.collection("companies").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                Log.d("CompanyProfile", "Document exists: ${document.exists()}")
                if (document != null && document.exists()) {
                    // Load profile data
                    companyNameText.text = document.getString("companyName") ?: "Not set"
                    industryText.text = document.getString("industry") ?: "Not set"
                    registrationNumberText.text = document.getString("registrationNumber") ?: "Not set"
                    companySizeText.text = document.getString("companySize") ?: "Not set"
                    locationText.text = document.getString("location") ?: "Not set"
                    websiteText.text = document.getString("website") ?: "Not set"
                    descriptionText.text = document.getString("description") ?: "Not set"
                    contactPersonNameText.text = document.getString("contactPersonName") ?: "Not set"
                    contactPersonEmailText.text = document.getString("contactPersonEmail") ?: "Not set"
                    contactPersonPhoneText.text = document.getString("contactPersonPhone") ?: "Not set"

                    // Load company logo
                    document.getString("logoUrl")?.let { logoUrl ->
                        Glide.with(this)
                            .load(logoUrl)
                            .placeholder(R.drawable.ic_company_placeholder)
                            .error(R.drawable.ic_company_placeholder)
                            .into(companyLogo)
                    }

                    // Load analytics
                    loadAnalytics()
                } else {
                    Log.e("CompanyProfile", "Document does not exist")
                    Toast.makeText(this, "Error: Company data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CompanyProfile", "Error loading company data", e)
                Toast.makeText(this, "Error loading company data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAnalytics() {
        // Load total applications
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                totalApplicationsText.text = documents.size().toString()
            }

        // Load active jobs
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                activeJobsText.text = documents.size().toString()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 