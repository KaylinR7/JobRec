package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ApplicationAdapter(
    private var applications: List<JobApplication>,
    private val onApplicationClick: (JobApplication) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    class ApplicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val jobTitle: TextView = view.findViewById(R.id.jobTitle)
        val applicantName: TextView = view.findViewById(R.id.applicantName)
        val appliedDate: TextView = view.findViewById(R.id.appliedDate)
        val status: TextView = view.findViewById(R.id.status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = applications[position]
        
        holder.jobTitle.text = application.jobTitle
        holder.applicantName.text = application.applicantName
        holder.appliedDate.text = formatDate(application.appliedDate)
        holder.status.text = application.status

        holder.itemView.setOnClickListener {
            onApplicationClick(application)
        }
    }

    override fun getItemCount() = applications.size

    fun updateApplications(newApplications: List<JobApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Date): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(timestamp)
    }
} 