package com.example.hakonsreader.api.model;

import com.example.hakonsreader.api.interfaces.RedditListing;
import com.google.gson.annotations.SerializedName;

public class Subreddit implements RedditListing {

    @SerializedName("kind")
    private String kind;

    @SerializedName("data")
    public Data data;

    /**
     * Subreddit specific data
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

        @SerializedName("over18")
        private boolean nsfw;
        /* ------------- End RedditListing ------------- */

        @SerializedName("display_name")
        private String name;

        @SerializedName("title")
        private String title;

        @SerializedName("subscribers")
        private int subscribers;

        @SerializedName("description")
        private String description;

        @SerializedName("description_html")
        private String descriptionHTML;

        @SerializedName("icon_img")
        private String iconImage;

        @SerializedName("header_img")
        private String headerImage;

        @SerializedName("quarantine")
        private boolean quarantine;

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
    /* --------------------- End inherited --------------------- */

    /**
     * @return The name of the subreddit
     */
    public String getName() {
        return data.name;
    }

    /**
     * @return The title (short description) of the subreddit
     */
    public String getTitle() {
        return data.title;
    }

    /**
     * @return The amount of subscribers the subreddit has
     */
    public int getSubscribers() {
        return data.subscribers;
    }

    /**
     * For HTML see {@link Subreddit#getDescriptionHTML()}
     *
     * @return The markdown text of the description of the subreddit
     */
    public String getDescription() {
        return data.description;
    }

    /**
     * For markdown see {@link Subreddit#getDescription()}
     *
     * @return The HTML text of the description of the subreddit
     */
    public String getDescriptionHTML() {
        return data.descriptionHTML;
    }

    /**
     * @return The URL to the subreddit's icon
     */
    public String getIconImage() {
        return data.iconImage;
    }

    /**
     * @return The URL to the subreddit's header image
     */
    public String getHeaderImage() {
        return data.headerImage;
    }

    /**
     * @return True if the subreddit is quarantined
     */
    public boolean isQuarantined() {
        return data.quarantine;
    }

}
