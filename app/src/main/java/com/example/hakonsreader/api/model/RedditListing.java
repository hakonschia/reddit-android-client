package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.jsonadapters.BooleanPrimitiveAdapter;
import com.example.hakonsreader.api.jsonadapters.ListingAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;


/**
 * Base class with data common for all types of listings
 */
@JsonAdapter(ListingAdapter.class)
public abstract class RedditListing {
    @SerializedName("kind")
    protected String kind;

    @SerializedName("id")
    protected String id;

    @SerializedName("subreddit")
    protected String subreddit;

    @SerializedName("author")
    protected String author;

    @SerializedName("url")
    protected String url;

    @SerializedName("name")
    protected String fullname;

    @SerializedName("created_utc")
    protected float createdAt;

    @SerializedName("over_18")
    protected boolean nsfw;

    @SerializedName("permalink")
    private String permalink;

    @SerializedName("locked")
    private boolean isLocked;

    @SerializedName("stickied")
    private boolean isStickied;

    /**
     * What the listing is distinguished as (such as "moderator")
     */
    @SerializedName("distinguished")
    private String distinguished;

    @SerializedName("score")
    private int score;

    @SerializedName("score_hidden")
    private boolean scoreHidden;

    @SerializedName("likes")
    @JsonAdapter(BooleanPrimitiveAdapter.class)
    private Boolean liked;

    /**
     * @return What kind of listing this is
     */
    public String getKind() {
        return kind;
    }

    /**
     * @return The ID of the listing
     */
    public String getId() {
        return id;
    }

    /**
     * @return The subreddit the listing is located in
     */
    public String getSubreddit() {
        return subreddit;
    }

    /**
     * @return The author of the listing
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return The URL of the listing
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return The fullname of the listing (equivalent to "{@link RedditListing#getKind()} + "_" + {@link RedditListing#getId()})
     */
    public String getFullname() {
        return fullname;
    }

    /**
     * @return The unix timestamp in milliseconds when the listing was created
     */
    public long getCreatedAt() {
        return (long)createdAt;
    }

    /**
     * @return True if the listing is NSFW (over 18)
     */
    public boolean isNsfw() {
        return nsfw;
    }

    /**
     * @return The permalink to the listing
     */
    public String getPermalink() {
        return permalink;
    }

    /**
     * @return True if the listing has been locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * @return True if the listing is stickied
     */
    public boolean isStickied() {
        return isStickied;
    }

    /**
     * @return True if the listing is distinguished as a moderator
     */
    public boolean isMod() {
        if (distinguished == null) {
            return false;
        }

        return distinguished.equals("moderator");
    }

    /**
     * @return The score of the listing
     */
    public int getScore() {
        return score;
    }

    /**
     * @return True if the score should be hidden
     */
    public boolean isScoreHidden() {
        return scoreHidden;
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
