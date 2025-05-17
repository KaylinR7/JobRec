package com.example.jobrec.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.models.Conversation
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)
        private val participantNameText: TextView = itemView.findViewById(R.id.participantNameText)
        private val jobTitleText: TextView = itemView.findViewById(R.id.jobTitleText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadCountText: TextView = itemView.findViewById(R.id.unreadCountText)

        fun bind(conversation: Conversation) {
            // Set participant name based on current user
            val isCompanyView = currentUserId == conversation.companyId

            if (isCompanyView) {
                // Company viewing candidate
                // Make sure we display the student name (not empty or default)
                val displayName = when {
                    conversation.candidateName.isBlank() -> "Student"
                    else -> conversation.candidateName
                }
                participantNameText.text = displayName
                // Use a person icon for candidates
                profileImageView.setImageResource(R.drawable.ic_person)
            } else {
                // Student viewing company
                // Make sure we display the company name (not empty or default)
                val displayName = when {
                    conversation.companyName.isBlank() || conversation.companyName == "unknown" -> {
                        // If company name is not available, show the job title as the company name
                        if (conversation.jobTitle.isNotBlank()) {
                            // Extract company name from job title if possible
                            val parts = conversation.jobTitle.split(" at ", " - ", " @ ", limit = 2)
                            if (parts.size > 1) {
                                parts[1].trim() // Use the part after "at" or "-" as company name
                            } else {
                                conversation.jobTitle
                            }
                        } else {
                            "Company"
                        }
                    }
                    else -> conversation.companyName
                }
                participantNameText.text = displayName
                // Use a building icon for companies
                profileImageView.setImageResource(R.drawable.ic_company_placeholder)
            }

            // Set job title based on view
            if (isCompanyView) {
                // Company viewing candidate - show job title
                jobTitleText.text = conversation.jobTitle
            } else {
                // Student viewing company - show the job title
                // If we're using the job title as the company name, show "Job Position" instead
                if (participantNameText.text.toString() == conversation.jobTitle) {
                    jobTitleText.text = "Job Position"
                } else {
                    jobTitleText.text = conversation.jobTitle
                }
            }

            // Set last message with preview
            val lastMsg = conversation.lastMessage
            if (lastMsg.startsWith("Meeting invitation")) {
                lastMessageText.text = "📅 Meeting invitation"
            } else {
                lastMessageText.text = lastMsg
            }

            // Set time
            val messageDate = conversation.lastMessageTime.toDate()
            timeText.text = formatMessageTime(messageDate)

            // Set unread count
            if (conversation.unreadCount > 0 &&
                ((currentUserId == conversation.candidateId && conversation.lastMessageSender == conversation.companyId) ||
                 (currentUserId == conversation.companyId && conversation.lastMessageSender == conversation.candidateId))) {
                unreadCountText.visibility = View.VISIBLE
                unreadCountText.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
            } else {
                unreadCountText.visibility = View.GONE
            }

            // Set click listener
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun formatMessageTime(date: Date): String {
            // Always use the date format (MMM dd, yyyy) for consistency
            return dateFormat.format(date)
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
