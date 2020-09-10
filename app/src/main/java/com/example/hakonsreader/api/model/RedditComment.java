package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RedditComment {

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
