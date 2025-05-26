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
    var onJobUpdated: (() -> Unit)? = null
    var onJobDeleted: (() -> Unit)? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
        job = arguments?.getParcelable(ARG_JOB) ?: Job()
        setupUI(view)
        view.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            saveChanges(view)
        }
        view.findViewById<MaterialButton>(R.id.deleteButton).setOnClickListener {
            confirmDelete(view)
        }
        if (job.id.isEmpty()) {
            view.findViewById<MaterialButton>(R.id.deleteButton).visibility = View.GONE
        }
    }
    private fun setupUI(view: View) {
        val isNewJob = job.id.isEmpty()
        view.findViewById<TextView>(R.id.dialogTitleTextView).text =
            if (isNewJob) "Add New Job" else "Edit Job: ${job.title}"
        view.findViewById<TextInputEditText>(R.id.jobTitleEditText).setText(job.title)
        view.findViewById<TextInputEditText>(R.id.companyNameEditText).setText(job.companyName)
        view.findViewById<TextInputEditText>(R.id.locationEditText).setText(job.location)
        view.findViewById<TextInputEditText>(R.id.salaryRangeEditText).setText(job.salary)
        view.findViewById<TextInputEditText>(R.id.descriptionEditText).setText(job.description)
        val jobTypeDropdown = view.findViewById<AutoCompleteTextView>(R.id.jobTypeDropdown)
        val jobTypeOptions = arrayOf("Full-time", "Part-time", "Contract", "Internship", "Temporary")
        val jobTypeAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, jobTypeOptions)
        jobTypeDropdown.setAdapter(jobTypeAdapter)
        val currentJobType = if (job.jobType.isNotEmpty()) {
            job.jobType.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } else {
            "Full-time" 
        }
        jobTypeDropdown.setText(currentJobType, false)
        val experienceLevelDropdown = view.findViewById<AutoCompleteTextView>(R.id.experienceLevelDropdown)
        val experienceLevelOptions = arrayOf("Entry Level", "Junior", "Mid-Level", "Senior", "Executive")
        val experienceLevelAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, experienceLevelOptions)
        experienceLevelDropdown.setAdapter(experienceLevelAdapter)
        val currentExperienceLevel = if (job.experienceLevel.isNotEmpty()) {
            job.experienceLevel.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } else {
            "Entry Level" 
        }
        experienceLevelDropdown.setText(currentExperienceLevel, false)
        val statusDropdown = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown)
        val statusOptions = arrayOf("Active", "Closed", "Draft", "Expired")
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, statusOptions)
        statusDropdown.setAdapter(statusAdapter)
        val currentStatus = if (job.status.isNotEmpty()) {
            job.status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } else {
            "Active" 
        }
        statusDropdown.setText(currentStatus, false)
    }
    private fun saveChanges(view: View) {
        val newTitle = view.findViewById<TextInputEditText>(R.id.jobTitleEditText).text.toString().trim()
        val newCompanyName = view.findViewById<TextInputEditText>(R.id.companyNameEditText).text.toString().trim()
        val newLocation = view.findViewById<TextInputEditText>(R.id.locationEditText).text.toString().trim()
        val newSalary = view.findViewById<TextInputEditText>(R.id.salaryRangeEditText).text.toString().trim()
        val newDescription = view.findViewById<TextInputEditText>(R.id.descriptionEditText).text.toString().trim()
        val newJobTypeCapitalized = view.findViewById<AutoCompleteTextView>(R.id.jobTypeDropdown).text.toString()
        val newJobType = newJobTypeCapitalized.lowercase()
        val newExperienceLevelCapitalized = view.findViewById<AutoCompleteTextView>(R.id.experienceLevelDropdown).text.toString()
        val newExperienceLevel = newExperienceLevelCapitalized.lowercase()
        val newStatusCapitalized = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown).text.toString()
        val newStatus = newStatusCapitalized.lowercase()
        if (newTitle.isBlank() || newCompanyName.isBlank()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.saveButton).text = "Saving..."
        val updatedJob = job.copy(
            title = newTitle,
            companyName = newCompanyName,
            location = newLocation,
            salary = newSalary,
            description = newDescription,
            jobType = newJobType,
            experienceLevel = newExperienceLevel,
            status = newStatus
        )
        if (job.id.isEmpty()) {
            val currentTime = com.google.firebase.Timestamp.now()
            val jobWithDate = updatedJob.copy(
                postedDate = currentTime,
                updatedDate = currentTime
            )
            db.collection("jobs")
                .add(jobWithDate)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Job created successfully with ID: ${documentReference.id}")
                    Toast.makeText(context, "Job created successfully", Toast.LENGTH_SHORT).show()
                    onJobUpdated?.invoke()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error creating job", e)
                    Toast.makeText(context, "Error creating job: ${e.message}", Toast.LENGTH_SHORT).show()
                    view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                    view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
                }
        } else {
            val jobWithUpdatedDate = updatedJob.copy(
                updatedDate = com.google.firebase.Timestamp.now()
            )
            db.collection("jobs").document(job.id)
                .set(jobWithUpdatedDate)
                .addOnSuccessListener {
                    Log.d(TAG, "Job updated successfully")
                    Toast.makeText(context, "Job updated successfully", Toast.LENGTH_SHORT).show()
                    onJobUpdated?.invoke()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating job", e)
                    Toast.makeText(context, "Error updating job: ${e.message}", Toast.LENGTH_SHORT).show()
                    view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                    view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
                }
        }
    }
    private fun confirmDelete(view: View) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete the job '${job.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteJob(view)
            }
            .setNegativeButton("Cancel", null)
            .create()
        alertDialog.show()
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(R.color.status_rejected, requireContext().theme)
        )
    }
    private fun deleteJob(view: View) {
        view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.deleteButton).text = "Deleting..."
        db.collection("jobs").document(job.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Job deleted successfully")
                Toast.makeText(context, "Job deleted successfully", Toast.LENGTH_SHORT).show()
                onJobDeleted?.invoke()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting job", e)
                Toast.makeText(context, "Error deleting job: ${e.message}", Toast.LENGTH_SHORT).show()
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
