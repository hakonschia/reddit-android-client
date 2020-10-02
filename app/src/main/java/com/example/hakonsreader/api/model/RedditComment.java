package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.PostableListing;
import com.example.hakonsreader.api.interfaces.VotableListing;
import com.example.hakonsreader.api.jsonadapters.BooleanPrimitiveAdapter;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a Reddit comment
 */
public class RedditComment implements VotableListing, PostableListing {
    private static final String TAG = "RedditComment";

    @SerializedName("kind")
    private String kind;

    @SerializedName("data")
    private Data data;

    /**
     * Replies is set from {@link Data#replies}
     */
    private List<RedditComment> replies;


    /**
     * Comment specific data
     */
    private static class Data {
        /* ------------- RedditListing ------------- */
        @SerializedName("id")
        private String id;

        @SerializedName("url")
        private String url;

        @SerializedName("name")
        private String fullname;

        @SerializedName("created_utc")
        private float createdAt;

        @SerializedName("over_18")
        private boolean nsfw;
        /* ------------- End RedditListing ------------- */

        /* ------------- PostableListing ------------- */
        @SerializedName("subreddit")
        private String subreddit;

        @SerializedName("author")
        private String author;

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
        /* ------------- End PostableListing ------------- */

        /* ------------- VoteableListing ------------- */
        @SerializedName("score")
        private int score;

        @SerializedName("score_hidden")
        private boolean scoreHidden;

        @SerializedName("likes")
        @JsonAdapter(BooleanPrimitiveAdapter.class)
        private Boolean liked;
        /* ------------- End VoteableListing ------------- */


        @SerializedName("body")
        private String body;

        @SerializedName("body_html")
        private String bodyHtml;

        /**
         * How far in the comment is in a comment chain
         */
        @SerializedName("depth")
        private int depth;

        // If there are no replies the replies is {"replies" : ""} which would cause an error
        // as gson tries to convert it to a string, so convert to null instead
        @JsonAdapter(EmptyStringAsNullAdapter.class)
        @SerializedName("replies")
        private RedditCommentsResponse replies;

        /* Fields for when the comment is a "load more comments" comment */
        @SerializedName("count")
        private int extraCommentsCount;

        /**
         * The list of IDs of more comments to be loaded
         */
        @SerializedName("children")
        private List<String> children;
    }



    /* --------------------- Inherited --------------------- */
    /* ------------- RedditListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return data.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        return data.url;
    }

    /**
     * {@inheritDoc}
     */@Override
    public String getFullname() {
        return data.fullname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreatedAt() {
        return (long)data.createdAt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNSFW() {
        return data.nsfw;
    }
    /* ------------- End RedditListing ------------- */

    /* ------------- PostableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubreddit() {
        return data.subreddit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthor() {
        return data.author;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPermalink() {
        return data.permalink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        return data.isLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStickied() {
        return data.isStickied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMod() {
        if (data.distinguished == null) {
            return false;
        }

        return data.distinguished.equals("moderator");
    }
    /* ------------- End PostableListing ------------- */

    /* ------------- VoteableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return data.score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScoreHidden() {
        return data.scoreHidden;
    }

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    public VoteType getVoteType() {
        if (data.liked == null) {
            return VoteType.NO_VOTE;
        }

        return (data.liked ? VoteType.UPVOTE : VoteType.DOWNVOTE);
    }

    /**
     * @param voteType The vote type for this post for the current user
     */
    public void setVoteType(VoteType voteType) {
        // Update the internal data as that is used in getVoteType
        switch (voteType) {
            case UPVOTE:
                data.liked = true;
                break;
            case DOWNVOTE:
                data.liked = false;
                break;
            case NO_VOTE:
                data.liked = null;
                break;
        }
    }
    /* ------------- End VoteableListing ------------- */
    /* --------------------- End inherited --------------------- */



    /**
     * Retrieves the raw body. For HTML use {@link RedditComment#getBodyHtml()}
     *
     * @return The raw string of the comment
     */
    public String getBody() {
        return data.body;
    }

    /**
     * Retrieves the body in HTML. For raw text use {@link RedditComment#getBody()}
     *
     * @return The HTML string of the comment
     */
    public String getBodyHtml() {
        return data.bodyHtml;
    }

    /**
     * Retrieve the depth of the comment, ie. how far in we are in a comment chain
     *
     * @return The depth of the comment
     */
    public int getDepth() {
        return data.depth;
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
        data.depth = depth;
    }


    /**
     * Retrieves the list of replies this comment has
     *
     * @return The list of replies
     */
    public List<RedditComment> getReplies() {
        // First time retrieving replies, create the list from data.replies
        if (replies == null) {
            if (data.replies != null) {
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
        replies = data.replies.getComments();
        for (RedditComment reply : replies) {
            all.add(reply);
            all.addAll(reply.getReplies());
        }

        return all;
    }

    /**
     * Removes a reply from the comment
     *
     * <p>The reply must be a direct child</p>
     *
     * @param reply The reply to remove
     */
    public void removeReply(RedditComment reply) {
        replies.remove(reply);
    }


    /**
     * Retrieves the children of the comment (for loading more comments)
     * <p>Note that if {@link RedditComment#getKind()} isn't "more" this will null</p>
     *
     * @return The list of ID's of the child comments
     */
    public List<String> getChildren() {
        return data.children;
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
        if (replies == null) {
            replies = new ArrayList<>();
        }

        // Add all as replies to this comment
        this.replies.addAll(replies);

        // Each comment holds a list of the replies to itself, so for every reply add the rest of
        // the comment chain as a reply to it
        for (int i = 0; i < replies.size(); i++) {
            RedditComment reply = replies.get(i);

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
        return data.extraCommentsCount;
    }
}
