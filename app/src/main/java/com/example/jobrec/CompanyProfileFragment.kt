package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class CompanyProfileFragment : Fragment() {
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

    companion object {
        private const val ARG_COMPANY_ID = "company_id"

        fun newInstance(companyId: String): CompanyProfileFragment {
            val fragment = CompanyProfileFragment()
            val args = Bundle()
            args.putString(ARG_COMPANY_ID, companyId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString(ARG_COMPANY_ID) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_company_profile, container, false)
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        
        // Initialize UI elements
        initializeViews(view)
        
        // Load company data
        loadCompanyData()
        
        return view
    }

    private fun initializeViews(view: View) {
        companyLogoImage = view.findViewById(R.id.companyLogoImage)
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
        editProfileButton = view.findViewById(R.id.editProfileButton)

        editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditCompanyProfileActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }
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
                    Toast.makeText(requireContext(), "Company data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading company data: ${e.message}", Toast.LENGTH_LONG).show()
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
} 