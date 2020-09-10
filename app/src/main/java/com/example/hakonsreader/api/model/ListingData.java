package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.VoteType;
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

    private boolean stickied;

    private String distinguished;


    public String getSubreddit() {
        return subreddit;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getScore() {
        return score;
    }

    /**
     * Retrieve the link to the comments of a post (full link)
     *
     * @return The permalink to the post
     */
    public String getPermalink() {
        // The link given from the Reddit API starts at "/r/..."
        return "https://reddit.com" + permalink;
    }

    public boolean isLocked() {
        return locked;
    }

    public float getCreatedAt() {
        return createdAt;
    }

    public Boolean getLiked() {
        return liked;
    }

    public boolean getStickied() {
        return stickied;
    }

    public boolean isMod() {
        if (distinguished == null) {
            return false;
        }

        // TODO fix magic string
        return distinguished.equals("moderator");
    }


    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    public VoteType getVoteType() {
        if (liked == null) {
            return VoteType.NO_VOTE;
        }

        return (liked ? VoteType.UPVOTE : VoteType.DOWNVOTE);
    }


    /**
     * @param voteType The vote type for this post for the current user
     */
    public void setVoteType(VoteType voteType) {
        // Update the internal data as that is used in getVoteType
        switch (voteType) {
            case UPVOTE:
                liked = true;
                break;
            case DOWNVOTE:
                liked = false;
                break;

            case NO_VOTE:
                liked = null;
                break;
        }
    }
}