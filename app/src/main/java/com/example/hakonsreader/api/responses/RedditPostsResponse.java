package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.RedditPost;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response for retrieving posts from a subreddit
 */
public class RedditPostsResponse {

    private Data data;

    // The JSON structure has a "data" object with the posts in an array called "children"
    private static class Data {
        @SerializedName("children")
        private List<RedditPost> posts;
    }

    public List<RedditPost> getPosts() {
        return this.data.posts;
    }
}
