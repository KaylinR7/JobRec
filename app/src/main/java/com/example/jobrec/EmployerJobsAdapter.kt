package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
class EmployerJobsAdapter(
    private var jobs: List<Job>,
    private val onEditClick: (Job) -> Unit,
    private val onDeleteClick: (Job) -> Unit
) : RecyclerView.Adapter<EmployerJobsAdapter.JobViewHolder>() {
    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        val jobLocationText: TextView = itemView.findViewById(R.id.jobLocationText)
        val jobTypeText: TextView = itemView.findViewById(R.id.jobTypeText)
        val jobStatusText: TextView = itemView.findViewById(R.id.jobStatusText)
        val editButton: MaterialButton = itemView.findViewById(R.id.editJobButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteJobButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employer_job, parent, false)
        return JobViewHolder(view)
    }
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.jobTitleText.text = job.title
        holder.jobLocationText.text = job.city
        holder.jobTypeText.text = job.type
        holder.jobStatusText.text = job.status.capitalize()
        holder.jobStatusText.setBackgroundResource(
            when (job.status.lowercase()) {
                "active" -> android.R.color.holo_green_light
                "closed" -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
            }
        )
        holder.editButton.setOnClickListener { onEditClick(job) }
        holder.deleteButton.setOnClickListener { onDeleteClick(job) }
    }
    override fun getItemCount() = jobs.size
    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
}