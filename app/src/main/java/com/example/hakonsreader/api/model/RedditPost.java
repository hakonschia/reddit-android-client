package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.interfaces.PostableListing;
import com.example.hakonsreader.api.interfaces.VotableListing;
import com.example.hakonsreader.api.jsonadapters.BooleanPrimitiveAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing a Reddit post
 */
public class RedditPost implements VotableListing, PostableListing {
    private static final String TAG = "RedditPost";

    @SerializedName("kind")
    private String kind;

    @SerializedName("data")
    public Data data;


    /**
     * Post specific data
     */
    private static class Data {
        /* ------------- RedditListing ------------- */
        @SerializedName("id")
        private String id;

        @SerializedName("url")
        private String url;

        @SerializedName("name")
        private String fullname;

        @SerializedName("created_utc")
        private float createdAt;

        @SerializedName("over_18")
        private boolean nsfw;
        /* ------------- End RedditListing ------------- */

        /* ------------- PostableListing ------------- */
        @SerializedName("subreddit")
        private String subreddit;

        @SerializedName("author")
        private String author;

        @SerializedName("permalink")
        private String permalink;

        @SerializedName("is_locked")
        private boolean isLocked;

        @SerializedName("is_stickied")
        private boolean isStickied;

        /**
         * What the listing is distinguished as (such as "moderator")
         */
        @SerializedName("distinguished")
        private String distinguished;
        /* ------------- End PostableListing ------------- */

        /* ------------- End VoteableListing ------------- */
        @SerializedName("score")
        private int score;

        @SerializedName("score_hidden")
        private boolean scoreHidden;

        @SerializedName("likes")
        @JsonAdapter(BooleanPrimitiveAdapter.class)
        private Boolean liked;
        /* ------------- End VoteableListing ------------- */



        @SerializedName("title")
        private String title;

        @SerializedName("num_comments")
        private int amountOfComments;

        @SerializedName("thumbnail")
        private String thumbnail;

        @SerializedName("spoiler")
        private boolean spoiler;

        @SerializedName("selftext_html")
        private String selftextHtml;


        @SerializedName("is_self")
        private boolean isText;

        @SerializedName("is_video")
        private boolean isVideo;

        @SerializedName("post_hint")
        private String postHint;


        @SerializedName("media")
        private Media media;

        /**
         * Data for video posts
         */
        private static class Media {

            @SerializedName("reddit_video")
            private RedditVideo redditVideo;

            private static class RedditVideo {
                private int duration;

                @SerializedName("fallback_url")
                private String url;

                @SerializedName("height")
                private int height;

                @SerializedName("width")
                private int width;
            }
        }
    }


    /* --------------------- Inherited --------------------- */
    /* ------------- RedditListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return data.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        return data.url;
    }

    /**
     * {@inheritDoc}
     */@Override
    public String getFullname() {
        return data.fullname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreatedAt() {
        return (long)data.createdAt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNSFW() {
        return data.nsfw;
    }
    /* ------------- End RedditListing ------------- */

    /* ------------- PostableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubreddit() {
        return data.subreddit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthor() {
        return data.author;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPermalink() {
        return data.permalink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        return data.isLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStickied() {
        return data.isStickied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMod() {
        if (data.distinguished == null) {
            return false;
        }

        return data.distinguished.equals("moderator");
    }
    /* ------------- End PostableListing ------------- */

    /* ------------- VoteableListing ------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return data.score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScoreHidden() {
        return data.scoreHidden;
    }

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return If upvoted, VoteType.Upvote. If downvoted VoteType.Downvote
     */
    public VoteType getVoteType() {
        if (data.liked == null) {
            return VoteType.NO_VOTE;
        }

        return (data.liked ? VoteType.UPVOTE : VoteType.DOWNVOTE);
    }

    /**
     * @param voteType The vote type for this post for the current user
     */
    public void setVoteType(VoteType voteType) {
        // Update the internal data as that is used in getVoteType
        switch (voteType) {
            case UPVOTE:
                data.liked = true;
                break;
            case DOWNVOTE:
                data.liked = false;
                break;
            case NO_VOTE:
                data.liked = null;
                break;
        }
    }
    /* ------------- End VoteableListing ------------- */
    /* --------------------- End inherited --------------------- */


    /**
     * @return The title of the post
     */
    public String getTitle() {
        return data.title;
    }

    /**
     * @return The amount of comments the post has
     */
    public int getAmountOfComments() {
        return data.amountOfComments;
    }

    /**
     * @return The URL to the thumbnail of the post
     */
    public String getThumbnail() {
        return data.thumbnail;
    }

    /**
     * @return True if the post is marked as a spoiler
     */
    public boolean isSpoiler() {
        return data.spoiler;
    }

    /**
     * @return The HTML of the text of the post if the post is {@link PostType#TEXT}
     */
    public String getSelftextHTML() {
        return data.selftextHtml;
    }

    /**
     * @return The URL to the video of the post if the post is {@link PostType#VIDEO}
     * or {@link PostType#RICH_VIDEO}
     */
    public String getVideoUrl() {
        // If video not hosted by reddit
        if (data.media == null) {
            return getURL();
        }
        return data.media.redditVideo.url;
    }

    /**
     * Retrieves the height of the video
     *
     * @return The height of the video, or -1 if there is no video
     */
    public int getVideoHeight() {
        if (data.media == null) {
            return -1;
        }

        return data.media.redditVideo.height;
    }

    /**
     * Retrieves the width of the video
     *
     * @return The width of the video, or -1 if there is no video
     */public int getVideoWidth() {
        if (data.media == null) {
            return -1;
        }

        return data.media.redditVideo.width;
    }



    /**
     * @return The type of post (image, video, text, or link)
     */
    public PostType getPostType() {
        // TODO make this less bad
        // TODO reddit galleries (multiple images)
        if (data.isVideo) {
            return PostType.VIDEO;
        } else if (data.isText) {
            return PostType.TEXT;
        }

        String hint = data.postHint;

        // Text posts don't have a hint
        if (hint == null) {
            return PostType.TEXT;
        }

        if (hint.equals("link")) {
            // Link posts might be images not uploaded to reddit
            if (getURL().matches("(.png|.jpeg|.jpg)$")) {
                return PostType.IMAGE;
            } else if (getURL().endsWith(".gifv REMOVE THIS LATER")) {
                // TODO load gifs somehow (change to PostType.GIF maybe)
                return PostType.VIDEO;
            }

            return PostType.LINK;
        }

        switch (hint) {
            case "image":
                // .gif is treated as image
                if (data.url.endsWith(".gif")) {
                    return PostType.VIDEO;
                }

                return PostType.IMAGE;

            case "hosted:video":
                return PostType.VIDEO;

            case "rich:video":
                return PostType.RICH_VIDEO;

            // No hint means it's a text post
            default:
                return PostType.TEXT;
        }
    }
}
