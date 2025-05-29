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

class UpcomingEventAdapter(
    private val onEventClick: (CalendarEvent) -> Unit,
    private val onEventEdit: (CalendarEvent) -> Unit,
    private val onEventDelete: (CalendarEvent) -> Unit,
    private val allowEditDelete: Boolean = true // Allow editing/deleting by default
) : ListAdapter<CalendarEvent, UpcomingEventAdapter.EventViewHolder>(EventDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.eventCard)
        private val titleText: TextView = itemView.findViewById(R.id.eventTitle)
        private val dateText: TextView = itemView.findViewById(R.id.eventDate)
        private val timeText: TextView = itemView.findViewById(R.id.eventTime)
        private val typeIcon: ImageView = itemView.findViewById(R.id.eventTypeIcon)
        private val typeText: TextView = itemView.findViewById(R.id.eventType)
        private val editButton: ImageView = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(event: CalendarEvent) {
            titleText.text = event.title
            dateText.text = getRelativeDateText(event.date.toDate())
            timeText.text = event.time

            // Set meeting type icon and text
            when (event.meetingType) {
                "online" -> {
                    typeIcon.setImageResource(R.drawable.ic_chatbot)
                    typeText.text = "Online"
                }
                "in-person" -> {
                    typeIcon.setImageResource(R.drawable.ic_location)
                    typeText.text = "In-Person"
                }
                else -> {
                    typeIcon.setImageResource(R.drawable.ic_schedule)
                    typeText.text = "Event"
                }
            }

            // Set card color based on event status, type and urgency
            val cardColor = when {
                event.status == "pending" -> R.color.status_pending
                event.status == "cancelled" -> android.R.color.darker_gray
                event.isInterview -> R.color.status_interviewing
                isToday(event.date.toDate()) -> R.color.status_pending
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

        private fun isToday(date: Date): Boolean {
            val today = Calendar.getInstance()
            val eventDate = Calendar.getInstance()
            eventDate.time = date

            return today.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
                   today.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
        }

        private fun getRelativeDateText(date: Date): String {
            val today = Calendar.getInstance()
            val eventDate = Calendar.getInstance()
            eventDate.time = date

            val daysDiff = eventDate.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR)
            val yearsDiff = eventDate.get(Calendar.YEAR) - today.get(Calendar.YEAR)

            return when {
                yearsDiff == 0 && daysDiff == 0 -> "Today"
                yearsDiff == 0 && daysDiff == 1 -> "Tomorrow"
                yearsDiff == 0 && daysDiff in 2..6 -> {
                    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
                    dayOfWeek
                }
                else -> dateFormat.format(date)
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
