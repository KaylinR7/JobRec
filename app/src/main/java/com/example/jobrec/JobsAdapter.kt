package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

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
        private val jobTitle: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val companyName: TextView = itemView.findViewById(R.id.companyNameTextView)
        private val jobLocation: TextView = itemView.findViewById(R.id.jobLocationTextView)
        private val jobType: TextView = itemView.findViewById(R.id.jobTypeTextView)
        private val jobSalary: TextView = itemView.findViewById(R.id.jobSalaryTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onJobClick(getItem(position))
                }
            }
        }

        fun bind(job: Job) {
            jobTitle.text = job.title
            companyName.text = job.companyName
            jobLocation.text = job.location
            jobType.text = job.type
            jobSalary.text = job.salary
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