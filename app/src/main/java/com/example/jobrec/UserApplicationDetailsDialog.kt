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
    private lateinit var application: Application
    companion object {
        private const val ARG_APPLICATION = "application"
        fun newInstance(application: Application): UserApplicationDetailsDialog {
            val args = Bundle().apply {
                putString("applicationId", application.id)
            }
            return UserApplicationDetailsDialog().apply {
                arguments = args
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val applicationId = arguments?.getString("applicationId")
        if (applicationId != null) {
            loadApplication(applicationId)
        } else {
            dismiss()
        }
    }
    private fun loadApplication(applicationId: String) {
        db.collection("applications").document(applicationId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    application = document.toObject(Application::class.java) ?: return@addOnSuccessListener
                    application.id = document.id
                    updateUI()
                } else {
                    dismiss()
                }
            }
            .addOnFailureListener {
                dismiss()
            }
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
        jobTitle = view.findViewById(R.id.jobTitle)
        companyName = view.findViewById(R.id.companyName)
        appliedDate = view.findViewById(R.id.appliedDate)
        status = view.findViewById(R.id.status)
        coverLetter = view.findViewById(R.id.coverLetter)
        downloadCvButton = view.findViewById(R.id.downloadCvButton)
        downloadCvButton.setOnClickListener {
            downloadCv()
        }
    }
    private fun updateUI() {
        if (!this::application.isInitialized) return
        jobTitle.text = application.jobTitle
        companyName.text = application.companyName
        coverLetter.text = application.coverLetter
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        appliedDate.text = "Applied: ${dateFormat.format(application.appliedDate.toDate())}"
        status.text = "Status: ${application.status}"
    }
    private fun downloadCv() {
        if (application.cvUrl.isEmpty()) {
            Toast.makeText(context, "No CV available", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, "Downloading CV...", Toast.LENGTH_SHORT).show()
    }
}