package com.example.jobrec.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.jobrec.ChatActivity
import com.example.jobrec.JobDetailsActivity
import com.example.jobrec.StudentApplicationDetailsActivity
import com.google.firebase.firestore.FirebaseFirestore

class NotificationReceiver : BroadcastReceiver() {
    private val TAG = "NotificationReceiver"
    private val db = FirebaseFirestore.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification clicked: ${intent.action}")
        
        val type = intent.getStringExtra("notification_type")
        val id = intent.getStringExtra("notification_id")
        
        if (type != null && id != null) {
            handleNotificationClick(context, type, id)
        }
    }
    
    private fun handleNotificationClick(context: Context, type: String, id: String) {
        Log.d(TAG, "Handling notification click: type=$type, id=$id")
        
        when (type) {
            "job" -> openJobDetails(context, id)
            "application" -> openApplicationDetails(context, id)
            "chat" -> openChatConversation(context, id)
            else -> Log.w(TAG, "Unknown notification type: $type")
        }
    }
    
    private fun openJobDetails(context: Context, jobId: String) {
        val intent = Intent(context, JobDetailsActivity::class.java).apply {
            putExtra("jobId", jobId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    private fun openApplicationDetails(context: Context, applicationId: String) {
        val intent = Intent(context, StudentApplicationDetailsActivity::class.java).apply {
            putExtra("applicationId", applicationId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    private fun openChatConversation(context: Context, conversationId: String) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("conversationId", conversationId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
