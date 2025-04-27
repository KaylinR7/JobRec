package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class UserApplicationDetailsDialog : DialogFragment() {
    private lateinit var jobTitle: TextView
    private lateinit var companyName: TextView
    private lateinit var appliedDate: TextView
    private lateinit var status: TextView
    private lateinit var coverLetter: TextView
    private lateinit var downloadCvButton: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var application: JobApplication

    companion object {
        private const val ARG_APPLICATION = "application"

        fun newInstance(application: JobApplication): UserApplicationDetailsDialog {
            val args = Bundle().apply {
                putSerializable(ARG_APPLICATION, application as Serializable)
            }
            return UserApplicationDetailsDialog().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = arguments?.getSerializable(ARG_APPLICATION) as JobApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_user_application_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        jobTitle = view.findViewById(R.id.jobTitle)
        companyName = view.findViewById(R.id.companyName)
        appliedDate = view.findViewById(R.id.appliedDate)
        status = view.findViewById(R.id.status)
        coverLetter = view.findViewById(R.id.coverLetter)
        downloadCvButton = view.findViewById(R.id.downloadCvButton)

        // Load job details
        loadJobDetails()
        // Load company details
        loadCompanyDetails()
        // Set up cover letter
        coverLetter.text = application.coverLetter
        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        appliedDate.text = "Applied: ${dateFormat.format(application.appliedDate)}"
        // Set status
        status.text = "Status: ${application.status}"

        // Set up download CV button
        downloadCvButton.setOnClickListener {
            downloadCv()
        }
    }

    private fun loadJobDetails() {
        db.collection("jobs")
            .document(application.jobId)
            .get()
            .addOnSuccessListener { document ->
                jobTitle.text = document.getString("title") ?: "Unknown Job"
            }
    }

    private fun loadCompanyDetails() {
        db.collection("companies")
            .document(application.companyId)
            .get()
            .addOnSuccessListener { document ->
                companyName.text = document.getString("companyName") ?: "Unknown Company"
            }
    }

    private fun downloadCv() {
        if (application.cvUrl.isEmpty()) {
            Toast.makeText(context, "No CV available", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement CV download
        Toast.makeText(context, "Downloading CV...", Toast.LENGTH_SHORT).show()
    }
} 