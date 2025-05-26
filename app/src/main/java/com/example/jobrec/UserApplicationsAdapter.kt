package com.example.jobrec
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
class UserApplicationsAdapter(private val onItemClick: (Application) -> Unit) :
    RecyclerView.Adapter<UserApplicationsAdapter.ApplicationViewHolder>() {
    private var applications: List<Application> = emptyList()
    private val db = FirebaseFirestore.getInstance()
    fun updateApplications(newApplications: List<Application>) {
        applications = newApplications
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_application, parent, false)
        return ApplicationViewHolder(view)
    }
    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }
    override fun getItemCount(): Int = applications.size
    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitle: TextView = itemView.findViewById(R.id.jobTitle)
        private val companyName: TextView = itemView.findViewById(R.id.companyName)
        private val appliedDate: TextView = itemView.findViewById(R.id.appliedDate)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        fun bind(application: Application) {
            jobTitle.text = application.jobTitle
            companyName.text = application.companyName
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            appliedDate.text = "Applied: ${dateFormat.format(application.appliedDate.toDate())}"
            statusChip.text = application.status
            statusChip.setChipBackgroundColorResource(
                when (application.status) {
                    "PENDING" -> R.color.status_pending
                    "REVIEWING" -> R.color.status_reviewing
                    "SHORTLISTED" -> R.color.status_shortlisted
                    "INTERVIEWING" -> R.color.status_interviewing
                    "OFFERED" -> R.color.status_offered
                    "REJECTED" -> R.color.status_rejected
                    else -> R.color.status_pending
                }
            )
            itemView.setOnClickListener { onItemClick(application) }
        }
    }
}