package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.databinding.VoteBarBinding;
import com.example.hakonsreader.misc.Util;

import java.util.Locale;

/**
 * Vote bar including buttons to upvote and downvote, and a text holding the current score
 * <p>Layout file: {@code layout/vote_bar.xml}</p>
 */
public class VoteBar extends ConstraintLayout {
    private RedditApi redditApi = App.get().getApi();

    private VoteBarBinding binding;

    private RedditListing listing;


    public VoteBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.binding = VoteBarBinding.inflate(inflater, this, true);

        this.binding.upvote.setOnClickListener(v -> this.vote(VoteType.UPVOTE));
        this.binding.downvote.setOnClickListener(v -> this.vote(VoteType.DOWNVOTE));
    }

    /**
     * Sets the listing to use in this VoteBar and sets the initial state of the vote status
     *
     * @param listing The listing to set
     */
    public void setListing(@NonNull RedditListing listing) {
        this.listing = listing;
        // Make sure the initial status is up to date
        this.updateVoteStatus();
    }

    /**
     * Sends a request to vote on a given listing
     *
     * @param voteType The vote type to cast
     */
    private void vote(VoteType voteType) {
        VoteType current = listing.getVoteType();

        // Ie. if upvote is clicked when the listing is already upvoted, unvote the listing
        if (voteType == current) {
            voteType = VoteType.NO_VOTE;
        }

        VoteType finalVoteType = voteType;

        // Assume it's successful as it feels like the buttons aren't pressed when you have to wait
        // until the colors are updated
        listing.setVoteType(finalVoteType);
        updateVoteStatus();

        this.redditApi.vote(listing, voteType, resp -> { }, (code, t) -> {
            // On failure set back to previous so the user doesn't think it updated when it didn't
            listing.setVoteType(current);
            updateVoteStatus();
            Util.handleGenericResponseErrors(this, code, t);
        });
    }


    /**
     * Updates the vote status for a listing (button + text colors)
     */
    public void updateVoteStatus() {
        VoteType voteType = listing.getVoteType();

        Context context = getContext();

        int color = R.color.text_color;

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        this.binding.upvote.setColorFilter(context.getColor(R.color.noVote));
        this.binding.downvote.setColorFilter(context.getColor(R.color.noVote));

        switch (voteType) {
            case UPVOTE:
                color = R.color.upvoted;
                binding.upvote.setColorFilter(context.getColor(color));
                break;

            case DOWNVOTE:
                color = R.color.downvoted;
                binding.downvote.setColorFilter(context.getColor(color));
                break;

            case NO_VOTE:
            default:
                break;
        }

        binding.score.setTextColor(context.getColor(color));

        int scoreCount = listing.getScore();

        if (listing.isScoreHidden()) {
            binding.score.setText(getResources().getString(R.string.scoreHidden));
        } else {
            // For scores over 10000 show as "10.5k"
            if (scoreCount > 10000) {
                binding.score.setText(String.format(
                        Locale.getDefault(),
                        getResources().getString(R.string.scoreThousands), scoreCount / 1000f)
                );
            } else {
                binding.score.setText(String.format(Locale.getDefault(), "%d", listing.getScore()));
            }
        }
    }

    /**
     * Enables or disables the animation for any {@link com.robinhood.ticker.TickerView} found
     * in this view
     *
     * @param enable True to enable
     */
    public void enableTickerAnimation(boolean enable) {
        binding.score.setAnimationDelay(enable ? (long)getResources().getInteger(R.integer.tickerAnimationDefault) : 0);
    }
}
