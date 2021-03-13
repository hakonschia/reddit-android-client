package com.example.hakonsreader.views

import android.content.Context
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import com.example.hakonsreader.App
import com.example.hakonsreader.databinding.ContentTextBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.showPeekUrlBottomSheet

/**
 * View for text posts. This only shows the text of the post (with Markwon), but includes an [android.widget.ScrollView]
 * instead of [android.widget.TextView] so that the scrolling of the text has acceleration/drag that continues
 * scrolling after the user has stopped scrolling (as this is expected behaviour when scrolling)
 */
class ContentText : Content {

    private val binding = ContentTextBinding.inflate(LayoutInflater.from(context), this, true).apply {
        val movementMethod = InternalLinkMovementMethod()
        content.movementMethod = movementMethod
        content.setOnLongClickListener {
            it as TextView

            val start = it.selectionStart
            val end = it.selectionEnd

            if (start != -1 && end != -1) {
                movementMethod.ignoreNextClick()

                val spans = it.text.toSpannable().getSpans(start, end, URLSpan::class.java)
                if (spans.isNotEmpty()) {
                    val span = spans.first()

                    val text = it.text.subSequence(start, end).toString()
                    val url = span.url

                    showPeekUrlBottomSheet(it.context as AppCompatActivity, text, url)
                }
            }

            true
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun updateView() {
        var markdown: String = redditPost.selftext

        if (markdown.isNotEmpty()) {
            markdown = App.get().adjuster.adjust(markdown)
            App.get().markwon.setMarkdown(binding.content, markdown)
        }
    }
}