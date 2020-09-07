package com.example.hakonsreader.listeners;

import android.view.View;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;

public class VoteButtonListener implements View.OnClickListener {
    private RedditApi redditApi = RedditApi.getInstance();

    private RedditPost post;
    private RedditApi.VoteType voteType;
    private Runnable onResponse;


    /**
     * Creates a new listener for clicks on vote buttons. When the listener is triggered it
     * sends an API request to cast the vote and {@code onResponse} is triggered.
     * {@code post} is updated and can be used directly to check the new status of the vote
     *
     * @param post The post to vote for. Note that this variable is directly modified
     * @param voteType The type of vote to cast
     * @param onResponse Runnable that can be used to modify UI based on the response
     */
    public VoteButtonListener(RedditPost post, RedditApi.VoteType voteType, Runnable onResponse) {
        this.post = post;
        this.voteType = voteType;
        this.onResponse = onResponse;
    }

    /**
     * Sends a request to vote on a given post
     */
    private void vote() {
        // Ie. if upvote is clicked when the post is already upvoted, unvote the post
        if (voteType == post.getVoteType()) {
            voteType = RedditApi.VoteType.NoVote;
        }

        this.redditApi.vote(post.getId(), voteType, RedditApi.Thing.Post, (call, response) -> {
            if (response.isSuccessful()) {
                post.setVoteType(voteType);
            }

            onResponse.run();
        }, (call, t) -> onResponse.run());
    }

    @Override
    public void onClick(View view) {
        this.vote();
    }
}
