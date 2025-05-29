package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class RecommendedJobsAdapter(private val onJobClick: (Job) -> Unit) :
    ListAdapter<Job, RecommendedJobsAdapter.JobViewHolder>(JobDiffCallback()) {

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
        private val titleTextView: TextView = itemView.findViewById(R.id.jobTitleText)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyNameText)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationText)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.jobCard)
        private val jobTypeText: TextView = itemView.findViewById(R.id.jobTypeText)
        private val salaryText: TextView? = itemView.findViewById(R.id.salaryText)
        private val matchContainer: LinearLayout? = itemView.findViewById(R.id.matchContainer)
        private val matchPercentageText: TextView? = itemView.findViewById(R.id.matchPercentageText)

        fun bind(job: Job) {
            titleTextView.text = job.title
            companyTextView.text = job.companyName
            locationTextView.text = job.city
            jobTypeText.text = job.type
            salaryText?.text = job.salary
            cardView.setOnClickListener { onJobClick(job) }

            // Show match percentage if available
            if (job.matchPercentage > 0 && matchContainer != null && matchPercentageText != null) {
                matchContainer.visibility = View.VISIBLE
                matchPercentageText.text = "${job.matchPercentage}%"

                val matchColor = when {
                    job.matchPercentage >= 80 -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    job.matchPercentage >= 60 -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                    else -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                }
                matchPercentageText.setTextColor(matchColor)
            } else {
                matchContainer?.visibility = View.GONE
            }
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
