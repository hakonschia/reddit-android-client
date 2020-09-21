package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.LayoutVoteBarBinding;
import com.example.hakonsreader.misc.Util;

import java.util.Locale;

/**
 * Vote bar including buttons to upvote and downvote, and a text holding the current score
 * <p>Layout file: {@code layout/layout_vote_bar.xml}</p>
 */
public class VoteBar extends ConstraintLayout {
    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private LayoutVoteBarBinding binding;

    private RedditListing listing;


    public VoteBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.binding = LayoutVoteBarBinding.inflate(inflater, this, true);

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
        // Ie. if upvote is clicked when the listing is already upvoted, unvote the listing
        if (voteType == listing.getVoteType()) {
            voteType = VoteType.NO_VOTE;
        }

        VoteType finalVoteType = voteType;

        this.redditApi.vote(listing, voteType, (resp) -> {
            listing.setVoteType(finalVoteType);
            updateVoteStatus();
        }, (code, t) -> Util.handleGenericResponseErrors(this, code, t));
    }


    /**
     * Updates the vote status for a listing (button + text colors)
     */
    public void updateVoteStatus() {
        VoteType voteType = listing.getVoteType();

        Context context = getContext();

        int color = R.color.textColor;

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        this.binding.upvote.setColorFilter(context.getColor(R.color.noVote));
        this.binding.downvote.setColorFilter(context.getColor(R.color.noVote));

        switch (voteType) {
            case UPVOTE:
                color = R.color.upvoted;
                this.binding.upvote.setColorFilter(context.getColor(color));
                break;

            case DOWNVOTE:
                color = R.color.downvoted;
                this.binding.downvote.setColorFilter(context.getColor(color));
                break;

            case NO_VOTE:
            default:
                break;
        }

        int scoreCount = listing.getScore();

        if (listing.isScoreHidden()) {
            this.binding.score.setText(getResources().getString(R.string.scoreHidden));
        } else {
            // For scores over 10000 show as "10.5k"
            if (scoreCount > 10000) {
                this.binding.score.setText(String.format(
                        Locale.getDefault(),
                        getResources().getString(R.string.scoreThousands), scoreCount / 1000f)
                );
            } else {
                this.binding.score.setText(String.format(Locale.getDefault(), "%d", listing.getScore()));
            }
        }

        this.binding.score.setTextColor(context.getColor(color));
    }
}
