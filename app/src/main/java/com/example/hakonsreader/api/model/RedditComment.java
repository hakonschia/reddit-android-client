package com.example.hakonsreader.api.model;

import java.util.List;

public class RedditComment extends RedditListing {

    /**
     * Create a comment object from a base listing
     *
     * @param base The base listing to create from
     * @return A post object with the values from {@code base}
     */
    public static RedditComment createFromListing(RedditListing base) {
        return base.createFromListing(RedditComment.class);
    }

    private List<RedditComment> replies;

    public List<RedditComment> getReplies() {
        return replies;
    }
}
