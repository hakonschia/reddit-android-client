package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost implements RedditListing {
    private static final String TAG = "RedditPost";

    public enum PostType {
        Image, Video, RichVideo, Link, Text
    }

    private String kind;
    public Data data;

    public static class Data extends ListingData {

        // The URL of the post. For images it links to the picture, for link posts it's the link
        protected String url;

        // The amount of comments the post has
        @SerializedName("num_comments")
        protected int amountOfComments;

        // Is the post a self post (ie. text post)
        @SerializedName("is_self")
        protected boolean isText;

        // Is the post a video?
        @SerializedName("is_video")
        protected boolean isVideo;

        protected boolean spoiler;

        @SerializedName("hide_score")
        private boolean scoreHidden;


        protected String thumbnail;

        // Is the post NSFW?
        @SerializedName("over_18")
        protected boolean nsfw;

        @SerializedName("post_hint")
        protected String postHint;


        // For video posts
        protected Media media;
        private static class Media {

            @SerializedName("reddit_video")
            protected RedditVideo redditVideo;

            protected static class RedditVideo {
                protected int duration;

                @SerializedName("scrubber_media_url")
                protected String url;
            }
        }
    }


    /* --------------------- Inherited from ListingData --------------------- */
    @Override
    public String getKind() {
        return this.kind;
    }

    /**
     * @return The clean name of the subreddit (no r/ prefix)
     */
    @Override
    public String getSubreddit() {
        return this.data.getSubreddit();
    }

    @Override
    public String getId() {
        return this.data.getId();
    }

    @Override
    public String getTitle() {
        return this.data.getTitle();
    }

    @Override
    public String getAuthor() {
        return this.data.getAuthor();
    }

    @Override
    public int getScore() {
        return this.data.getScore();
    }

    @Override
    public Boolean getLiked() {
        return this.data.getLiked();
    }

    @Override
    public boolean isLocked() {
        return this.data.isLocked();
    }

    /**
     * Should the score be hidden?
     *
     * @return True if the score should be hidden
     */
    @Override
    public boolean isScoreHidden() {
        return this.data.scoreHidden;
    }

    /**
     * Retrieve the link to the comments of a post (full link)
     *
     * @return The permalink to the post
     */
    @Override
    public String getPermalink() {
        return this.data.getPermalink();
    }

    /**
     * @return The unix timestamp in UTC when the post was created
     */
    @Override
    public long getCreatedAt() {
        return (long)data.getCreatedAt();
    }

    @Override
    public boolean isStickied() {
        return data.getStickied();
    }

    @Override
    public String getDistinguished() {
        return data.getDistinguished();
    }

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    @Override
    public VoteType getVoteType() {
        return this.data.getVoteType();
    }
    /**
     * @param voteType The vote type for this post for the current user
     */
    @Override
    public void setVoteType(VoteType voteType) {
        this.data.setVoteType(voteType);
    }
    /* --------------------- End inherited from ListingData --------------------- */



    public String getThumbnail() {
        return this.data.thumbnail;
    }

    public String getUrl() {
        return this.data.url;
    }

    public boolean isSpoiler() {
        return this.data.spoiler;
    }

    public boolean isVideo() {
        return this.data.isVideo;
    }

    public int getAmountOfComments() {
        return this.data.amountOfComments;
    }

    public String getVideoUrl() {
        // If video not hosted by reddit
        if (this.data.media == null) {
            return this.getUrl();
        }
        return this.data.media.redditVideo.url;
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
}
