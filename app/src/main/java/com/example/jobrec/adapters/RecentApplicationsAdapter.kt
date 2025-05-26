package com.example.jobrec.adapters
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.Application
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class RecentApplicationsAdapter(
    private val onApplicationClick: (Application) -> Unit
) : ListAdapter<Application, RecentApplicationsAdapter.ApplicationViewHolder>(ApplicationDiffCallback()) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }
    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = getItem(position)
        holder.bind(application)
    }
    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView 
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitle)
        private val companyNameText: TextView = itemView.findViewById(R.id.companyName)
        private val appliedDateText: TextView = itemView.findViewById(R.id.appliedDate)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val jobLocationText: TextView = itemView.findViewById(R.id.jobLocation)
        private val jobSalaryText: TextView = itemView.findViewById(R.id.jobSalary)
        private val jobTypeText: TextView = itemView.findViewById(R.id.jobType)
        private val viewDetailsButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
        fun bind(application: Application) {
            jobTitleText.text = application.jobTitle?.takeIf { it.isNotBlank() } ?: "Unknown Job"
            val applicantName = try {
                application.candidateName?.takeIf { it.isNotBlank() }
                    ?: try {
                        val mainApp = application as? com.example.jobrec.Application
                        mainApp?.applicantName?.takeIf { name -> name.isNotBlank() }
                    } catch (e: Exception) {
                        null
                    }
                    ?: "Unknown Applicant"
            } catch (e: Exception) {
                "Unknown Applicant"
            }
            companyNameText.text = applicantName
            val appliedDate = try {
                val date = application.timestamp?.toDate()
                android.util.Log.d("RecentApplicationsAdapter", "Successfully got timestamp: $date")
                date
            } catch (e: Exception) {
                android.util.Log.e("RecentApplicationsAdapter", "Error getting timestamp: ${e.message}")
                try {
                    val date = (application as? com.example.jobrec.Application)?.appliedDate?.toDate()
                    android.util.Log.d("RecentApplicationsAdapter", "Successfully got appliedDate: $date")
                    date
                } catch (e2: Exception) {
                    android.util.Log.e("RecentApplicationsAdapter", "Error getting appliedDate: ${e2.message}")
                    try {
                        val date = application.appliedAt?.toDate()
                        android.util.Log.d("RecentApplicationsAdapter", "Successfully got appliedAt: $date")
                        date
                    } catch (e3: Exception) {
                        android.util.Log.e("RecentApplicationsAdapter", "Error getting appliedAt: ${e3.message}")
                        null
                    }
                }
            }
            val now = Calendar.getInstance().time
            val safeAppliedDate = appliedDate ?: java.util.Date()
            android.util.Log.d("RecentApplicationsAdapter", "Applied date before formatting: $appliedDate")
            val formattedDate = if (appliedDate == null) {
                "Unknown"
            } else {
                dateFormat.format(safeAppliedDate)
            }
            android.util.Log.d("RecentApplicationsAdapter", "Formatted date: $formattedDate")
            appliedDateText.text = formattedDate
            val status = try {
                application.status
            } catch (e: Exception) {
                "pending" 
            }
            val statusText = status.capitalize()
            statusChip.text = statusText
            val chipColor = when (status.lowercase()) {
                "applied" -> R.color.status_applied
                "reviewed" -> R.color.status_reviewed
                "shortlisted" -> R.color.status_shortlisted
                "interviewed" -> R.color.status_interviewed
                "offered" -> R.color.status_offered
                "accepted" -> R.color.status_accepted 
                "rejected" -> R.color.status_rejected
                "pending" -> R.color.status_pending
                else -> R.color.status_applied
            }
            statusChip.setChipBackgroundColorResource(chipColor)
            statusChip.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            try {
                val location = "Remote" 
                jobLocationText.text = location
                val salary = "Competitive" 
                jobSalaryText.text = salary
                val type = "Full-time" 
                jobTypeText.text = type
            } catch (e: Exception) {
                jobLocationText.text = "N/A"
                jobSalaryText.text = "N/A"
                jobTypeText.text = "N/A"
            }
            cardView.isClickable = true
            cardView.isFocusable = true
            viewDetailsButton.setOnClickListener {
                onApplicationClick(application)
            }
            cardView.setOnClickListener {
                cardView.postDelayed({
                    onApplicationClick(application)
                }, 100)
            }
        }
    }
    class ApplicationDiffCallback : DiffUtil.ItemCallback<Application>() {
        override fun areItemsTheSame(oldItem: Application, newItem: Application): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Application, newItem: Application): Boolean {
            return oldItem == newItem
        }
    }
}
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
