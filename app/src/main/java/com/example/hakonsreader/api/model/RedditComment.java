package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RedditComment implements RedditListing {
    private static final String TAG = "RedditComment";

    private String kind;
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

        @SerializedName("score_hidden")
        private boolean scoreHidden;



        /* Fields for when the comment is a "load more comments" comment */
        @SerializedName("count")
        private int extraCommentsCount;

        private List<String> children;
    }
    /* --------------------- Inherited from ListingData --------------------- */

    /**
     * Retrieves the kind of listing
     * @return
     */
    @Override
    public String getKind() {
        return this.kind;
    }

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

    @Override
    public boolean isStickied() {
        return this.data.getStickied();
    }

    /**
     * Should the score be hidden?
     *
     * @return True if the score should be hidden
     */
    @Override
    public boolean isScoreHidden() {
        return this.data.scoreHidden;
    }


    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    @Override
    public VoteType getVoteType() {
        return this.data.getVoteType();
    }
    /**
     * @param voteType The vote type for this post for the current user
     */
    @Override
    public void setVoteType(VoteType voteType) {
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

        // All the comments from the current and its replies
        List<RedditComment> all = new ArrayList<>();

        // Loop through the list of replies and add the reply and the replies to the reply
        List<RedditComment> replies = this.data.replies.getComments();
        for (RedditComment reply : replies) {
            all.add(reply);
            all.addAll(reply.getReplies());
        }

        return all;
    }


    /**
     * Retrieves the children of the comment (for loading more comments)
     * <p>Note that if {@link RedditComment#getKind()} isn't "more" this will null</p>
     *
     * @return The list of ID's of the child comments
     */
    public List<String> getChildren() {
        return this.data.children;
    }

    /**
     * The amount of extra child comments
     * <p>Note that if {@link RedditComment#getKind()} isn't "more" this will always be 0</p>
     *
     * @return
     */
    public int getExtraCommentsCount() {
        return this.data.extraCommentsCount;
    }
}
