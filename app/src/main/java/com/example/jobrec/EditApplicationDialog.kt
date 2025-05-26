package com.example.jobrec
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
class EditApplicationDialog : DialogFragment() {
    private lateinit var application: Application
    private val db = FirebaseFirestore.getInstance()
    companion object {
        private const val ARG_APPLICATION_ID = "application_id"
        fun newInstance(application: Application): EditApplicationDialog {
            val args = Bundle()
            args.putString(ARG_APPLICATION_ID, application.id)
            val fragment = EditApplicationDialog()
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val applicationId = arguments?.getString(ARG_APPLICATION_ID) ?: return
        loadApplication(applicationId)
    }
    private fun loadApplication(applicationId: String) {
        db.collection("applications").document(applicationId)
            .get()
            .addOnSuccessListener { document ->
                application = document.toObject(Application::class.java) ?: return@addOnSuccessListener
                application.id = document.id
                updateUI()
            }
    }
    private fun updateUI() {
        view?.let { view ->
            val jobTitleTextView = view.findViewById<TextView>(R.id.jobTitleTextView)
            val companyNameTextView = view.findViewById<TextView>(R.id.companyNameTextView)
            val applicantNameTextView = view.findViewById<TextView>(R.id.applicantNameTextView)
            val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
            val statusDropdown = view.findViewById<AutoCompleteTextView>(R.id.statusDropdown)
            val notesEditText = view.findViewById<TextInputEditText>(R.id.notesEditText)
            val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)
            val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)
            jobTitleTextView.text = application.jobTitle
            companyNameTextView.text = application.companyName
            applicantNameTextView.text = application.applicantName
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(application.appliedDate.toDate())
            notesEditText.setText(application.notes)
            val statuses = arrayOf("Pending", "Reviewed", "Shortlisted", "Rejected", "Hired")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
            statusDropdown.setAdapter(adapter)
            statusDropdown.setText(application.status, false)
            saveButton.setOnClickListener {
                val updatedStatus = statusDropdown.text.toString()
                val updatedNotes = notesEditText.text.toString()
                db.collection("applications").document(application.id)
                    .update(
                        mapOf(
                            "status" to updatedStatus,
                            "notes" to updatedNotes
                        )
                    )
                    .addOnSuccessListener {
                        dismiss()
                    }
            }
            deleteButton.setOnClickListener {
                db.collection("applications").document(application.id)
                    .delete()
                    .addOnSuccessListener {
                        dismiss()
                    }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_application, container, false)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}