package com.example.jobrec.chatbot
import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.jobrec.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
class ChatbotHelper {
    companion object {
        fun addChatbotButton(activity: Activity): FloatingActionButton? {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            if (rootView is CoordinatorLayout) {
                val fab = activity.layoutInflater.inflate(
                    R.layout.layout_chatbot_fab,
                    rootView,
                    false
                ) as FloatingActionButton
                rootView.addView(fab)
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
