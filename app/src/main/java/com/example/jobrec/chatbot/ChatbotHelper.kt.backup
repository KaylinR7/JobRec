package com.example.jobrec.chatbot

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.jobrec.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Helper class to add chatbot functionality to any activity
 */
class ChatbotHelper {
    companion object {
        /**
         * Add a chatbot FAB to an activity
         * @param activity The activity to add the FAB to
         * @return The FAB that was added
         */
        fun addChatbotButton(activity: Activity): FloatingActionButton? {
            // Find the root view
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            
            // Only add to CoordinatorLayout for proper behavior
            if (rootView is CoordinatorLayout) {
                // Inflate the FAB
                val fab = activity.layoutInflater.inflate(
                    R.layout.layout_chatbot_fab,
                    rootView,
                    false
                ) as FloatingActionButton
                
                // Add the FAB to the layout
                rootView.addView(fab)
                
                // Set click listener
                fab.setOnClickListener {
                    val intent = Intent(activity, ChatbotActivity::class.java)
                    activity.startActivity(intent)
                }
                
                return fab
            }
            
            return null
        }
    }
}
