package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JobAdapter(
    private var jobs: List<Job>,
    private val onJobClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.jobTitleTextView)
        val companyText: TextView = view.findViewById(R.id.companyNameTextView)
        val locationText: TextView = view.findViewById(R.id.jobLocationTextView)
        val salaryText: TextView = view.findViewById(R.id.jobSalaryTextView)
        val typeText: TextView = view.findViewById(R.id.jobTypeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]

        holder.titleText.text = job.title
        holder.companyText.text = job.companyName
        holder.locationText.text = job.location
        holder.salaryText.text = job.salary
        holder.typeText.text = job.type

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onJobClick(job)
        }
    }

    override fun getItemCount() = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
} 