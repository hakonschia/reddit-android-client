package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.example.hakonsreader.App
import com.example.hakonsreader.databinding.ContentTextBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod

/**
 * View for text posts. This only shows the text of the post (with Markwon), but includes an [android.widget.ScrollView]
 * instead of [android.widget.TextView] so that the scrolling of the text has acceleration/drag that continues
 * scrolling after the user has stopped scrolling (as this is expected behaviour when scrolling)
 */
class ContentText : Content {

    val binding: ContentTextBinding

    constructor(ctx: Context) : super(ctx) {
        binding = ContentTextBinding.inflate(LayoutInflater.from(ctx), this, true)
    }
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        binding = ContentTextBinding.inflate(LayoutInflater.from(ctx), this, true)
    }
    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr) {
        binding = ContentTextBinding.inflate(LayoutInflater.from(ctx), this, true)
    }

    override fun updateView() {
        var markdown: String = redditPost.selftext

        if (markdown.isNotEmpty()) {
            binding.content.movementMethod = InternalLinkMovementMethod.getInstance(context)

            markdown = App.get().adjuster.adjust(markdown)
            App.get().markwon.setMarkdown(binding.content, markdown)
        }
    }

}