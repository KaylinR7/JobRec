package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(
    private val onNotificationClick: (NotificationsActivity.Notification) -> Unit,
    private val onApplyClick: (NotificationsActivity.Notification) -> Unit
) : ListAdapter<NotificationsActivity.Notification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.notificationCard)
        private val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        private val messageTextView: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timeTextView: TextView = itemView.findViewById(R.id.notificationTime)
        private val applyButton: Button = itemView.findViewById(R.id.applyButton)

        fun bind(notification: NotificationsActivity.Notification) {
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            timeTextView.text = dateFormat.format(notification.timestamp.toDate())
            
            // Set card background based on read status
            cardView.alpha = if (notification.read) 0.7f else 1.0f
            
            // Show apply button only for job notifications
            if (notification.type == "new_job" && notification.jobId != null) {
                applyButton.visibility = View.VISIBLE
                applyButton.setOnClickListener {
                    onApplyClick(notification)
                }
            } else {
                applyButton.visibility = View.GONE
            }
            
            // Set click listener for the whole card
            cardView.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationsActivity.Notification>() {
    override fun areItemsTheSame(oldItem: NotificationsActivity.Notification, newItem: NotificationsActivity.Notification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificationsActivity.Notification, newItem: NotificationsActivity.Notification): Boolean {
        return oldItem == newItem
    }
}
