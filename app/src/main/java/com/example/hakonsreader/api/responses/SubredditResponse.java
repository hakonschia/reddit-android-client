package com.example.hakonsreader.api.responses;

import com.example.hakonsreader.api.model.Subreddit;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubredditResponse {

    private Data data;

    // The JSON structure has a "data" object with the posts in an array called "children"
    private static class Data {
        @SerializedName("children")
        private List<Subreddit> subreddits;
    }

    public List<Subreddit> getSubreddits() {
        return this.data.subreddits;
    }

}
