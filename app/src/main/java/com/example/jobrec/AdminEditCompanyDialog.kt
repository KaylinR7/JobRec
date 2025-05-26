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
import java.util.Locale
class AdminEditCompanyDialog : DialogFragment() {
    private lateinit var company: Company
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminEditCompanyDialog"
    var onCompanyUpdated: (() -> Unit)? = null
    var onCompanyDeleted: (() -> Unit)? = null
    private var companyId: String = ""
    private var isNewCompany: Boolean = false
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
        return inflater.inflate(R.layout.dialog_admin_edit_company, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        company = arguments?.getParcelable(ARG_COMPANY) ?: Company()
        companyId = arguments?.getString(ARG_COMPANY_ID) ?: ""
        isNewCompany = companyId.isEmpty()
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
        if (isNewCompany) {
            view.findViewById<MaterialButton>(R.id.deleteButton).visibility = View.GONE
        }
    }
    private fun setupUI(view: View) {
        val dialogTitle = if (isNewCompany) "Add New Company" else "Edit Company: ${company.companyName}"
        view.findViewById<TextView>(R.id.dialogTitleTextView).text = dialogTitle
        view.findViewById<TextInputEditText>(R.id.companyNameEditText).setText(company.companyName)
        view.findViewById<TextInputEditText>(R.id.industryEditText).setText(company.industry)
        view.findViewById<TextInputEditText>(R.id.locationEditText).setText(company.location)
        view.findViewById<TextInputEditText>(R.id.websiteEditText).setText(company.website)
        view.findViewById<TextInputEditText>(R.id.emailEditText).setText(company.email)
        view.findViewById<TextInputEditText>(R.id.descriptionEditText).setText(company.description)
        if (!isNewCompany) {
            view.findViewById<TextInputEditText>(R.id.emailEditText).isEnabled = false
        }
        val statusDropdown = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown)
        val statusOptions = arrayOf("Active", "Inactive", "Pending", "Suspended")
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, statusOptions)
        statusDropdown.setAdapter(statusAdapter)
        val currentStatus = if (company.status.isNotEmpty()) {
            company.status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } else {
            "Active" 
        }
        statusDropdown.setText(currentStatus, false)
    }
    private fun saveChanges(view: View) {
        val newCompanyName = view.findViewById<TextInputEditText>(R.id.companyNameEditText).text.toString().trim()
        val newIndustry = view.findViewById<TextInputEditText>(R.id.industryEditText).text.toString().trim()
        val newLocation = view.findViewById<TextInputEditText>(R.id.locationEditText).text.toString().trim()
        val newWebsite = view.findViewById<TextInputEditText>(R.id.websiteEditText).text.toString().trim()
        val newEmail = view.findViewById<TextInputEditText>(R.id.emailEditText).text.toString().trim()
        val newDescription = view.findViewById<TextInputEditText>(R.id.descriptionEditText).text.toString().trim()
        val newStatusCapitalized = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown).text.toString()
        val newStatus = newStatusCapitalized.lowercase()
        if (newCompanyName.isBlank() || newEmail.isBlank()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.saveButton).text = "Saving..."
        val updatedCompany = Company(
            companyName = newCompanyName,
            industry = newIndustry,
            location = newLocation,
            website = newWebsite,
            email = newEmail,
            description = newDescription,
            status = newStatus
        )
        if (isNewCompany) {
            db.collection("companies")
                .add(updatedCompany)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Company created successfully with ID: ${documentReference.id}")
                    Toast.makeText(context, "Company created successfully", Toast.LENGTH_SHORT).show()
                    onCompanyUpdated?.invoke()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error creating company", e)
                    Toast.makeText(context, "Error creating company: ${e.message}", Toast.LENGTH_SHORT).show()
                    view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                    view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
                }
        } else {
            db.collection("companies").document(companyId)
                .update(
                    mapOf(
                        "companyName" to newCompanyName,
                        "industry" to newIndustry,
                        "location" to newLocation,
                        "website" to newWebsite,
                        "email" to newEmail,
                        "description" to newDescription,
                        "status" to newStatus
                    )
                )
                .addOnSuccessListener {
                    Log.d(TAG, "Company updated successfully")
                    Toast.makeText(context, "Company updated successfully", Toast.LENGTH_SHORT).show()
                    onCompanyUpdated?.invoke()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating company", e)
                    Toast.makeText(context, "Error updating company: ${e.message}", Toast.LENGTH_SHORT).show()
                    view.findViewById<MaterialButton>(R.id.saveButton).isEnabled = true
                    view.findViewById<MaterialButton>(R.id.saveButton).text = "Save Changes"
                }
        }
    }
    private fun confirmDelete(view: View) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Company")
            .setMessage("Are you sure you want to delete the company '${company.companyName}'? This will also delete all associated jobs.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCompany(view)
            }
            .setNegativeButton("Cancel", null)
            .create()
        alertDialog.show()
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(R.color.status_rejected, requireContext().theme)
        )
    }
    private fun deleteCompany(view: View) {
        view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = false
        view.findViewById<MaterialButton>(R.id.deleteButton).text = "Deleting..."
        db.collection("companies").document(companyId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Company deleted successfully")
                db.collection("jobs")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .addOnSuccessListener { documents ->
                        val batch = db.batch()
                        for (document in documents) {
                            batch.delete(db.collection("jobs").document(document.id))
                        }
                        if (documents.isEmpty) {
                            Toast.makeText(context, "Company deleted successfully", Toast.LENGTH_SHORT).show()
                            onCompanyDeleted?.invoke()
                            dismiss()
                        } else {
                            batch.commit()
                                .addOnSuccessListener {
                                    Log.d(TAG, "All associated jobs deleted successfully")
                                    Toast.makeText(context, "Company and all associated jobs deleted successfully", Toast.LENGTH_SHORT).show()
                                    onCompanyDeleted?.invoke()
                                    dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error deleting associated jobs", e)
                                    Toast.makeText(context, "Company deleted but error deleting associated jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onCompanyDeleted?.invoke()
                                    dismiss()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error querying associated jobs", e)
                        Toast.makeText(context, "Company deleted but error querying associated jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                        onCompanyDeleted?.invoke()
                        dismiss()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting company", e)
                Toast.makeText(context, "Error deleting company: ${e.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<MaterialButton>(R.id.deleteButton).isEnabled = true
                view.findViewById<MaterialButton>(R.id.deleteButton).text = "Delete"
            }
    }
    companion object {
        private const val ARG_COMPANY = "company"
        private const val ARG_COMPANY_ID = "company_id"
        fun newInstance(company: Company, companyId: String = ""): AdminEditCompanyDialog {
            val fragment = AdminEditCompanyDialog()
            val args = Bundle()
            args.putParcelable(ARG_COMPANY, company)
            args.putString(ARG_COMPANY_ID, companyId)
            fragment.arguments = args
            return fragment
        }
    }
}
