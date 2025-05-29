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
import java.text.SimpleDateFormat
import java.util.*
class RecentJobsAdapter(private val onJobClick: (Job) -> Unit) :
    ListAdapter<Job, RecentJobsAdapter.JobViewHolder>(JobDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_job, parent, false)
        return JobViewHolder(view)
    }
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }
    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.jobTitleText)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyNameText)
        private val locationTextView: TextView = itemView.findViewById(R.id.jobLocationText)
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val jobTypeChip: TextView = itemView.findViewById(R.id.jobTypeChip)
        private val postedDateText: TextView = itemView.findViewById(R.id.postedDateText)
        private val matchContainer: LinearLayout? = itemView.findViewById(R.id.matchContainer)
        private val matchPercentageText: TextView? = itemView.findViewById(R.id.matchPercentageText)

        fun bind(job: Job) {
            titleTextView.text = job.title
            companyTextView.text = job.companyName
            locationTextView.text = job.city
            jobTypeChip.text = job.type
            cardView.setOnClickListener { onJobClick(job) }

            // Format posted date
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val postedDate = job.postedDate.toDate()
            val now = Date()
            val diffInMillis = now.time - postedDate.time
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            val dateText = when {
                diffInDays == 0L -> "Posted today"
                diffInDays == 1L -> "Posted yesterday"
                diffInDays < 7 -> "Posted ${diffInDays} days ago"
                else -> "Posted ${dateFormat.format(postedDate)}"
            }
            postedDateText.text = dateText

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