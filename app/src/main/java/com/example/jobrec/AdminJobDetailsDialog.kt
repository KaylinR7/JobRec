package com.example.jobrec
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
class AdminJobDetailsDialog : DialogFragment() {
    private lateinit var job: Job
    private val db = FirebaseFirestore.getInstance()
    companion object {
        private const val ARG_JOB_ID = "job_id"
        fun newInstance(job: Job): AdminJobDetailsDialog {
            val args = Bundle()
            args.putString(ARG_JOB_ID, job.id)
            val fragment = AdminJobDetailsDialog()
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jobId = arguments?.getString(ARG_JOB_ID) ?: return
        loadJob(jobId)
    }
    private fun loadJob(jobId: String) {
        db.collection("jobs").document(jobId)
            .get()
            .addOnSuccessListener { document ->
                job = document.toObject(Job::class.java)?.copy(id = document.id) ?: return@addOnSuccessListener
                updateUI()
            }
    }
    private fun updateUI() {
        view?.let { view ->
            val titleTextView = view.findViewById<TextView>(R.id.jobTitleTextView)
            val companyTextView = view.findViewById<TextView>(R.id.companyNameTextView)
            val locationTextView = view.findViewById<TextView>(R.id.locationTextView)
            val typeTextView = view.findViewById<TextView>(R.id.typeTextView)
            val salaryTextView = view.findViewById<TextView>(R.id.salaryTextView)
            val descriptionTextView = view.findViewById<TextView>(R.id.descriptionTextView)
            val requirementsTextView = view.findViewById<TextView>(R.id.requirementsTextView)
            val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
            val editButton = view.findViewById<MaterialButton>(R.id.editButton)
            val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)
            titleTextView.text = job.title
            companyTextView.text = job.companyName
            locationTextView.text = job.city
            typeTextView.text = job.type
            salaryTextView.text = job.salary
            descriptionTextView.text = job.description
            requirementsTextView.text = job.requirements
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(job.postedDate.toDate())
            editButton.setOnClickListener {
                dismiss()
            }
            deleteButton.setOnClickListener {
                db.collection("jobs").document(job.id)
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
        return inflater.inflate(R.layout.dialog_admin_job_details, container, false)
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