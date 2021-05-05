package com.example.hakonsreader.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.interfaces.VoteableListing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.VoteBarBinding
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.robinhood.ticker.TickerUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

/**
 * View for displaying a vote bar for a [VoteableListing]. This displays one button for upvoting the listing
 * and one button for downvoting the listing, as well as a text with the score of the listing
 */
@AndroidEntryPoint
class VoteBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = VoteBarBinding.inflate(LayoutInflater.from(context), this, true).apply {
        upvote.setOnClickListener { vote(VoteType.UPVOTE) }
        downvote.setOnClickListener { vote(VoteType.DOWNVOTE) }
        score.setCharacterLists(TickerUtils.provideAlphabeticalList(), TickerUtils.provideNumberList())
    }


    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var postsDao: RedditPostsDao

    /**
     * If set to true, the score of the listing will be hidden
     */
    var hideScore = false

    /**
     * The listing to show in the vote bar. Setting this automatically updates the view (unless
     * the passed value is `null`)
     */
    var listing: VoteableListing? = null
        set(value) {
            val oldId = field?.id
            field = value
            if (value != null) {
                // If the listing has been "updated" then we should animate the change
                updateVoteStatus(animate = oldId == value.id)

                // Deleted comments/posts will always give a 404 error
                // Kind of hardcoding values, but unless reddit change their API I don't know how else
                // to check if a comment/post is deleted before sending an API request
                if (value.author == "[deleted]") {
                    binding.upvote.isEnabled = false
                    binding.downvote.isEnabled = false
                } else {
                    binding.upvote.isEnabled = true
                    binding.downvote.isEnabled = true
                }
            }
        }


    /**
     * Sends a request to vote on the listing
     *
     * If the listing is archived a snackbar is shown and the API request is not made
     *
     * @param voteType The vote type to cast
     */
    private fun vote(voteType: VoteType) {
        // In a case that the post doesn't load, clicking on the vote buttons would cause a NPE
        listing?.let {
            if (it.isArchived) {
                showArchivedSnackbar()
                return
            }

            val currentVote = it.voteType
            val actualVote = if (currentVote == voteType) {
                // Ie. if upvote is clicked when the listing is already upvoted, unvote the listing
                VoteType.NO_VOTE
            } else {
                voteType
            }

            // Assume it's successful as it feels like the buttons aren't pressed when you have to wait
            // until the colors are updated
            it.voteType = actualVote
            updateVoteStatus(animate = true)

            val id = it.id

            CoroutineScope(IO).launch {
                val resp = if (it is RedditPost) {
                    postsDao.update(it)
                    api.post(id)
                } else {
                    api.comment(id)
                }.vote(actualVote)

                when (resp) {
                    is ApiResponse.Success -> {}
                    is ApiResponse.Error -> {
                        // Request failed, set back to default
                        it.voteType = currentVote
                        if (it is RedditPost) {
                            postsDao.update(it)
                        }

                        withContext(Main) {
                            updateVoteStatus(animate = true)
                            handleGenericResponseErrors(this@VoteBar, resp.error, resp.throwable)
                        }
                    }
                }
            }

            // Disable both buttons, and enable them again after a short time delay
            // This is to avoid spamming. It's still possible to get a 429 Too Many Requests, but it should
            // reduce the amount of times that would happen (and it removes potential missclicks right after a vote)
            binding.upvote.isEnabled = false
            binding.downvote.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({
                binding.upvote.isEnabled = true
                binding.downvote.isEnabled = true
            }, 350)
        }

    }

    /**
     * Updates the colors of the vote text/buttons and the vote count
     *
     * @param animate If set to false the ticker will not animate the change
     */
    fun updateVoteStatus(animate: Boolean) {
        if (listing == null) {
            return
        }

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        binding.upvote.setColorFilter(ContextCompat.getColor(context, R.color.noVote))
        binding.downvote.setColorFilter(ContextCompat.getColor(context, R.color.noVote))

        listing?.let {
            binding.score.textColor = when (it.voteType) {
                VoteType.UPVOTE -> {
                    ContextCompat.getColor(context, R.color.upvoted).also { color ->
                        binding.upvote.setColorFilter(color)
                    }
                }
                VoteType.DOWNVOTE -> {
                    ContextCompat.getColor(context, R.color.downvoted).also { color ->
                        binding.upvote.setColorFilter(color)
                    }
                }
                VoteType.NO_VOTE -> {
                    ContextCompat.getColor(context, R.color.text_color).also { color ->
                        binding.upvote.setColorFilter(color)
                    }
                }
            }

            // In tests setting the text (binding.score.text = "") fails since it is not on the UI thread for some reason
            Handler(Looper.getMainLooper()).post {
                if (it.isScoreHidden || hideScore) {
                    binding.score.setText(resources.getString(R.string.scoreHidden), animate)
                } else {
                    val scoreCount = it.score

                    // For scores over 10000 show as "10.5k"
                    binding.score.setText(if (scoreCount >= 10000) {
                        resources.getString(R.string.scoreThousands, scoreCount / 1000f)
                    } else {
                        scoreCount.toString()
                    }, animate)
                }
            }
        }
    }

    /**
     * Enables or disables the animation for any [com.robinhood.ticker.TickerView] found
     * in this view
     *
     * @param enable True to enable
     */
    fun enableTickerAnimation(enable: Boolean) {
        binding.score.animationDuration = if (enable) {
            resources.getInteger(R.integer.tickerAnimationFast).toLong()
        }
        else {
            0
        }
    }

    /**
     * Shows a snackbar saying that the listing has been archived
     */
    private fun showArchivedSnackbar() {
        val stringRes = if (listing is RedditPost) {
            context.getString(R.string.postHasBeenArchivedVote)
        } else {
            context.getString(R.string.commentHasBeenArchivedVote)
        }

        Snackbar.make(this, stringRes, BaseTransientBottomBar.LENGTH_SHORT).show()
    }
}