package com.example.jobrec.adapters
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.RecentActivity
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class RecentActivityAdapter(
    private val onActivityClick: (RecentActivity) -> Unit
) : ListAdapter<RecentActivity, RecentActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }
    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = getItem(position)
        holder.bind(activity)
    }
    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.activityCard)
        private val titleTextView: TextView = itemView.findViewById(R.id.activityTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.activityDescription)
        private val timeTextView: TextView = itemView.findViewById(R.id.activityTime)
        fun bind(activity: RecentActivity) {
            titleTextView.text = activity.title
            descriptionTextView.text = activity.description
            val date = activity.timestamp.toDate()
            val now = Calendar.getInstance().time
            timeTextView.text = dateFormat.format(date)
            val cardColor = when (activity.type) {
                "application" -> {
                    when {
                        activity.title.contains("New Application") -> R.color.status_applied
                        activity.title.contains("Shortlisted") -> R.color.status_shortlisted
                        activity.title.contains("Reviewed") -> R.color.status_reviewed
                        activity.title.contains("Interview") -> R.color.status_interviewed
                        activity.title.contains("Offered") -> R.color.status_offered
                        activity.title.contains("Rejected") -> R.color.status_rejected
                        else -> R.color.status_pending
                    }
                }
                "job_post" -> R.color.primary_light
                "message" -> R.color.accent_light
                else -> R.color.white
            }
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(itemView.context, cardColor).let { color ->
                    ColorUtils.setAlphaComponent(color, 40)
                }
            )
            val titleColor = when (activity.type) {
                "application" -> R.color.primary
                "job_post" -> R.color.primary_dark
                "message" -> R.color.accent_dark
                else -> R.color.text_primary
            }
            titleTextView.setTextColor(ContextCompat.getColor(itemView.context, titleColor))
            cardView.setOnClickListener {
                cardView.postDelayed({
                    onActivityClick(activity)
                }, 100)
            }
        }
    }
    class ActivityDiffCallback : DiffUtil.ItemCallback<RecentActivity>() {
        override fun areItemsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean {
            return oldItem == newItem
        }
    }
}
