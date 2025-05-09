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
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
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
                participantNameText.text = conversation.candidateName
                // Use a person icon for candidates
                profileImageView.setImageResource(R.drawable.ic_person)
            } else {
                // Candidate viewing company
                participantNameText.text = conversation.companyName
                // Use a building icon for companies
                profileImageView.setImageResource(R.drawable.ic_company_placeholder)
            }

            // Set job title
            jobTitleText.text = conversation.jobTitle

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
            calendar.time = date
            val messageCalendar = calendar.clone() as Calendar

            return when {
                // Today
                messageCalendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                messageCalendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) -> {
                    timeFormat.format(date)
                }
                // This week
                date.after(Date(today.time - 6 * 24 * 60 * 60 * 1000)) -> {
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    dayFormat.format(date)
                }
                // Older
                else -> {
                    dateFormat.format(date)
                }
            }
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
