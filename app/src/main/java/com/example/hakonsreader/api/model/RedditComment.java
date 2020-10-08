package com.example.hakonsreader.api.model;

import android.util.Log;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.utils.MarkdownAdjuster;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a Reddit comment
 */
public class RedditComment extends RedditListing {
    private static final String TAG = "RedditComment";

    private static MarkdownAdjuster adjuster = new MarkdownAdjuster.Builder()
            .checkHeaderSpaces()
            .checkRedditSpecificLinks()
            .build();


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
    private ListingResponse repliesInternal;

    /**
     * The replies are retrieved from {@link RedditComment#repliesInternal} and set here when they are
     * retrieved with {@link RedditComment#getReplies()}
     */
    private List<RedditComment> repliesActual;


    /* Fields for when the comment is a "load more comments" comment */
    @SerializedName("count")
    private int extraCommentsCount;

    /**
     * The list of IDs of more comments to be loaded
     */
    @SerializedName("children")
    private List<String> children;


    /**
     * Retrieves the markdown body. For HTML use {@link RedditComment#getBodyHtml()}
     *
     * @return The markdown string of the comment
     */
    public String getBody(boolean adjustFormatting) {
        String text = body;
        if (adjustFormatting) {
            text = adjuster.adjust(body);
        }

        return text;
    }

    /**
     * Retrieves the body in HTML. For markdown text use {@link RedditComment#getBody(boolean)}
     *
     * @return The HTML string of the comment
     */
    public String getBodyHtml() {
        return bodyHtml;
    }

    /**
     * Retrieve the depth of the comment, ie. how far in we are in a comment chain
     *
     * @return The depth of the comment
     */
    public int getDepth() {
        return depth;
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
        this.depth = depth;
    }

    /**
     * Retrieves the list of replies this comment has
     *
     * @return The list of replies
     */
    public List<RedditComment> getReplies() {
        // First time retrieving replies, create the list from data.replies
        if (repliesActual == null) {
            if (repliesInternal != null) {
                repliesActual = getRepliesInternal();
            } else {
                // No more for the comment
                repliesActual = new ArrayList<>();
            }
        }

        return repliesActual;
    }

    private List<RedditComment> getRepliesInternal() {
        // All the comments from the current and its replies
        List<RedditComment> all = new ArrayList<>();

        // Loop through the list of replies and add the reply and the replies to the reply
        repliesActual = (List<RedditComment>) repliesInternal.getListings();
        for (RedditComment reply : repliesActual) {
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
        repliesActual.remove(reply);
    }


    /**
     * Retrieves the children of the comment (for loading more comments)
     * <p>Note that if {@link RedditComment#getKind()} isn't "more" this will null</p>
     *
     * @return The list of ID's of the child comments
     */
    public List<String> getChildren() {
        return children;
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
        this.repliesActual.addAll(replies);

        // Each comment holds a list of the replies to itself, so for every reply add the rest of
        // the comment chain as a reply to it
        for (int i = 0; i < replies.size(); i++) {
            RedditComment reply = replies.get(i);

            // Create the chain of this replies comments and add them
            List<RedditComment> replyChain = createCommentChain(replies, i);
            if (reply.repliesActual == null) {
                reply.repliesActual = new ArrayList<>();
            }
            reply.repliesActual.addAll(replyChain);
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
        return extraCommentsCount;
    }
}
