package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.repositories.NotificationRepository
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(
    private val onNotificationClick: (NotificationRepository.Notification) -> Unit,
    private val onApplyClick: (NotificationRepository.Notification) -> Unit,
    private val onDeleteClick: (NotificationRepository.Notification) -> Unit = {}
) : ListAdapter<NotificationRepository.Notification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

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
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        fun bind(notification: NotificationRepository.Notification) {

            titleTextView.text = notification.title
            messageTextView.text = notification.message

            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            timeTextView.text = dateFormat.format(notification.timestamp.toDate())

            // Set card background based on read status
            cardView.alpha = if (notification.read) 0.7f else 1.0f

            // Show apply button only for job notifications
            if (notification.type == NotificationRepository.TYPE_NEW_JOB && notification.jobId != null) {
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

            // Set click listener for delete button
            deleteButton.setOnClickListener {
                onDeleteClick(notification)
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationRepository.Notification>() {
    override fun areItemsTheSame(oldItem: NotificationRepository.Notification, newItem: NotificationRepository.Notification): Boolean {
        // Compare by ID to identify the same notification
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificationRepository.Notification, newItem: NotificationRepository.Notification): Boolean {
        // Compare all fields to determine if content has changed
        return oldItem.id == newItem.id &&
               oldItem.title == newItem.title &&
               oldItem.message == newItem.message &&
               oldItem.type == newItem.type &&
               oldItem.jobId == newItem.jobId &&
               oldItem.read == newItem.read
    }
}
