package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


/**
 * Class representing a Reddit post.
 * <p>Contains some of the fields needed for a Reddit post. To see all fields available see
 * data returned from https://reddit.com/.json</p>
 */
public class RedditPostResponse {
    private Data data;

    private class Data {
        private List<RedditPost> children;
    }

    public List<RedditPost> getPosts() {
        return data.children;
    }
}
