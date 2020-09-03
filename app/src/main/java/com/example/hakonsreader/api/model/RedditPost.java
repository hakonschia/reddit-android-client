package com.example.hakonsreader.api.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost {
    private static final String TAG = "RedditPost";
    
    // The JSON structure of a post has an internal object called "data"
    private Data data;

    private static class Data {
        // Which subreddit the post is in
        private String subreddit;

        // The ID of the post
        private String id;

        // The title of the post
        private String title;

        // The author of the psot
        private String author;

        // The score of the post
        private int score;

        // The URL of the post. For images it links to the picture, for link posts it's the link
        private String url;


        // Show spoiler tag?
        private boolean spoiler;

        // Is the post locked?
        private boolean locked;

        // Is the post NSFW?
        @SerializedName("over_18")
        private boolean nsfw;


        // The UTC unix timestamp the post was created at
        @SerializedName("created_utc")
        private float createdAt;

        // Is the post a video?
        @SerializedName("is_video")
        private boolean isVideo;

        // The amount of comments the post has
        @SerializedName("num_comments")
        private int amountOfComments;

        @SerializedName("post_hint")
        private String postHint;


        private Media media;

        // For video posts
        private static class Media {

            @SerializedName("reddit_video")
            private RedditVideo redditVideo;

            private static class RedditVideo {
                private int duration;

                @SerializedName("scrubber_media_url")
                private String url;
            }
        }
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


    public String getUrl() {
        return this.data.url;
    }

    /**
     * Possible values: "image", "hosted:video", "link"
     * If the post is a text post the hint is an empty string
     *
     * @return The hint for the post type
     */
    public String getPostHint() {
        return (data.postHint != null ? data.postHint : "");
    }
}
