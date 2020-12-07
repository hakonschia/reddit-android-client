package com.example.hakonsreader.dialogadapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.flairs.SubmissionFlair
import com.example.hakonsreader.views.util.ViewUtil

/**
 * Adapter for a spinner for the submission flairs
 *
 * This class will add "Select flair" as the first item. When retrieving the selected item
 * for the spinner, you should use "selectedItem - 1" for the correct item
 */
class SubmissionFlairAdapter(
        context: Context,
        resourceId: Int,
        val flairs: ArrayList<SubmissionFlair>
) : ArrayAdapter<SubmissionFlair>(context, resourceId, flairs) {
    companion object {
        private const val TAG = "SubmissionFlairAdapter"
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
        val text = TextView(context)
        text.text = context.getString(R.string.submitFlairSpinner)

        val padding = context.resources.getDimension(R.dimen.defaultMargin).toInt()
        text.setPadding(padding, padding, padding, padding)
        text.setTextColor(ContextCompat.getColor(context, R.color.text_color))

        return text
    }

    private fun getCustomView(position: Int, viewGroup: ViewGroup) : View {
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_submission_flair_view, viewGroup, false)
        val submissionFlair = flairs[position]

        val parentLayout = view.findViewById<LinearLayout>(R.id.parentLayout)
        val tag = ViewUtil.createFlair(
                submissionFlair.richtextFlairs,
                submissionFlair.text,
                submissionFlair.textColor,
                submissionFlair.backgroundColor,
                context
        )
        parentLayout.addView(tag)

        return view
    }

}