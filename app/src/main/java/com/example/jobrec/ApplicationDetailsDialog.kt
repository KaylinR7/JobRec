package com.example.jobrec

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class ApplicationDetailsDialog : DialogFragment() {
    private lateinit var jobTitle: TextView
    private lateinit var applicantName: TextView
    private lateinit var appliedDate: TextView
    private lateinit var coverLetter: TextView
    private lateinit var downloadCvButton: MaterialButton
    private lateinit var updateStatusButton: MaterialButton

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var application: JobApplication

    companion object {
        private const val ARG_APPLICATION = "application"

        fun newInstance(application: JobApplication): ApplicationDetailsDialog {
            val args = Bundle().apply {
                putSerializable(ARG_APPLICATION, application as Serializable)
            }
            return ApplicationDetailsDialog().apply {
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
        return inflater.inflate(R.layout.dialog_application_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        jobTitle = view.findViewById(R.id.jobTitle)
        applicantName = view.findViewById(R.id.applicantName)
        appliedDate = view.findViewById(R.id.appliedDate)
        coverLetter = view.findViewById(R.id.coverLetter)
        downloadCvButton = view.findViewById(R.id.downloadCvButton)
        updateStatusButton = view.findViewById(R.id.updateStatusButton)

        // Set up views
        jobTitle.text = application.jobTitle
        applicantName.text = application.applicantName
        
        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        appliedDate.text = "Applied: ${dateFormat.format(application.appliedDate)}"
        
        coverLetter.text = application.coverLetter

        // Set up download CV button
        downloadCvButton.setOnClickListener {
            downloadCv()
        }

        // Set up update status button
        updateStatusButton.setOnClickListener {
            showStatusUpdateDialog()
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

    private fun showStatusUpdateDialog() {
        val statuses = arrayOf("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWING", "OFFERED", "REJECTED")
        val currentStatusIndex = statuses.indexOf(application.status)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Application Status")
            .setSingleChoiceItems(statuses, currentStatusIndex) { dialog, which ->
                val newStatus = statuses[which]
                updateStatus(newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatus(newStatus: String) {
        db.collection("applications")
            .document(application.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                application = application.copy(status = newStatus)
                Toast.makeText(context, "Status updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 