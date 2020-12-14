package com.example.hakonsreader.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.interfaces.VoteableListing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.VoteBarBinding
import com.example.hakonsreader.misc.Util
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.robinhood.ticker.TickerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class VoteBar : FrameLayout {
    // TODO when you upvote a post, go into the post, unvote/downvote, then go out to the list again
    //  and cast another vote it will be unsynced since it doesn't update the post in the list of posts
    //  Not really relevant to this class, but putting it here as a reminder

    private lateinit var binding: VoteBarBinding
    var hideScore = false
    var listing: VoteableListing? = null
        set(value) {
            field = value
            updateVoteStatus()
        }

    constructor(context: Context) : super(context) {
        this.setupBinding()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.setupBinding()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.setupBinding()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.setupBinding()
    }

    private fun setupBinding() {
        binding = VoteBarBinding.inflate(LayoutInflater.from(context), this, true)

        binding.upvote.setOnClickListener { this.vote(VoteType.UPVOTE) }
        binding.downvote.setOnClickListener { this.vote(VoteType.DOWNVOTE) }

        // Include both in case it goes from "9999" to "10K"
        binding.score.setCharacterLists(TickerUtils.provideAlphabeticalList(), TickerUtils.provideNumberList())
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
            updateVoteStatus()

            val id = it.id

            val api = App.get().api
            CoroutineScope(IO).launch {
                val resp = if (listing is RedditPost) {
                    api.post(id)
                } else {
                    api.comment(id)
                }.vote(actualVote)

                when (resp) {
                    is ApiResponse.Success -> {}
                    is ApiResponse.Error -> {
                        // Request failed, set back to default
                        it.voteType = currentVote
                        withContext(Main) {
                            updateVoteStatus()
                            resp.throwable.printStackTrace()
                            Util.handleGenericResponseErrors(this@VoteBar, resp.error, resp.throwable)
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
     */
    fun updateVoteStatus() {
        if (listing == null) {
            return
        }

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        binding.upvote.setColorFilter(context.getColor(R.color.noVote))
        binding.downvote.setColorFilter(context.getColor(R.color.noVote))

        listing?.let {
            binding.score.textColor = when (it.voteType) {
                VoteType.UPVOTE -> {
                    binding.upvote.setColorFilter(context.getColor(R.color.upvoted))
                    context.getColor(R.color.upvoted)
                }
                VoteType.DOWNVOTE -> {
                    binding.downvote.setColorFilter(context.getColor(R.color.downvoted))
                    context.getColor(R.color.downvoted)
                }
                VoteType.NO_VOTE -> {
                    context.getColor(R.color.text_color)
                }
            }

            if (it.isScoreHidden || hideScore) {
                binding.score.text = resources.getString(R.string.scoreHidden)
            } else {
                val scoreCount = it.score

                // For scores over 10000 show as "10.5k"
                binding.score.text = if (scoreCount >= 10000) {
                    resources.getString(R.string.scoreThousands, scoreCount / 1000f)
                } else {
                   scoreCount.toString()
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
     * Check if the TickerView for the score has animation enabled
     *
     * @return True if the animation is enabled
     */
    fun tickerAnimationEnabled(): Boolean {
        return binding.score.animationDuration != 0L
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