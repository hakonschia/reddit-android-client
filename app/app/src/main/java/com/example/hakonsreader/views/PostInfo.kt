package com.example.hakonsreader.views

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.PostInfoBinding
import com.example.hakonsreader.views.util.ViewUtil
import com.google.gson.Gson

/**
 * View for info about posts (title, author, subreddit etc)
 */
class PostInfo : FrameLayout {

    private val binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Sets the post to use in this VoteBar and sets the initial state of the vote status
     *
     * @param post The post to set
     * @param updateAwards True to update the awards in the awards layout, default is false
     */
    fun setPost(post: RedditPost, updateAwards: Boolean = false) {
        binding.post = post
        binding.isCrosspost = post.crosspostParentId != null
        if (updateAwards) {
            binding.awards.listing = post
        }
        binding.userReportsTitle.setOnClickListener { ViewUtil.openReportsBottomSheet(post, context) { binding.invalidateAll() } }

        val crossposts = post.crossposts
        if (crossposts != null && crossposts.isNotEmpty()) {
            val crosspost = crossposts[0]
            binding.crosspost = crosspost
            binding.crosspostText.setOnClickListener { openPost(crosspost) }
        }
    }

    /**
     * Enables or disables layout animation
     *
     * @param enable If set to true, layout animations will be enabled
     */
    fun enableLayoutAnimations(enable: Boolean) {
        binding.parentLayout.layoutTransition = if (enable) {
            LayoutTransition()
        } else {
            null
        }
        binding.executePendingBindings()
    }

    /**
     * Opens a post in a [PostActivity]
     *
     * @param post The post to open
     */
    private fun openPost(post: RedditPost) {
        val intent = Intent(context, PostActivity::class.java).apply {
            putExtra(PostActivity.POST_KEY, Gson().toJson(post))
        }
        val activity = context as Activity
        activity.startActivity(intent)
    }
}