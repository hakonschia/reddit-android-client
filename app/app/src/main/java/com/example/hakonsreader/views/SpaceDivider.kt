package com.example.hakonsreader.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView decorator that adds space between items by setting the bottom margin for every item
 * except the last
 */
class SpaceDivider(val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Add bottom padding as long as it is not the last item
        outRect.bottom = if (parent.adapter?.itemCount != parent.getChildAdapterPosition(view) + 1) {
            space
        } else {
            0
        }
    }
}