package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;


/**
 * Class representing a Reddit post.
 * <p>Contains some of the fields needed for a Reddit post. To see all fields available see
 * data returned from https://reddit.com/.json</p>
 */
public class RedditPost {
    private String subreddit;
    private String title;
    private String author;
    private String id;

    @SerializedName("is_video")
    private boolean isVideo;

    @SerializedName("num_comments")
    private int amountOfComments;

    public RedditPost(String subreddit, String title, String author, String id, boolean isVideo, int amountOfComments) {
        this.subreddit = subreddit;
        this.title = title;
        this.author = author;
        this.id = id;
        this.isVideo = isVideo;
        this.amountOfComments = amountOfComments;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getId() {
        return id;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public int getAmountOfComments() {
        return amountOfComments;
    }
}
