package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.databinding.PostBarBinding
import com.example.hakonsreader.recyclerviewadapters.menuhandlers.showPopupForPost
import com.robinhood.ticker.TickerUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wrapper for a full post bar. This view shows the number of comments on the post, as well as
 * a [VoteBar] and a button to open a popup menu for the post
 */
@AndroidEntryPoint
class PostBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var postsDao: RedditPostsDao

    private val binding = PostBarBinding.inflate(LayoutInflater.from(context), this, true).apply {
        postPopupMenu.setOnClickListener {
            showPopupForPost(it, post, postsDao, api)
        }
    }

    /**
     * The post to display. The view is automatically updated when this is set, assuming the passed
     * value isn't `null`
     */
    var post: RedditPost? = null
        set(value) {
            field = value
            if (value != null) {
                binding.voteBar.listing = value
                updateView()
            }
        }

    /**
     * Call this if the score should always be hidden. Must be called before [PostBar.setPost]
     */
    fun setHideScore(hideScore: Boolean) {
        binding.voteBar.hideScore = hideScore
    }

    /**
     * Gets if the score is set to always be hidden
     *
     * @return True if the score is always hidden
     */
    fun getHideScore(): Boolean {
        return binding.voteBar.hideScore
    }

    /**
     * Updates the view based on the post set with [PostBar.post]
     */
    private fun updateView() {
        binding.voteBar.updateVoteStatus(animate = true)

        val comments = post!!.amountOfComments.toFloat()

        binding.numComments.setCharacterLists(TickerUtils.provideNumberList())

        // Above 10k comments, show "1.5k comments" instead
        binding.numComments.text = if (comments > 1000) {
            String.format(resources.getString(R.string.numCommentsThousands), comments / 1000f)
        } else {
            resources.getQuantityString(
                    R.plurals.numComments,
                    post!!.amountOfComments,
                    post!!.amountOfComments
            )
        }
    }

    /**
     * Enables or disables the animation for any [com.robinhood.ticker.TickerView] found
     * in this view
     *
     * @param enable True to enable
     */
    fun enableTickerAnimation(enable: Boolean) {
        binding.voteBar.enableTickerAnimation(enable)
        binding.numComments.animationDuration = if (enable) resources.getInteger(R.integer.tickerAnimationDefault).toLong() else 0
    }

}