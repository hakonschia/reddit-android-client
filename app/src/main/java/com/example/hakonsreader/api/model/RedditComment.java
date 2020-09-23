package com.example.hakonsreader.api.model;

import android.util.Log;

import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a Reddit comment
 */
public class RedditComment implements RedditListing {
    private static final String TAG = "RedditComment";

    private String kind;
    private Data data;
    private List<RedditComment> replies;

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

    @Override
    public boolean isMod() {
        return this.data.isMod();
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
     * Sets the depth of the comment
     *
     * <p>This should only be used when a new comment is created as the depth isn't given
     * by Reddit automatically</p>
     *
     * @param depth The depth of the comment
     */
    public void setDepth(int depth) {
        this.data.depth = depth;
    }


    /**
     * Retrieves the list of replies this comment has
     *
     * @return The list of replies
     */
    public List<RedditComment> getReplies() {
        // First time retrieving replies, create the list from data.replies
        if (this.replies == null) {
            if (this.data.replies != null) {
                replies = getRepliesInternal();
            } else {
                // No more for the comment
                replies = new ArrayList<>();
            }
        }

        return replies;
    }

    private List<RedditComment> getRepliesInternal() {
        // All the comments from the current and its replies
        List<RedditComment> all = new ArrayList<>();

        // Loop through the list of replies and add the reply and the replies to the reply
        replies = this.data.replies.getComments();
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
     * Adds a list of comments as replies. This sets the replies for every comment downwards in the chain
     * so every child has a list to its replies
     *
     * <p>Use this after retrieving new comments via {@link RedditComment#getChildren()}
     * to add the replies. Note that this function should be called on the parent of the comment
     * {@link RedditComment#getChildren()} was called on, as the comments received are replies to the
     * parent, not that object itself</p>
     */
    public void addReplies(List<RedditComment> replies) {
        if (this.replies == null) {
            this.replies = new ArrayList<>();
        }

        // Add all as replies to this comment
        this.replies.addAll(replies);

        // Each comment holds a list of the replies to itself, so for every reply add the rest of
        // the comment chain as a reply to it
        for (int i = 0; i < replies.size(); i++) {
            RedditComment reply = replies.get(i);
            Log.d(TAG, "addReplies: " + reply.getAuthor());

            // Create the chain of this replies comments and add them
            List<RedditComment> replyChain = createCommentChain(replies, i);
            if (reply.replies == null) {
                reply.replies = new ArrayList<>();
            }
            reply.replies.addAll(replyChain);
        }
    }

    /**
     * Creates a subchain of comments
     *
     * @param parentChain The parent chain to create a subchain from
     * @param pos The position of {@code parentChain} to start at
     * @return The comment chain after {@code pos}
     */
    private List<RedditComment> createCommentChain(List<RedditComment> parentChain, int pos) {
        List<RedditComment> chain = new ArrayList<>();

        RedditComment start = parentChain.get(pos);
        for(int i = pos + 1; i < parentChain.size(); i++) {
            RedditComment current = parentChain.get(i);

            if (current.getDepth() > start.getDepth()) {
                chain.add(current);
            } else {
                break;
            }
        }

        return chain;
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
