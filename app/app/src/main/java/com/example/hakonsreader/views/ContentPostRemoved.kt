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

    val binding: ContentPostRemovedBinding

    constructor(context: Context?) : super(context) {
        binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true)
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        binding = ContentPostRemovedBinding.inflate(LayoutInflater.from(context), this, true)
    }


    override fun updateView() {
        binding.post = redditPost
    }

    /**
     * Sets the text saying which category (ie. moderator, admin etc.) removed the post
     *
     * @param tv The TextView to set the text on
     * @param post The post that has been removed
     */
    @BindingAdapter("removedBy")
    fun removedBy(tv: TextView, post: RedditPost) {
        val removedByCategory: String = post.removedByCategory

        tv.text = when (removedByCategory) {
            "moderator" -> resources.getString(R.string.postRemovedByMods, post.subreddit)
            "automod_filtered" -> resources.getString(R.string.postRemovedByAutoMod)
            "author" -> resources.getString(R.string.postRemovedByAuthor)
            else -> resources.getString(R.string.postRemovedGeneric)
        }
    }
}