package com.example.hakonsreader.markwonplugins

import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.URLSpan
import android.widget.TextView
import com.example.hakonsreader.App
import com.example.hakonsreader.misc.Settings
import io.noties.markwon.AbstractMarkwonPlugin

/**
 * Markwon plugin that goes over the entire text and applies a [RelativeSizeSpan] to the text where a
 * [URLSpan] is found.
 *
 * The scale applied is the scale returned by [App.linkScale]
 */
class EnlargeLinkPlugin : AbstractMarkwonPlugin() {
    override fun afterSetText(textView: TextView) {
        super.afterSetText(textView)

        val linkScale = Settings.linkScale()

        // Default scale, don't do anything
        if (linkScale == 100) {
            return
        }

        val scale = linkScale / 100f

        val string = textView.text as SpannableString
        val urlSpans = string.getSpans(0, string.length, URLSpan::class.java)

        for (urlSpan in urlSpans) {
            val start = string.getSpanStart(urlSpan)
            val end = string.getSpanEnd(urlSpan)
            string.setSpan(RelativeSizeSpan(scale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}