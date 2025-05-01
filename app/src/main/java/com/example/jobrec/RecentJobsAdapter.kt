package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class RecentJobsAdapter(private val onJobClick: (Job) -> Unit) : 
    ListAdapter<Job, RecentJobsAdapter.JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyNameTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.jobCard)
        private val jobTypeChip: Chip = itemView.findViewById(R.id.jobTypeChip)
        private val postedDateText: TextView = itemView.findViewById(R.id.postedDateText)

        fun bind(job: Job) {
            titleTextView.text = job.title
            companyTextView.text = job.companyName
            locationTextView.text = job.location
            jobTypeChip.text = job.type

            // Format the posted date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val postedDate = Date(job.getPostedDateMillis())
            val daysAgo = calculateDaysAgo(job.getPostedDateMillis())
            postedDateText.text = if (daysAgo == 0) "Posted today" else "Posted $daysAgo days ago"

            cardView.setOnClickListener { onJobClick(job) }
        }

        private fun calculateDaysAgo(timestamp: Long): Int {
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - timestamp
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    private class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem == newItem
        }
    }
} 