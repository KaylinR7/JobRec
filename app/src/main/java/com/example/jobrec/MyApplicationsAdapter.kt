package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
class MyApplicationsAdapter(
    private val applications: List<ApplicationsActivity.Application>,
    private val onItemClick: (ApplicationsActivity.Application) -> Unit
) : RecyclerView.Adapter<MyApplicationsAdapter.ApplicationViewHolder>() {
    private val db = FirebaseFirestore.getInstance()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }
    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }
    override fun getItemCount() = applications.size
    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitle: TextView = itemView.findViewById(R.id.jobTitle)
        private val companyName: TextView = itemView.findViewById(R.id.companyName)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val appliedDate: TextView = itemView.findViewById(R.id.appliedDate)
        private val jobLocation: TextView = itemView.findViewById(R.id.jobLocation)
        private val jobSalary: TextView = itemView.findViewById(R.id.jobSalary)
        private val jobType: TextView = itemView.findViewById(R.id.jobType)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
        fun bind(application: ApplicationsActivity.Application) {
            jobTitle.text = application.jobTitle
            companyName.text = "Company: Loading..."
            try {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                appliedDate.text = dateFormat.format(application.timestamp)
            } catch (e: Exception) {
                android.util.Log.e("MyApplicationsAdapter", "Error formatting date: ${e.message}")
                appliedDate.text = "Unknown date"
            }
            statusChip.text = application.status.capitalize()
            statusChip.setChipBackgroundColorResource(
                when (application.status.lowercase()) {
                    "pending" -> R.color.status_pending
                    "reviewed" -> R.color.status_reviewed
                    "accepted" -> R.color.status_accepted
                    "rejected" -> R.color.status_rejected
                    else -> R.color.status_pending
                }
            )
            loadJobDetails(application.jobId)
            viewDetailsButton.setOnClickListener { onItemClick(application) }
        }
        private fun loadJobDetails(jobId: String) {
            if (jobId.isEmpty()) {
                jobLocation.text = "N/A"
                jobSalary.text = "N/A"
                jobType.text = "N/A"
                return
            }
            db.collection("jobs").document(jobId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val companyId = document.getString("companyId") ?: ""
                        if (companyId.isNotEmpty()) {
                            loadCompanyName(companyId)
                        }
                        jobLocation.text = document.getString("location") ?: "N/A"
                        jobSalary.text = document.getString("salary") ?: "N/A"
                        jobType.text = document.getString("type") ?: "N/A"
                    } else {
                        jobLocation.text = "N/A"
                        jobSalary.text = "N/A"
                        jobType.text = "N/A"
                    }
                }
                .addOnFailureListener {
                    jobLocation.text = "N/A"
                    jobSalary.text = "N/A"
                    jobType.text = "N/A"
                }
        }
        private fun loadCompanyName(companyId: String) {
            db.collection("companies").document(companyId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("companyName") ?: document.getString("name") ?: "Unknown Company"
                        companyName.text = name
                        android.util.Log.d("MyApplicationsAdapter", "Loaded company name: $name for ID: $companyId")
                    } else {
                        companyName.text = "Unknown Company"
                        android.util.Log.d("MyApplicationsAdapter", "Company document doesn't exist for ID: $companyId")
                    }
                }
                .addOnFailureListener { e ->
                    companyName.text = "Unknown Company"
                    android.util.Log.e("MyApplicationsAdapter", "Failed to load company name for ID: $companyId", e)
                }
        }
    }
    private fun String.capitalize(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
