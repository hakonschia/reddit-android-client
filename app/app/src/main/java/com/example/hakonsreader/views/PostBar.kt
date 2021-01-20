package com.example.hakonsreader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.databinding.PostBarBinding
import com.robinhood.ticker.TickerUtils

class PostBar : FrameLayout {

    private val binding = PostBarBinding.inflate(LayoutInflater.from(context), this, true)

    var post: RedditPost? = null
        set(value) {

            field = value
            binding.voteBar.listing = value
            binding.post = value

            updateView()
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

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
        binding.voteBar.updateVoteStatus()

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

    /**
     * Check if the TickerViews have animation enabled
     *
     * @return True if animation is enabled
     */
    fun tickerAnimationEnabled(): Boolean {
        // Technically voteBar and numComments can have different values, but assume they're always synced
        return binding.voteBar.tickerAnimationEnabled()
    }
}