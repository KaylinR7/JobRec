package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AdminApplicationsAdapter(
    private var applications: List<JobApplication>,
    private val onViewDetailsClick: (JobApplication) -> Unit
) : RecyclerView.Adapter<AdminApplicationsAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val jobTitleTextView: TextView = view.findViewById(R.id.jobTitleTextView)
        val statusTextView: TextView = view.findViewById(R.id.statusTextView)
        val companyNameTextView: TextView = view.findViewById(R.id.companyNameTextView)
        val applicantNameTextView: TextView = view.findViewById(R.id.applicantNameTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val viewDetailsButton: MaterialButton = view.findViewById(R.id.viewDetailsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = applications[position]
        holder.jobTitleTextView.text = application.jobTitle
        holder.statusTextView.text = application.status
        holder.companyNameTextView.text = application.companyId // TODO: Load company name
        holder.applicantNameTextView.text = application.applicantName
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.dateTextView.text = dateFormat.format(application.appliedDate)
        
        // Set status text color based on status
        when (application.status.toLowerCase()) {
            "pending" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.orange))
            "accepted" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.green))
            "rejected" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.red))
        }
        
        holder.viewDetailsButton.setOnClickListener {
            onViewDetailsClick(application)
        }
    }

    override fun getItemCount(): Int = applications.size

    fun updateApplications(newApplications: List<JobApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }
} 