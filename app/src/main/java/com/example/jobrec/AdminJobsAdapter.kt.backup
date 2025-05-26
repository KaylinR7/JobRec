package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AdminJobsAdapter(
    private var jobs: List<Job>,
    private val onViewClick: (Job) -> Unit,
    private val onEditClick: (Job) -> Unit,
    private val onDeleteClick: (Job) -> Unit
) : RecyclerView.Adapter<AdminJobsAdapter.JobViewHolder>() {

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.jobTitleText)
        val companyText: TextView = itemView.findViewById(R.id.jobCompanyText)
        val statusText: TextView = itemView.findViewById(R.id.jobStatusText)
        val viewButton: MaterialButton = itemView.findViewById(R.id.viewJobButton)
        val editButton: MaterialButton = itemView.findViewById(R.id.editJobButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteJobButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.titleText.text = job.title
        holder.companyText.text = job.companyName

        // Set status with appropriate color
        val statusText = "Status: ${job.status.replaceFirstChar { it.uppercase() }}"
        holder.statusText.text = statusText

        // Set status color based on status
        val statusColor = when (job.status.lowercase()) {
            "active" -> "#4CAF50" // Green
            "inactive" -> "#FF9800" // Orange
            "expired" -> "#F44336" // Red
            "draft" -> "#2196F3" // Blue
            else -> "#757575" // Gray
        }
        holder.statusText.setTextColor(android.graphics.Color.parseColor(statusColor))

        holder.viewButton.setOnClickListener { onViewClick(job) }
        holder.editButton.setOnClickListener { onEditClick(job) }
        holder.deleteButton.setOnClickListener { onDeleteClick(job) }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }

    val jobsList: List<Job>
        get() = jobs
}