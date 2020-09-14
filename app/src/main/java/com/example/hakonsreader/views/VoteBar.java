package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.misc.Util;

import java.util.Locale;

/**
 * Vote bar including buttons to upvote and downvote, and a text holding the current score
 * Layout file: {@code layout/layout_vote_bar.xml}
 */
public class VoteBar extends ConstraintLayout {
    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private TextView score;
    private ImageButton upvote;
    private ImageButton downvote;

    private RedditListing listing;


    public VoteBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.layout_vote_bar, this);

        this.score = findViewById(R.id.vote_bar_score);
        this.upvote = findViewById(R.id.vote_bar_upvote);
        this.downvote = findViewById(R.id.vote_bar_downvote);

        this.upvote.setOnClickListener(v -> this.vote(VoteType.UPVOTE));
        this.downvote.setOnClickListener(v -> this.vote(VoteType.DOWNVOTE));
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

        Thing thing = (listing instanceof RedditPost ? Thing.POST : Thing.COMMENT);

        this.redditApi.vote(listing.getId(), voteType, thing, (resp) -> {
            listing.setVoteType(finalVoteType);

            updateVoteStatus();
        }, (code, t) -> {
            if (code == 503) {
                Util.showGenericServerErrorSnackbar(this);
            } else if (code == 429) {
                Util.showTooManyRequestsSnackbar(this);
            }
            t.printStackTrace();
        });
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
        upvote.setColorFilter(context.getColor(R.color.noVote));
        downvote.setColorFilter(context.getColor(R.color.noVote));

        switch (voteType) {
            case UPVOTE:
                color = R.color.upvoted;
                upvote.setColorFilter(context.getColor(color));
                break;

            case DOWNVOTE:
                color = R.color.downvoted;
                downvote.setColorFilter(context.getColor(color));
                break;

            case NO_VOTE:
            default:
                break;
        }

        int scoreCount = listing.getScore();

        if (listing.isScoreHidden()) {
            score.setText(getResources().getString(R.string.scoreHidden));
        } else {
            // For scores over 10000 show as "10.5k"
            if (scoreCount > 10000) {
                score.setText(String.format(
                        Locale.getDefault(),
                        getResources().getString(R.string.scoreThousands), scoreCount / 1000f)
                );
            } else {
                score.setText(String.format(Locale.getDefault(), "%d", listing.getScore()));
            }
        }

        score.setTextColor(context.getColor(color));
    }
}
