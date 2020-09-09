package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.jsonadapters.BooleanPrimitiveAdapter;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;


/**
 * Represents a Reddit Listing
 *
 * Serves as a base class for the specific listing types, and holds common attributes
 */
public class RedditListing {

    /**
     * Create a subclass object from this base listing
     *
     * @param type The type of object to create
     * @param <T> A subclass of {@link RedditListing} to create as
     * @return A post object with the values from {@code base}
     */
    protected <T extends RedditListing> T createFromListing(Class<T> type) {
        // TODO if there isn't a better way to do this idek
        Gson gson = new Gson();

        // Let gson handle the conversion for us
        String json = gson.toJson(this);
        return gson.fromJson(json, type);
    }

    // What kind of listing it is
    protected String kind;

    public String getKind() {
        return kind;
    }

    // The JSON structure of a post has an internal object called "data"
    protected Data data;

    protected static class Data {
        // Which subreddit the post is in
        protected String subreddit;

        // The ID of the post
        protected String id;

        // The title of the post
        protected String title;

        // The author of the psot
        protected String author;

        // The score of the post
        protected int score;

        // The URL of the post. For images it links to the picture, for link posts it's the link
        protected String url;

        // The full link to the comments
        protected String permalink;


        // Show spoiler tag?
        protected boolean spoiler;

        // Is the post locked?
        protected boolean locked;

        protected String thumbnail;


        // Is the post NSFW?
        @SerializedName("over_18")
        protected boolean nsfw;


        // The UTC unix timestamp the post was created at
        @SerializedName("created_utc")
        protected float createdAt;

        // Is the post a self post (ie. text post)
        @SerializedName("is_self")
        protected boolean isText;

        // Is the post a video?
        @SerializedName("is_video")
        protected boolean isVideo;

        // The amount of comments the post has
        @SerializedName("num_comments")
        protected int amountOfComments;

        @SerializedName("post_hint")
        protected String postHint;

        @SerializedName("likes")
        @JsonAdapter(BooleanPrimitiveAdapter.class)
        protected Boolean liked;


        protected Media media;

        // For video posts
        static class Media {

            @SerializedName("reddit_video")
            protected RedditVideo redditVideo;

            protected static class RedditVideo {
                protected int duration;

                @SerializedName("scrubber_media_url")
                protected String url;
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

    public String getThumbnail() {
        return data.thumbnail;
    }

    /**
     * @return The unix timestamp in UTC when the post was created
     */
    public long getCreatedAt() {
        return (long)data.createdAt;
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
