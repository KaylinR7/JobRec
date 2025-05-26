package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.jobrec.databinding.DialogApplicationDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicationDetailsDialog : BottomSheetDialogFragment() {
    private var _binding: DialogApplicationDetailsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var application: Application

    companion object {
        private const val ARG_APPLICATION = "application"

        fun newInstance(application: Application): ApplicationDetailsDialog {
            return ApplicationDetailsDialog().apply {
                this.application = application
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogApplicationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.apply {
            jobTitleText.text = application.jobTitle
            companyNameText.text = application.companyName
            applicantNameText.text = application.applicantName
            appliedDateText.text = formatDate(application.appliedDate)
            statusChip.text = application.status.capitalize()
            statusChip.setChipBackgroundColorResource(getStatusColor(application.status))
            
            if (application.coverLetter.isNotEmpty()) {
                coverLetterText.text = application.coverLetter
                coverLetterLayout.visibility = View.VISIBLE
            } else {
                coverLetterLayout.visibility = View.GONE
            }
            
            if (application.cvUrl.isNotEmpty()) {
                cvUrlText.text = application.cvUrl
                cvUrlLayout.visibility = View.VISIBLE
            } else {
                cvUrlLayout.visibility = View.GONE
            }

            updateStatusButton.setOnClickListener {
                showStatusUpdateDialog()
            }
        }
    }

    private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
        val date = timestamp.toDate()
        return SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
    }

    private fun getStatusColor(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> R.color.status_pending
            "shortlisted" -> R.color.status_shortlisted
            "rejected" -> R.color.status_rejected
            else -> R.color.status_pending
        }
    }

    private fun showStatusUpdateDialog() {
        val statuses = arrayOf("pending", "shortlisted", "rejected")
        val currentStatusIndex = statuses.indexOf(application.status.lowercase())

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 