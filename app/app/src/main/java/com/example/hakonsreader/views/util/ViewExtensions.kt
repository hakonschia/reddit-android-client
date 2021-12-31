package com.example.hakonsreader.views.util

import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.showPeekUrlBottomSheet


/**
 * Sets a views visibility to [View.GONE] if a given predicate is true, otherwise it is set to [View.VISIBLE]
 *
 * @see invisibleIf
 */
fun View.goneIf(predicate: Boolean) {
    visibility = if (predicate) {
        View.GONE
    } else {
        View.VISIBLE
    }
}

/**
 * Sets a views visibility to [View.INVISIBLE] if a given predicate is true, otherwise it is set to [View.VISIBLE]
 *
 * @see goneIf
 */
fun View.invisibleIf(predicate: Boolean) {
    visibility = if (predicate) {
        View.INVISIBLE
    } else {
        View.VISIBLE
    }
}

/**
 * Sets a long click listener on a TextView that opens a bottom sheet to peek a URL long pressed, if
 * a selection of the TextView was long pressed.
 *
 * @param default If the TextView is pressed on a part of the view not a URL, then this will be invoked
 * instead, if set.
 */
fun TextView.setLongClickToPeekUrl(default: (() -> Unit)? = null) {
    setOnLongClickListener {
        val start = selectionStart
        val end = selectionEnd

        // Selection of the text has been made, ie
        if (start != -1 && end != -1) {
            val method = movementMethod
            if (method is InternalLinkMovementMethod) {
                method.ignoreNextClick()
            }

            val spans = text.toSpannable().getSpans(start, end, URLSpan::class.java)
            if (spans.isNotEmpty()) {
                val span = spans.first()

                val text = text.subSequence(start, end).toString()
                val url = if (span.url.isNotEmpty()) span.url else text

                if (context is AppCompatActivity) {
                    showPeekUrlBottomSheet(context as AppCompatActivity, text, url)
                }
            } else {
                default?.invoke()
            }
        } else {
            default?.invoke()
        }
        true
    }
}
