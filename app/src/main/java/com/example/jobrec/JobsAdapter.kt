package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
class JobsAdapter(private val onJobClick: (Job) -> Unit) :
    ListAdapter<Job, JobsAdapter.JobViewHolder>(JobDiffCallback()) {
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
        private val jobTypeTextView: TextView = itemView.findViewById(R.id.jobTypeText)
        private val salaryTextView: TextView = itemView.findViewById(R.id.salaryText)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onJobClick(getItem(position))
                }
            }
        }
        fun bind(job: Job) {
            titleTextView.text = job.title
            companyTextView.text = job.companyName
            locationTextView.text = job.location
            jobTypeTextView.text = job.type
            salaryTextView.text = job.salary
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