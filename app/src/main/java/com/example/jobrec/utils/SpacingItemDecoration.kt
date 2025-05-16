package com.example.jobrec.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        
        // Add spacing to all items except the last one
        if (position != parent.adapter?.itemCount?.minus(1)) {
            if (parent.layoutManager?.canScrollHorizontally() == true) {
                // Horizontal list
                outRect.right = spacing
            } else {
                // Vertical list
                outRect.bottom = spacing
            }
        }
    }
}
