package com.example.jobrec.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.JobAlert
import com.google.android.material.switchmaterial.SwitchMaterial
class JobAlertsAdapter(
    private val onDelete: (JobAlert) -> Unit,
    private val onToggle: (JobAlert) -> Unit
) : ListAdapter<JobAlert, JobAlertsAdapter.JobAlertViewHolder>(JobAlertDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobAlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_alert, parent, false)
        return JobAlertViewHolder(view)
    }
    override fun onBindViewHolder(holder: JobAlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    inner class JobAlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val keywordsText: TextView = itemView.findViewById(R.id.keywordsText)
        private val locationsText: TextView = itemView.findViewById(R.id.locationsText)
        private val jobTypesText: TextView = itemView.findViewById(R.id.jobTypesText)
        private val salaryRangeText: TextView = itemView.findViewById(R.id.salaryRangeText)
        private val activeSwitch: SwitchMaterial = itemView.findViewById(R.id.activeSwitch)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)
        fun bind(alert: JobAlert) {
            keywordsText.text = alert.keywords.joinToString(", ")
            locationsText.text = alert.locations.joinToString(", ")
            jobTypesText.text = alert.jobTypes.joinToString(", ")
            salaryRangeText.text = alert.salaryRange?.let { "${it.min} - ${it.max} ${it.currency}" } ?: "Any"
            activeSwitch.isChecked = alert.isActive
            activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != alert.isActive) {
                    onToggle(alert)
                }
            }
            deleteButton.setOnClickListener {
                onDelete(alert)
            }
        }
    }
    private class JobAlertDiffCallback : DiffUtil.ItemCallback<JobAlert>() {
        override fun areItemsTheSame(oldItem: JobAlert, newItem: JobAlert): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: JobAlert, newItem: JobAlert): Boolean {
            return oldItem == newItem
        }
    }
} 