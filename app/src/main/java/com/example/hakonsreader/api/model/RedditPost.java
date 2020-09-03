package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost {

    // The JSON structure of a post has an internal object called "data"
    private Data data;

    private static class Data {
        private String subreddit;

        private String title;
        private String author;
        private String id;
        private int score;
        private boolean spoiler;

        @SerializedName("is_video")
        private boolean isVideo;

        @SerializedName("num_comments")
        private int amountOfComments;
    }

    /**
     * @return The clean name of the subreddit (no r/ prefix)
     */
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

    public int getScore() {
        return data.score;
    }

    public boolean isSpoiler() {
        return data.spoiler;
    }

}
