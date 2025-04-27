package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class RecentJobsAdapter(
    private var jobs: List<Job> = emptyList(),
    private val onJobClick: (Job) -> Unit
) : RecyclerView.Adapter<RecentJobsAdapter.JobViewHolder>() {

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        private val companyNameText: TextView = itemView.findViewById(R.id.companyNameText)
        private val jobLocationText: TextView = itemView.findViewById(R.id.jobLocationText)
        private val postedDateText: TextView = itemView.findViewById(R.id.postedDateText)
        private val jobTypeChip: Chip = itemView.findViewById(R.id.jobTypeChip)

        fun bind(job: Job) {
            jobTitleText.text = job.title
            companyNameText.text = job.companyName
            jobLocationText.text = job.location
            jobTypeChip.text = job.type

            // Format the posted date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val postedDate = Date(job.getPostedDateMillis())
            val daysAgo = calculateDaysAgo(job.getPostedDateMillis())
            postedDateText.text = if (daysAgo == 0) "Posted today" else "Posted $daysAgo days ago"

            itemView.setOnClickListener { onJobClick(job) }
        }

        private fun calculateDaysAgo(timestamp: Long): Int {
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - timestamp
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(jobs[position])
    }

    override fun getItemCount(): Int = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
} 