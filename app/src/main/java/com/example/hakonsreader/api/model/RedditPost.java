package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost {
    private static final String TAG = "RedditPost";

    public enum PostType {
        Image, Video, Link, Text
    }


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

        // The full link to the comments
        private String permalink;


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

        @SerializedName("likes")
        private boolean liked;



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

    public String getVideoUrl() {
        return this.data.media.redditVideo.url;
    }

    /**
     * Retrieve information about the currently logged in users vote on a post
     *
     * @return Returns true if the post is upvoted, false if downvoted, null if no vote is cast
     */
    public boolean getLiked() {
        return this.data.liked;
    }

    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        if (data.isVideo) {
            return PostType.Video;
        }

        String hint = data.postHint;

        // Text posts don't have a hint
        if (hint == null) {
            return PostType.Text;
        }

        if (hint.equals("link")) {
            // Link posts might be images not uploaded to reddit
            if (hint.matches("(.png|.jpeg|.jpg)$")) {
                return PostType.Image;
            } else if (hint.matches(".gifv")) {
                return PostType.Video;
            }

            return PostType.Link;
        }

        switch (hint) {
            case "image":
                // .gif is treated as image
                if (data.url.endsWith(".gif")) {
                    return PostType.Video;
                }

                return PostType.Image;

            case "hosted:video":
                return PostType.Video;

            // No hint means it's a text post
            default:
                return PostType.Text;
        }
    }

    /**
     * Retrieve the link to the comments of a post (full link)
     *
     * @return The permalink to the post
     */
    public String getPermalink() {
        // The link given from the Reddit API starts at "/r/..."
        return "https://reddit.com" + data.permalink;
    }

}
