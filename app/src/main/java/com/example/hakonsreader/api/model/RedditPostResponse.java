package com.example.hakonsreader.api.model;

import java.util.List;


/**
 * Class representing a Reddit post.
 * <p>Contains some of the fields needed for a Reddit post. To see all fields available see
 * data returned from https://reddit.com/.json</p>
 */
public class RedditPostResponse {
    private Data data;

    // The JSON structure has a "data" object with the posts in an array called "children"
    private static class Data {
        private List<RedditPost> children;
    }

    /**
     * @return The posts retrieved from an API request
     */
    public List<RedditPost> getPosts() {
        return data.children;
    }
}
