package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.example.hakonsreader.App
import com.example.hakonsreader.databinding.ContentTextBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.views.util.setLongClickToPeekUrl

/**
 * View for text posts. This only shows the text of the post (with Markwon), but includes an [android.widget.ScrollView]
 * instead of [android.widget.TextView] so that the scrolling of the text has acceleration/drag that continues
 * scrolling after the user has stopped scrolling (as this is expected behaviour when scrolling)
 */
class ContentText : Content {

    private val binding = ContentTextBinding.inflate(LayoutInflater.from(context), this, true).apply {
        val movementMethod = InternalLinkMovementMethod()
        content.movementMethod = movementMethod
        content.setLongClickToPeekUrl()
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