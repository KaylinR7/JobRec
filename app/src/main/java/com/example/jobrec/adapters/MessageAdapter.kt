package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(
    private val onMeetingAccept: (Message) -> Unit,
    private val onMeetingDecline: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(message)
        } else {
            (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTimeText: TextView = itemView.findViewById(R.id.messageTimeText)
        private val meetingDetailsLayout: LinearLayout = itemView.findViewById(R.id.meetingDetailsLayout)
        private val meetingDateText: TextView = itemView.findViewById(R.id.meetingDateText)
        private val meetingTimeText: TextView = itemView.findViewById(R.id.meetingTimeText)
        private val meetingTypeText: TextView = itemView.findViewById(R.id.meetingTypeText)
        private val meetingLocationText: TextView = itemView.findViewById(R.id.meetingLocationText)
        private val meetingLinkText: TextView = itemView.findViewById(R.id.meetingLinkText)
        private val meetingStatusText: TextView = itemView.findViewById(R.id.meetingStatusText)

        fun bind(message: Message) {
            messageText.text = message.content
            messageTimeText.text = timeFormat.format(message.createdAt.toDate())

            if (message.type == "interview" && message.interviewDetails != null) {
                meetingDetailsLayout.visibility = View.VISIBLE
                
                val details = message.interviewDetails
                meetingDateText.text = "Date: ${dateFormat.format(details.date.toDate())}"
                meetingTimeText.text = "Time: ${details.time}"
                meetingTypeText.text = "Type: ${details.type.capitalize()}"
                
                if (!details.location.isNullOrEmpty()) {
                    meetingLocationText.visibility = View.VISIBLE
                    meetingLocationText.text = "Location: ${details.location}"
                } else {
                    meetingLocationText.visibility = View.GONE
                }
                
                if (!details.meetingLink.isNullOrEmpty()) {
                    meetingLinkText.visibility = View.VISIBLE
                    meetingLinkText.text = "Link: ${details.meetingLink}"
                } else {
                    meetingLinkText.visibility = View.GONE
                }
                
                val statusText = when (details.status) {
                    "pending" -> "Pending response"
                    "accepted" -> "Meeting accepted"
                    "rejected" -> "Meeting declined"
                    "completed" -> "Meeting completed"
                    else -> "Pending response"
                }
                meetingStatusText.text = statusText
            } else {
                meetingDetailsLayout.visibility = View.GONE
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTimeText: TextView = itemView.findViewById(R.id.messageTimeText)
        private val meetingDetailsLayout: LinearLayout = itemView.findViewById(R.id.meetingDetailsLayout)
        private val meetingDateText: TextView = itemView.findViewById(R.id.meetingDateText)
        private val meetingTimeText: TextView = itemView.findViewById(R.id.meetingTimeText)
        private val meetingTypeText: TextView = itemView.findViewById(R.id.meetingTypeText)
        private val meetingLocationText: TextView = itemView.findViewById(R.id.meetingLocationText)
        private val meetingLinkText: TextView = itemView.findViewById(R.id.meetingLinkText)
        private val meetingResponseLayout: LinearLayout = itemView.findViewById(R.id.meetingResponseLayout)
        private val acceptMeetingButton: Button = itemView.findViewById(R.id.acceptMeetingButton)
        private val declineMeetingButton: Button = itemView.findViewById(R.id.declineMeetingButton)
        private val meetingStatusText: TextView = itemView.findViewById(R.id.meetingStatusText)

        fun bind(message: Message) {
            messageText.text = message.content
            messageTimeText.text = timeFormat.format(message.createdAt.toDate())

            if (message.type == "interview" && message.interviewDetails != null) {
                meetingDetailsLayout.visibility = View.VISIBLE
                
                val details = message.interviewDetails
                meetingDateText.text = "Date: ${dateFormat.format(details.date.toDate())}"
                meetingTimeText.text = "Time: ${details.time}"
                meetingTypeText.text = "Type: ${details.type.capitalize()}"
                
                if (!details.location.isNullOrEmpty()) {
                    meetingLocationText.visibility = View.VISIBLE
                    meetingLocationText.text = "Location: ${details.location}"
                } else {
                    meetingLocationText.visibility = View.GONE
                }
                
                if (!details.meetingLink.isNullOrEmpty()) {
                    meetingLinkText.visibility = View.VISIBLE
                    meetingLinkText.text = "Link: ${details.meetingLink}"
                } else {
                    meetingLinkText.visibility = View.GONE
                }
                
                // Show response buttons only if the meeting is pending
                if (details.status == "pending") {
                    meetingResponseLayout.visibility = View.VISIBLE
                    meetingStatusText.visibility = View.GONE
                    
                    acceptMeetingButton.setOnClickListener {
                        onMeetingAccept(message)
                    }
                    
                    declineMeetingButton.setOnClickListener {
                        onMeetingDecline(message)
                    }
                } else {
                    meetingResponseLayout.visibility = View.GONE
                    meetingStatusText.visibility = View.VISIBLE
                    
                    val statusText = when (details.status) {
                        "accepted" -> "Meeting accepted"
                        "rejected" -> "Meeting declined"
                        "completed" -> "Meeting completed"
                        else -> "Pending response"
                    }
                    meetingStatusText.text = statusText
                }
            } else {
                meetingDetailsLayout.visibility = View.GONE
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
