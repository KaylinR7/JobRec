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
class AdminEditApplicationDialog : DialogFragment() {
    private lateinit var application: Application
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminEditApplicationDialog"
    var onApplicationUpdated: (() -> Unit)? = null
    var onApplicationDeleted: (() -> Unit)? = null
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
        return inflater.inflate(R.layout.dialog_admin_edit_application, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        application = arguments?.getParcelable(ARG_APPLICATION) ?: return
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
    }
    private fun setupUI(view: View) {
        view.findViewById<TextView>(R.id.dialogTitleTextView).text = "Edit Application: ${application.jobTitle}"
        view.findViewById<TextView>(R.id.jobTitleTextView).text = application.jobTitle
        view.findViewById<TextView>(R.id.companyNameTextView).text = application.companyName
        view.findViewById<TextView>(R.id.applicantNameTextView).text = application.applicantName
        view.findViewById<TextView>(R.id.applicantEmailTextView).text = application.applicantEmail
        view.findViewById<TextView>(R.id.applicantPhoneTextView).text = application.applicantPhone
        try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val appliedDate = "Applied on: ${dateFormat.format(application.appliedDate.toDate())}"
            view.findViewById<TextView>(R.id.appliedDateTextView).text = appliedDate
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}")
            view.findViewById<TextView>(R.id.appliedDateTextView).text = "Applied date: Unknown"
        }
        val statusDropdown = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown)
        val statusOptions = arrayOf("Pending", "Reviewed", "Shortlisted", "Accepted", "Rejected")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, statusOptions)
        statusDropdown.setAdapter(adapter)
        val currentStatus = application.status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        statusDropdown.setText(currentStatus, false)
        view.findViewById<TextInputEditText>(R.id.notesEditText).setText(application.notes)
    }
    private fun saveChanges(view: View) {
        val newStatusCapitalized = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown).text.toString()
        val newStatus = newStatusCapitalized.lowercase()
        val newNotes = view.findViewById<TextInputEditText>(R.id.notesEditText).text.toString()
        if (newStatus.isBlank()) {
            Toast.makeText(context, "Please select a status", Toast.LENGTH_SHORT).show()
            return
        }
        view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.saveButton).text = "Saving..."
        db.collection("applications").document(application.id)
            .update(
                mapOf(
                    "status" to newStatus,
                    "notes" to newNotes
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Application updated successfully")
                Toast.makeText(context, "Application updated successfully", Toast.LENGTH_SHORT).show()
                onApplicationUpdated?.invoke()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating application", e)
                Toast.makeText(context, "Error updating application: ${e.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
            }
    }
    private fun confirmDelete(view: View) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Application")
            .setMessage("Are you sure you want to delete this application from ${application.applicantName} for ${application.jobTitle}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteApplication(view)
            }
            .setNegativeButton("Cancel", null)
            .create()
        alertDialog.show()
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(R.color.status_rejected, requireContext().theme)
        )
    }
    private fun deleteApplication(view: View) {
        view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.deleteButton).text = "Deleting..."
        db.collection("applications").document(application.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Application deleted successfully")
                Toast.makeText(context, "Application deleted successfully", Toast.LENGTH_SHORT).show()
                onApplicationDeleted?.invoke()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting application", e)
                Toast.makeText(context, "Error deleting application: ${e.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = true
                view.findViewById<MaterialButton>(R.id.deleteButton).text = "Delete"
            }
    }
    companion object {
        private const val ARG_APPLICATION = "application"
        fun newInstance(application: Application): AdminEditApplicationDialog {
            val fragment = AdminEditApplicationDialog()
            val args = Bundle()
            args.putParcelable(ARG_APPLICATION, application)
            fragment.arguments = args
            return fragment
        }
    }
}
