package com.example.hakonsreader.dialogadapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.views.Tag
import com.example.hakonsreader.views.util.ViewUtil

/**
 * Adapter for a spinner for the submission flairs
 *
 * This class will add "Select flair" as the first item. When retrieving the selected item
 * for the spinner, you should use "selectedItem - 1" for the correct item
 */
class RedditFlairAdapter(
        context: Context,
        resourceId: Int,
        val flairs: ArrayList<RedditFlair>
) : ArrayAdapter<RedditFlair>(context, resourceId, flairs) {
    companion object {
        private const val TAG = "SubmissionFlairAdapter"
    }

    /**
     * Gets the flair by a position
     *
     * @param position The position to get the flair from
     * @return The RedditFlair at [position], or `null` if the position is 0 as this is the "Select flair"
     * item
     */
    fun getFlairAt(position: Int): RedditFlair? {
        return if (position != 0) {
            flairs[position - 1]
        } else {
            null
        }
    }

    override fun getCount(): Int {
        // The first item is a "Select flair" item
        return flairs.size + 1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return if (position == 0) {
            getViewForFirstPosition()
        } else {
            getCustomView(position - 1, parent)
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return if (position == 0) {
            getViewForFirstPosition()
        } else {
            getCustomView(position - 1, parent)
        }
    }

    private fun getViewForFirstPosition() : View {
        val padding = context.resources.getDimension(R.dimen.defaultMargin).toInt()
        return TextView(context).apply {
            text = context.getString(R.string.submitFlairSpinner)
            setPadding(padding, padding, padding, padding)
            setTextColor(ContextCompat.getColor(context, R.color.text_color))
        }
    }

    private fun getCustomView(position: Int, viewGroup: ViewGroup) : View {
        val submissionFlair = flairs[position]
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_submission_flair_view, viewGroup, false)

        val parentLayout = view.findViewById<LinearLayout>(R.id.parentLayout)
        val tag = Tag(context)
        ViewUtil.setFlair(
                tag,
                submissionFlair.richtextFlairs,
                submissionFlair.text,
                submissionFlair.textColor,
                submissionFlair.backgroundColor,
        )
        parentLayout.addView(tag)

        return view
    }
}