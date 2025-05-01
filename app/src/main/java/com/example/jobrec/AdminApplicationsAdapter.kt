package com.example.jobrec

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.databinding.ItemAdminApplicationBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class AdminApplicationsAdapter(
    private val applications: List<Application>,
    private val onViewDetailsClick: (Application) -> Unit
) : RecyclerView.Adapter<AdminApplicationsAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(private val binding: ItemAdminApplicationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(application: Application, onViewDetailsClick: (Application) -> Unit) {
            binding.apply {
                jobTitleTextView.text = application.jobTitle
                companyNameTextView.text = application.companyName
                applicantNameTextView.text = application.applicantName
                dateTextView.text = formatDate(application.appliedDate)
                
                // Set status chip
                statusChip.text = application.status.capitalize()
                statusChip.setChipBackgroundColorResource(getStatusColor(application.status))
                
                // Set click listener
                viewDetailsButton.setOnClickListener { onViewDetailsClick(application) }
            }
        }

        private fun formatDate(timestamp: Timestamp): String {
            val date = timestamp.toDate()
            return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "pending" -> R.color.status_pending
                "shortlisted" -> R.color.status_shortlisted
                "rejected" -> R.color.status_rejected
                else -> R.color.status_pending
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemAdminApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApplicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position], onViewDetailsClick)
    }

    override fun getItemCount() = applications.size
} 