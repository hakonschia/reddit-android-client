package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.jsonadapters.BooleanPrimitiveAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost {
    private static final String TAG = "RedditPost";

    public enum PostType {
        Image, Video, RichVideo, Link, Text
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

        // Is the post a self post (ie. text post)
        @SerializedName("is_self")
        private boolean isText;

        // Is the post a video?
        @SerializedName("is_video")
        private boolean isVideo;

        // The amount of comments the post has
        @SerializedName("num_comments")
        private int amountOfComments;

        @SerializedName("post_hint")
        private String postHint;

        @SerializedName("likes")
        @JsonAdapter(BooleanPrimitiveAdapter.class)
        private Boolean liked;


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
        // If video not hosted by reddit
        if (this.data.media == null) {
            return this.getUrl();
        }
        return this.data.media.redditVideo.url;
    }

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    public RedditApi.VoteType getVoteType() {
        if (this.data.liked == null) {
            return RedditApi.VoteType.NoVote;
        }

        return (this.data.liked ? RedditApi.VoteType.Upvote : RedditApi.VoteType.Downvote);
    }

    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        // TODO reddit galleries (multiple images)
        if (data.isVideo) {
            return PostType.Video;
        } else if (data.isText) {
            return PostType.Text;
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

            case "rich:video":
                return PostType.RichVideo;

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


    /**
     * @param voteType The vote type for this post for the current user
     */
    public void setVoteType(RedditApi.VoteType voteType) {
        // Update the internal data as that is used in getVoteType

        switch (voteType) {
            case Upvote:
                this.data.liked = true;
                break;
            case Downvote:
                this.data.liked = false;
                break;

            case NoVote:
                this.data.liked = null;
                break;
        }
    }

}
