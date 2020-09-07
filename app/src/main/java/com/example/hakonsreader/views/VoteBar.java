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
import com.example.hakonsreader.api.model.RedditPost;

import java.util.Locale;

/**
 * Vote bar including buttons to upvote and downvote, and a text holding the current score
 * Layout file: {@code layout/layout_vote_bar.xml}
 */
public class VoteBar extends ConstraintLayout {
    private RedditApi redditApi = RedditApi.getInstance();

    private TextView score;
    private ImageButton upvote;
    private ImageButton downvote;

    private RedditPost post;


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
     * Sets the post to use in this VoteBar and sets the initial state of the vote status
     *
     * @param post The post to set
     */
    public void setPost(@NonNull RedditPost post) {
        this.post = post;
        // Make sure the initial status is up to date
        this.updateVoteStatus();
    }

    /**
     * Sends a request to vote on a given post
     *
     * @param voteType The vote type to cast
     */
    private void vote(RedditApi.VoteType voteType) {
        // Ie. if upvote is clicked when the post is already upvoted, unvote the post
        if (voteType == post.getVoteType()) {
            voteType = RedditApi.VoteType.NoVote;
        }

        RedditApi.VoteType finalVoteType = voteType;
        this.redditApi.vote(post.getId(), voteType, RedditApi.Thing.Post, (call, response) -> {
            if (response.isSuccessful()) {
                post.setVoteType(finalVoteType);

                updateVoteStatus();
            }
        }, (call, t) -> {});
    }


    /**
     * Updates the vote status for a post (button + text colors)
     */
    public void updateVoteStatus() {
        RedditApi.VoteType voteType = post.getVoteType();

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

        score.setText(String.format(Locale.getDefault(), "%d", post.getScore()));
        score.setTextColor(getContext().getColor(color));
    }
}
