package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminJobsAdapter(
    private var jobs: List<Job>,
    private val onViewClick: (Job) -> Unit,
    private val onEditClick: (Job) -> Unit,
    private val onDeleteClick: (Job) -> Unit
) : RecyclerView.Adapter<AdminJobsAdapter.JobViewHolder>() {

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.jobTitleText)
        val companyText: TextView = itemView.findViewById(R.id.jobCompanyText)
        val viewButton: Button = itemView.findViewById(R.id.viewJobButton)
        val editButton: Button = itemView.findViewById(R.id.editJobButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteJobButton)
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