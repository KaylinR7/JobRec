package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ApplicationsAdapter(private val onItemClick: (JobApplication) -> Unit) : 
    RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder>() {

    private var applications: List<JobApplication> = emptyList()
    private val db = FirebaseFirestore.getInstance()

    fun updateApplications(newApplications: List<JobApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val applicantName: TextView = itemView.findViewById(R.id.applicantName)
        private val jobTitle: TextView = itemView.findViewById(R.id.jobTitle)
        private val appliedDate: TextView = itemView.findViewById(R.id.appliedDate)
        private val status: TextView = itemView.findViewById(R.id.status)

        fun bind(application: JobApplication) {
            // Load applicant name from Firestore
            db.collection("Users")
                .document(application.userId)
                .get()
                .addOnSuccessListener { document: DocumentSnapshot ->
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    applicantName.text = "$firstName $lastName"
                }

            // Load job title from Firestore
            db.collection("jobs")
                .document(application.jobId)
                .get()
                .addOnSuccessListener { document: DocumentSnapshot ->
                    jobTitle.text = document.getString("title") ?: "Unknown Job"
                }

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            appliedDate.text = "Applied: ${dateFormat.format(application.appliedDate)}"
            
            // Set status
            status.text = application.status

            itemView.setOnClickListener { onItemClick(application) }
        }
    }
} 