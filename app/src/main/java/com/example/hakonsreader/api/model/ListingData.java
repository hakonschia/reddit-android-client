package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.jsonadapters.BooleanPrimitiveAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Base data that is common for all listings
 */
public abstract class ListingData {

    // Which subreddit the post is in
    private String subreddit;

    // The ID of the post
    private String id;

    // The title of the post
    private String title;

    // The author of the psot
    private String author;

    // The score of the post
    private int score;

    // The full link to the comments
    private String permalink;

    // Is the post locked?
    private boolean locked;

    // The UTC unix timestamp the post was created at
    @SerializedName("created_utc")
    private float createdAt;

    @SerializedName("likes")
    @JsonAdapter(BooleanPrimitiveAdapter.class)
    private Boolean liked;


    protected String getSubreddit() {
        return subreddit;
    }

    protected String getId() {
        return id;
    }

    protected String getTitle() {
        return title;
    }

    protected String getAuthor() {
        return author;
    }

    protected int getScore() {
        return score;
    }

    /**
     * Retrieve the link to the comments of a post (full link)
     *
     * @return The permalink to the post
     */
    protected String getPermalink() {
        // The link given from the Reddit API starts at "/r/..."
        return "https://reddit.com" + permalink;
    }

    protected boolean isLocked() {
        return locked;
    }

    protected float getCreatedAt() {
        return createdAt;
    }

    protected Boolean getLiked() {
        return liked;
    }


    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    protected RedditApi.VoteType getVoteType() {
        if (liked == null) {
            return RedditApi.VoteType.NoVote;
        }

        return (liked ? RedditApi.VoteType.Upvote : RedditApi.VoteType.Downvote);
    }


    /**
     * @param voteType The vote type for this post for the current user
     */
    protected void setVoteType(RedditApi.VoteType voteType) {
        // Update the internal data as that is used in getVoteType
        switch (voteType) {
            case Upvote:
                liked = true;
                break;
            case Downvote:
                liked = false;
                break;

            case NoVote:
                liked = null;
                break;
        }
    }
}