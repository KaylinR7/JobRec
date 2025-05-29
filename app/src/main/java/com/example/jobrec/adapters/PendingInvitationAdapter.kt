package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.Message
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class PendingInvitationAdapter(
    private var invitations: List<Message>,
    private val onAccept: (Message) -> Unit,
    private val onDecline: (Message) -> Unit
) : RecyclerView.Adapter<PendingInvitationAdapter.InvitationViewHolder>() {

    class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val companyText: TextView = itemView.findViewById(R.id.companyText)
        val dateTimeText: TextView = itemView.findViewById(R.id.dateTimeText)
        val durationText: TextView = itemView.findViewById(R.id.durationText)
        val meetingTypeIcon: ImageView = itemView.findViewById(R.id.meetingTypeIcon)
        val meetingTypeText: TextView = itemView.findViewById(R.id.meetingTypeText)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_invitation, parent, false)
        return InvitationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        val invitation = invitations[position]
        val details = invitation.interviewDetails

        if (details != null) {
            // Set title
            holder.titleText.text = "Interview Invitation"
            
            // Set company name (from sender)
            holder.companyText.text = "From: ${invitation.senderName}"
            
            // Format date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(details.date.toDate())
            holder.dateTimeText.text = "$formattedDate at ${details.time}"
            
            // Set duration
            val hours = details.duration / 60
            val minutes = details.duration % 60
            val durationText = when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
            holder.durationText.text = "Duration: $durationText"
            
            // Set meeting type
            when (details.type) {
                "online" -> {
                    holder.meetingTypeIcon.setImageResource(R.drawable.ic_video_call)
                    holder.meetingTypeText.text = "Online Meeting"
                    holder.locationText.text = details.meetingLink ?: "Meeting link will be provided"
                }
                "in-person" -> {
                    holder.meetingTypeIcon.setImageResource(R.drawable.ic_location)
                    holder.meetingTypeText.text = "In-Person Meeting"
                    holder.locationText.text = details.location ?: "Location to be confirmed"
                }
                else -> {
                    holder.meetingTypeIcon.setImageResource(R.drawable.ic_calendar)
                    holder.meetingTypeText.text = "Meeting"
                    holder.locationText.text = "Details to be confirmed"
                }
            }
            
            // Set card background color to indicate pending status
            holder.cardView.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.status_pending)
            )
            holder.cardView.alpha = 0.9f
            
            // Set button click listeners
            holder.acceptButton.setOnClickListener {
                onAccept(invitation)
            }
            
            holder.declineButton.setOnClickListener {
                onDecline(invitation)
            }
        }
    }

    override fun getItemCount(): Int = invitations.size

    fun updateInvitations(newInvitations: List<Message>) {
        invitations = newInvitations
        notifyDataSetChanged()
    }
}
