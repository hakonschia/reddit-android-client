package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.ContentPostRemovedBinding

/**
 * View for posts that have been removed
 */
class ContentPostRemoved : Content {

    val binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun updateView() {
        binding.post = redditPost
    }
}

/**
 * Sets the text saying which category (ie. moderator, admin etc.) removed the post
 *
 * @param tv The TextView to set the text on
 * @param post The post that has been removed
 */
@BindingAdapter("removedBy")
fun removedBy(tv: TextView, post: RedditPost) {
    // We shouldn't get here with this being null, but in case it is return as the else branch
    // wouldn't make sense (it's not removed)
    val removedByCategory: String = post.removedByCategory ?: return

    tv.text = when (removedByCategory) {
        "moderator" -> tv.resources.getString(R.string.postRemovedByMods, post.subreddit)
        "automod_filtered" -> tv.resources.getString(R.string.postRemovedByAutoMod)
        "author" -> tv.resources.getString(R.string.postRemovedByAuthor)
        else -> tv.resources.getString(R.string.postRemovedGeneric)
    }
}