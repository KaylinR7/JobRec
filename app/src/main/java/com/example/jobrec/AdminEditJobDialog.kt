package com.example.jobrec

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AdminEditJobDialog : DialogFragment() {
    private lateinit var job: Job
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminEditJobDialog"
    
    // Callbacks for when a job is updated or deleted
    var onJobUpdated: (() -> Unit)? = null
    var onJobDeleted: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set dialog width to match parent with margins
        dialog.setOnShowListener {
            val width = resources.displayMetrics.widthPixels
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout((width * 0.9).toInt(), height)
        }
        
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_admin_edit_job, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get job from arguments
        job = arguments?.getParcelable(ARG_JOB) ?: return
        
        // Set up UI with job data
        setupUI(view)
        
        // Set up buttons
        view.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }
        
        view.findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            saveChanges(view)
        }
        
        view.findViewById<MaterialButton>(R.id.deleteButton).setOnClickListener {
            confirmDelete(view)
        }
    }
    
    private fun setupUI(view: View) {
        // Set dialog title
        view.findViewById<TextView>(R.id.dialogTitleTextView).text = "Edit Job: ${job.title}"
        
        // Set job details
        view.findViewById<TextInputEditText>(R.id.jobTitleEditText).setText(job.title)
        view.findViewById<TextInputEditText>(R.id.companyNameEditText).setText(job.companyName)
        view.findViewById<TextInputEditText>(R.id.locationEditText).setText(job.location)
        view.findViewById<TextInputEditText>(R.id.salaryRangeEditText).setText(job.salary)
        view.findViewById<TextInputEditText>(R.id.descriptionEditText).setText(job.description)
        
        // Set up job type dropdown
        val jobTypeDropdown = view.findViewById<AutoCompleteTextView>(R.id.jobTypeDropdown)
        val jobTypeOptions = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Temporary")
        val jobTypeAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, jobTypeOptions)
        jobTypeDropdown.setAdapter(jobTypeAdapter)
        
        // Set current job type (capitalize first letter)
        val currentJobType = job.jobType.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        jobTypeDropdown.setText(currentJobType, false)
        
        // Set up experience level dropdown
        val experienceLevelDropdown = view.findViewById<AutoCompleteTextView>(R.id.experienceLevelDropdown)
        val experienceLevelOptions = arrayOf("Entry Level", "Junior", "Mid-Level", "Senior", "Executive")
        val experienceLevelAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, experienceLevelOptions)
        experienceLevelDropdown.setAdapter(experienceLevelAdapter)
        
        // Set current experience level (capitalize first letter)
        val currentExperienceLevel = job.experienceLevel.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        experienceLevelDropdown.setText(currentExperienceLevel, false)
        
        // Set up status dropdown
        val statusDropdown = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown)
        val statusOptions = arrayOf("Active", "Closed", "Draft", "Expired")
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, statusOptions)
        statusDropdown.setAdapter(statusAdapter)
        
        // Set current status (capitalize first letter)
        val currentStatus = job.status.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        statusDropdown.setText(currentStatus, false)
    }
    
    private fun saveChanges(view: View) {
        // Get updated values
        val newTitle = view.findViewById<TextInputEditText>(R.id.jobTitleEditText).text.toString()
        val newCompanyName = view.findViewById<TextInputEditText>(R.id.companyNameEditText).text.toString()
        val newLocation = view.findViewById<TextInputEditText>(R.id.locationEditText).text.toString()
        val newSalary = view.findViewById<TextInputEditText>(R.id.salaryRangeEditText).text.toString()
        val newDescription = view.findViewById<TextInputEditText>(R.id.descriptionEditText).text.toString()
        
        val newJobTypeCapitalized = view.findViewById<AutoCompleteTextView>(R.id.jobTypeDropdown).text.toString()
        val newJobType = newJobTypeCapitalized.lowercase()
        
        val newExperienceLevelCapitalized = view.findViewById<AutoCompleteTextView>(R.id.experienceLevelDropdown).text.toString()
        val newExperienceLevel = newExperienceLevelCapitalized.lowercase()
        
        val newStatusCapitalized = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown).text.toString()
        val newStatus = newStatusCapitalized.lowercase()
        
        // Validate input
        if (newTitle.isBlank() || newCompanyName.isBlank()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.saveButton).text = "Saving..."
        
        // Update job in Firestore
        db.collection("jobs").document(job.id)
            .update(
                mapOf(
                    "title" to newTitle,
                    "companyName" to newCompanyName,
                    "location" to newLocation,
                    "salary" to newSalary,
                    "description" to newDescription,
                    "jobType" to newJobType,
                    "experienceLevel" to newExperienceLevel,
                    "status" to newStatus
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Job updated successfully")
                Toast.makeText(context, "Job updated successfully", Toast.LENGTH_SHORT).show()
                
                // Notify callback
                onJobUpdated?.invoke()
                
                // Close dialog
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating job", e)
                Toast.makeText(context, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Reset button state
                view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
            }
    }
    
    private fun confirmDelete(view: View) {
        // Create confirmation dialog
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete the job '${job.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteJob(view)
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        alertDialog.show()
        
        // Style the dialog buttons
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(R.color.status_rejected, requireContext().theme)
        )
    }
    
    private fun deleteJob(view: View) {
        // Show loading state
        view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.deleteButton).text = "Deleting..."
        
        // Delete job from Firestore
        db.collection("jobs").document(job.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Job deleted successfully")
                Toast.makeText(context, "Job deleted successfully", Toast.LENGTH_SHORT).show()
                
                // Notify callback
                onJobDeleted?.invoke()
                
                // Close dialog
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting job", e)
                Toast.makeText(context, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Reset button state
                view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = true
                view.findViewById<MaterialButton>(R.id.deleteButton).text = "Delete"
            }
    }
    
    companion object {
        private const val ARG_JOB = "job"
        
        fun newInstance(job: Job): AdminEditJobDialog {
            val fragment = AdminEditJobDialog()
            val args = Bundle()
            args.putParcelable(ARG_JOB, job)
            fragment.arguments = args
            return fragment
        }
    }
}
