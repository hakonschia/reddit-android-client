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
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;

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

        this.upvote.setOnClickListener(v -> this.vote(RedditApi.VoteType.Upvote));
        this.downvote.setOnClickListener(v -> this.vote(RedditApi.VoteType.Downvote));
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
    private void vote(RedditApi.VoteType voteType) {
        // Ie. if upvote is clicked when the listing is already upvoted, unvote the listing
        if (voteType == listing.getVoteType()) {
            voteType = RedditApi.VoteType.NoVote;
        }

        RedditApi.VoteType finalVoteType = voteType;

        RedditApi.Thing thing = (listing instanceof RedditPost ? RedditApi.Thing.Post : RedditApi.Thing.Comment);

        this.redditApi.vote(listing.getId(), voteType, thing, (resp) -> {
            listing.setVoteType(finalVoteType);

            updateVoteStatus();
        }, (call, t) -> {});
    }


    /**
     * Updates the vote status for a listing (button + text colors)
     */
    public void updateVoteStatus() {
        RedditApi.VoteType voteType = listing.getVoteType();

        int color = R.color.textColor;

        // Reset both buttons as at least one will change
        // (to avoid keeping the color if going from upvote to downvote and vice versa)
        upvote.getDrawable().setTint(getContext().getColor(R.color.no_vote));
        downvote.getDrawable().setTint(getContext().getColor(R.color.no_vote));

        switch (voteType) {
            case Upvote:
                color = R.color.upvoted;
                upvote.getDrawable().setTint(getContext().getColor(color));
                break;

            case Downvote:
                color = R.color.downvoted;
                downvote.getDrawable().setTint(getContext().getColor(color));
                break;

            case NoVote:
            default:
                break;
        }

        int scoreCount = listing.getScore();

        // For scores over 10000 show as "10.5k"
        if (scoreCount > 10000) {
            score.setText(String.format(
                    Locale.getDefault(),
                    getResources().getString(R.string.scoreThousands), scoreCount / 1000f)
            );
        } else {
            score.setText(String.format(Locale.getDefault(), "%d", listing.getScore()));
        }

        score.setTextColor(getContext().getColor(color));
    }
}
