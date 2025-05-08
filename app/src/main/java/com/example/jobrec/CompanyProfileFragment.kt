package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView

class CompanyProfileFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNav: BottomNavigationView

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
    private lateinit var viewDetailedAnalyticsButton: MaterialButton
    private lateinit var editProfileButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_company_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews(view)
        setupBottomNavigation()
        loadCompanyProfile()
    }

    private fun initializeViews(view: View) {
        // Profile views
        companyLogo = view.findViewById(R.id.companyLogo)
        companyNameText = view.findViewById(R.id.companyNameText)
        industryText = view.findViewById(R.id.industryText)
        registrationNumberText = view.findViewById(R.id.registrationNumberText)
        companySizeText = view.findViewById(R.id.companySizeText)
        locationText = view.findViewById(R.id.locationText)
        websiteText = view.findViewById(R.id.websiteText)
        descriptionText = view.findViewById(R.id.descriptionText)
        contactPersonNameText = view.findViewById(R.id.contactPersonNameText)
        contactPersonEmailText = view.findViewById(R.id.contactPersonEmailText)
        contactPersonPhoneText = view.findViewById(R.id.contactPersonPhoneText)

        // Analytics views
        totalApplicationsText = view.findViewById(R.id.totalApplicationsText)
        activeJobsText = view.findViewById(R.id.activeJobsText)
        viewDetailedAnalyticsButton = view.findViewById(R.id.viewDetailedAnalyticsButton)
        editProfileButton = view.findViewById(R.id.editProfileButton)

        // Set up click listeners
        viewDetailedAnalyticsButton.setOnClickListener {
            startActivity(Intent(requireContext(), CompanyAnalyticsActivity::class.java))
        }

        editProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), EditCompanyProfileActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = requireView().findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_profile -> {
                    // Already on profile, do nothing
                    true
                }
                R.id.navigation_analytics -> {
                    startActivity(Intent(requireContext(), CompanyAnalyticsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCompanyProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("companies").document(userId)
            .get()
            .addOnSuccessListener { document ->
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
                    loadAnalytics(userId)
                } else {
                    Toast.makeText(context, "Company profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAnalytics(companyId: String) {
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
} 