package com.example.jobrec
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.databinding.ItemAdminApplicationBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
class AdminApplicationsAdapter(
    private val applications: List<Application>,
    private val onViewDetailsClick: (Application) -> Unit
) : RecyclerView.Adapter<AdminApplicationsAdapter.ApplicationViewHolder>() {
    private val TAG = "AdminApplicationsAdapter"
    init {
        Log.d(TAG, "Initialized with ${applications.size} applications")
    }
    class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val companyNameTextView: TextView = itemView.findViewById(R.id.companyNameTextView)
        private val applicantNameTextView: TextView = itemView.findViewById(R.id.applicantNameTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
        fun bind(application: Application, onViewDetailsClick: (Application) -> Unit) {
            jobTitleTextView.text = application.jobTitle
            companyNameTextView.text = application.companyName
            applicantNameTextView.text = application.applicantName
            try {
                dateTextView.text = formatDate(application.appliedDate)
            } catch (e: Exception) {
                Log.e("ApplicationViewHolder", "Error formatting date: ${e.message}")
                dateTextView.text = "Unknown date"
            }
            val status = application.status.trim()
            Log.d("ApplicationViewHolder", "Setting status chip text to: '$status'")
            statusChip.text = status.capitalize()
            val colorResId = getStatusColor(status)
            Log.d("ApplicationViewHolder", "Status color resource ID: $colorResId")
            val context = itemView.context
            val color = context.resources.getColor(colorResId, context.theme)
            statusChip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
            viewDetailsButton.setOnClickListener {
                Log.d("ApplicationViewHolder", "View details clicked for: ${application.jobTitle}")
                onViewDetailsClick(application)
            }
            itemView.setOnLongClickListener {
                Log.d("ApplicationViewHolder", "Long press detected for: ${application.jobTitle}")
                onViewDetailsClick(application)
                true
            }
        }
        private fun formatDate(timestamp: Timestamp?): String {
            if (timestamp == null) return "Unknown date"
            try {
                val date = timestamp.toDate()
                return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                Log.e("ApplicationViewHolder", "Error formatting date: ${e.message}")
                return "Unknown date"
            }
        }
        private fun getStatusColor(status: String): Int {
            val statusLower = status.lowercase().trim()
            Log.d("ApplicationViewHolder", "Getting color for status: '$statusLower'")
            return when {
                statusLower == "pending" -> R.color.status_pending
                statusLower == "shortlisted" || statusLower == "reviewed" -> R.color.status_reviewed
                statusLower == "rejected" -> R.color.status_rejected
                statusLower == "accepted" -> R.color.status_accepted
                statusLower.contains("accept") -> R.color.status_accepted
                statusLower.contains("reject") -> R.color.status_rejected
                statusLower.contains("pend") -> R.color.status_pending
                statusLower.contains("review") || statusLower.contains("short") -> R.color.status_reviewed
                else -> {
                    Log.d("ApplicationViewHolder", "No specific color for status: '$statusLower', using default")
                    R.color.status_pending
                }
            }
        }
        private fun String.capitalize(): String {
            return this.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_application, parent, false)
        return ApplicationViewHolder(view)
    }
    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        try {
            val application = applications[position]
            Log.d(TAG, "Binding application at position $position: ${application.jobTitle}")
            holder.bind(application, onViewDetailsClick)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding application at position $position: ${e.message}")
        }
    }
    override fun getItemCount(): Int {
        val count = applications.size
        Log.d(TAG, "getItemCount() returning $count")
        return count
    }
}