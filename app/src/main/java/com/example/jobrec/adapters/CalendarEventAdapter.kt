package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.CalendarEvent
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class CalendarEventAdapter(
    private val onEventClick: (CalendarEvent) -> Unit,
    private val onEventEdit: (CalendarEvent) -> Unit,
    private val onEventDelete: (CalendarEvent) -> Unit,
    private val allowEditDelete: Boolean = true // Allow editing/deleting by default
) : ListAdapter<CalendarEvent, CalendarEventAdapter.EventViewHolder>(EventDiffCallback()) {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.eventCard)
        private val titleText: TextView = itemView.findViewById(R.id.eventTitle)
        private val timeText: TextView = itemView.findViewById(R.id.eventTime)
        private val durationText: TextView = itemView.findViewById(R.id.eventDuration)
        private val typeIcon: ImageView = itemView.findViewById(R.id.eventTypeIcon)
        private val typeText: TextView = itemView.findViewById(R.id.eventType)
        private val locationText: TextView = itemView.findViewById(R.id.eventLocation)
        private val editButton: ImageView = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(event: CalendarEvent) {
            titleText.text = event.title
            timeText.text = event.time

            // Format duration
            val hours = event.duration / 60
            val minutes = event.duration % 60
            durationText.text = when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }

            // Set meeting type icon and text
            when (event.meetingType) {
                "online" -> {
                    typeIcon.setImageResource(R.drawable.ic_chatbot) // Using chatbot icon as placeholder for online
                    typeText.text = "Online"
                    locationText.text = event.meetingLink ?: "No link provided"
                    locationText.visibility = if (event.meetingLink.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
                "in-person" -> {
                    typeIcon.setImageResource(R.drawable.ic_location)
                    typeText.text = "In-Person"
                    locationText.text = event.location ?: "No location provided"
                    locationText.visibility = if (event.location.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
                else -> {
                    typeIcon.setImageResource(R.drawable.ic_schedule)
                    typeText.text = "Event"
                    locationText.visibility = View.GONE
                }
            }

            // Set card color based on event status and type
            val cardColor = when {
                event.status == "pending" -> R.color.status_pending
                event.status == "cancelled" -> android.R.color.darker_gray
                event.isInterview -> R.color.status_interviewing
                event.meetingType == "online" -> R.color.status_pending
                else -> R.color.status_reviewed
            }
            cardView.setCardBackgroundColor(itemView.context.getColor(cardColor))

            // Add visual indicator for pending events
            if (event.status == "pending") {
                cardView.alpha = 0.7f
            } else {
                cardView.alpha = 1.0f
            }

            // Show/hide edit and delete buttons based on event type and permissions
            val canEditDelete = allowEditDelete && !event.isInterview
            editButton.visibility = if (canEditDelete) View.VISIBLE else View.GONE
            deleteButton.visibility = if (canEditDelete) View.VISIBLE else View.GONE

            // Set click listeners
            cardView.setOnClickListener { onEventClick(event) }
            if (canEditDelete) {
                editButton.setOnClickListener { onEventEdit(event) }
                deleteButton.setOnClickListener { onEventDelete(event) }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
}
