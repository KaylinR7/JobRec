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
        // Using the correct IDs from item_application.xml
        private val cardView: MaterialCardView = itemView as MaterialCardView // The root view is already a MaterialCardView
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitle)
        private val companyNameText: TextView = itemView.findViewById(R.id.companyName)
        private val appliedDateText: TextView = itemView.findViewById(R.id.appliedDate)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val jobLocationText: TextView = itemView.findViewById(R.id.jobLocation)
        private val jobSalaryText: TextView = itemView.findViewById(R.id.jobSalary)
        private val jobTypeText: TextView = itemView.findViewById(R.id.jobType)
        private val viewDetailsButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.viewDetailsButton)

        fun bind(application: Application) {
            // Set job title with proper handling of null/empty values
            jobTitleText.text = application.jobTitle?.takeIf { it.isNotBlank() } ?: "Unknown Job"

            // Set applicant name instead of company name
            val applicantName = try {
                // Try different field names for applicant name
                application.candidateName?.takeIf { it.isNotBlank() }
                    ?: try {
                        // Try to cast to the other Application class to access applicantName
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

            // Format and set applied date with more user-friendly format
            // Directly use the timestamp field which is working in the application details view
            val appliedDate = try {
                // First try to use timestamp directly (this is what works in the details view)
                val date = application.timestamp?.toDate()
                android.util.Log.d("RecentApplicationsAdapter", "Successfully got timestamp: $date")
                date
            } catch (e: Exception) {
                android.util.Log.e("RecentApplicationsAdapter", "Error getting timestamp: ${e.message}")
                // If that fails, try other fields as fallback
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

            // Handle nullable date safely
            val safeAppliedDate = appliedDate ?: java.util.Date()

            // Log the date for debugging
            android.util.Log.d("RecentApplicationsAdapter", "Applied date before formatting: $appliedDate")

            // Format date concisely as just "MMM dd, yyyy" without prefix
            val formattedDate = if (appliedDate == null) {
                "Unknown"
            } else {
                // Just use the date format (MMM dd, yyyy) for consistency and conciseness
                dateFormat.format(safeAppliedDate)
            }

            // Log the formatted date for debugging
            android.util.Log.d("RecentApplicationsAdapter", "Formatted date: $formattedDate")
            appliedDateText.text = formattedDate

            // Get status safely with fallback
            val status = try {
                application.status
            } catch (e: Exception) {
                "pending" // Default status if not found
            }

            // Set status chip with capitalized text
            val statusText = status.capitalize()
            statusChip.text = statusText

            // Set status chip color based on status
            val chipColor = when (status.lowercase()) {
                "applied" -> R.color.status_applied
                "reviewed" -> R.color.status_reviewed
                "shortlisted" -> R.color.status_shortlisted
                "interviewed" -> R.color.status_interviewed
                "offered" -> R.color.status_offered
                "accepted" -> R.color.status_accepted // Use green for accepted status
                "rejected" -> R.color.status_rejected
                "pending" -> R.color.status_pending
                else -> R.color.status_applied
            }

            // Apply color to chip
            statusChip.setChipBackgroundColorResource(chipColor)

            // Make text white for better contrast
            statusChip.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))

            // Set additional job details with concise values
            // These fields might not exist in all Application classes, so handle them safely
            try {
                // Try to get job location - keep it short
                val location = "Remote" // Default value
                jobLocationText.text = location

                // Try to get job salary - keep it short
                val salary = "Competitive" // Default value
                jobSalaryText.text = salary

                // Try to get job type - keep it short
                val type = "Full-time" // Default value
                jobTypeText.text = type
            } catch (e: Exception) {
                // Set concise default values if there's an error
                jobLocationText.text = "N/A"
                jobSalaryText.text = "N/A"
                jobTypeText.text = "N/A"
            }

            // Add ripple effect to card
            cardView.isClickable = true
            cardView.isFocusable = true

            // Set click listener for the view details button
            viewDetailsButton.setOnClickListener {
                onApplicationClick(application)
            }

            // Set click listener for the card with small delay to show ripple
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

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
