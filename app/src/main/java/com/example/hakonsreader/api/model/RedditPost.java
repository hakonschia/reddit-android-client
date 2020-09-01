package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

public class RedditPost {

    // The JSON structure of a post has an internal object called "data"
    private Data data;

    private static class Data {
        private String subreddit;

        @SerializedName("title")
        private String title;
        private String author;
        private String id;

        @SerializedName("is_video")
        private boolean isVideo;

        @SerializedName("num_comments")
        private int amountOfComments;
    }

    public String getSubreddit() {
        return this.data.subreddit;
    }

    public String getTitle() {
        return this.data.title;
    }

    public String getAuthor() {
        return this.data.author;
    }

    public String getId() {
        return this.data.id;
    }

    public boolean isVideo() {
        return this.data.isVideo;
    }

    public int getAmountOfComments() {
        return this.data.amountOfComments;
    }
}
