package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RedditComment implements RedditListing {

    private Data data;

    private static class Data extends ListingData {
        private String body;

        @SerializedName("body_html")
        private String bodyHtml;

        // How far in we are in a comment chain
        private int depth;

        // If there are no replies the replies is {"replies" : ""} which would cause an error
        // as gson tries to convert it to a string, so convert to null instead
        @JsonAdapter(EmptyStringAsNullAdapter.class)
        private RedditCommentsResponse replies;
    }

    /* --------------------- Inherited from ListingData --------------------- */
    /**
     * @return The clean name of the subreddit (no r/ prefix)
     */
    @Override
    public String getSubreddit() {
        return this.data.getSubreddit();
    }

    @Override
    public String getId() {
        return this.data.getId();
    }

    @Override
    public String getTitle() {
        return this.data.getTitle();
    }

    @Override
    public String getAuthor() {
        return this.data.getAuthor();
    }

    @Override
    public int getScore() {
        return this.data.getScore();
    }

    @Override
    public Boolean getLiked() {
        return this.data.getLiked();
    }

    @Override
    public boolean isLocked() {
        return this.data.isLocked();
    }

    /**
     * Retrieve the link to the comments of a post (full link)
     *
     * @return The permalink to the post
     */
    @Override
    public String getPermalink() {
        return this.data.getPermalink();
    }

    /**
     * @return The unix timestamp in UTC when the post was created
     */
    @Override
    public long getCreatedAt() {
        return (long)data.getCreatedAt();
    }


    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    @Override
    public RedditApi.VoteType getVoteType() {
        return this.data.getVoteType();
    }
    /**
     * @param voteType The vote type for this post for the current user
     */
    @Override
    public void setVoteType(RedditApi.VoteType voteType) {
        this.data.setVoteType(voteType);
    }
    /* --------------------- End inherited from ListingData --------------------- */




    /**
     * Retrieves the raw body. For HTML use {@link RedditComment#getBodyHtml()}
     *
     * @return The raw string of the comment
     */
    public String getBody() {
        return this.data.body;
    }

    /**
     * Retrieves the body in HTML. For raw text use {@link RedditComment#getBody()}
     *
     * @return The HTML string of the comment
     */
    public String getBodyHtml() {
        return this.data.bodyHtml;
    }

    /**
     * Retrieve the depth of the comment, ie. how far in we are in a comment chain
     *
     * @return The depth of the comment
     */
    public int getDepth() {
        return this.data.depth;
    }

    /**
     * Retrieves the list of replies this comment has
     *
     * @return The list of replies
     */
    public List<RedditComment> getReplies() {
        // No replies
        if (this.data.replies == null) {
            return new ArrayList<>();
        }

        return this.data.replies.getComments();
    }

}
