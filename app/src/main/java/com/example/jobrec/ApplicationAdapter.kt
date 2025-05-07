package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicationAdapter(
    private val applications: List<ApplicationsActivity.Application>,
    private val onItemClick: (ApplicationsActivity.Application) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_company_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount() = applications.size

    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        private val applicantNameText: TextView = itemView.findViewById(R.id.applicantNameText)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)

        fun bind(application: ApplicationsActivity.Application) {
            jobTitleText.text = application.jobTitle
            applicantNameText.text = application.applicantName
            
            // Format and set the date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(application.timestamp)
            
            // Set status chip
            statusChip.text = application.status.capitalize()
            statusChip.setChipBackgroundColorResource(
                when (application.status.lowercase()) {
                    "pending" -> R.color.status_pending
                    "reviewed" -> R.color.status_pending  // Using pending color for reviewed
                    "accepted" -> R.color.status_accepted
                    "rejected" -> R.color.status_rejected
                    else -> R.color.status_pending
                }
            )

            itemView.setOnClickListener { onItemClick(application) }
        }
    }
} 