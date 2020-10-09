package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;


/**
 * Class representing a subreddit
 */
public class Subreddit extends RedditListing {

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

    @SerializedName("user_has_favorited")
    private boolean userHasFavorited;

    @SerializedName("submit_text")
    private String submitText;

    @SerializedName("subreddit_type")
    private String subredditType;


    /**
     * @return The name of the subreddit
     */
    public String getName() {
        return name;
    }

    /**
     * @return The title (short description) of the subreddit
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The amount of subscribers the subreddit has
     */
    public int getSubscribers() {
        return subscribers;
    }

    /**
     * For HTML see {@link Subreddit#getDescriptionHTML()}
     *
     * @return The markdown text of the description of the subreddit
     */
    public String getDescription() {
        return description;
    }

    /**
     * For markdown see {@link Subreddit#getDescription()}
     *
     * @return The HTML text of the description of the subreddit
     */
    public String getDescriptionHTML() {
        return descriptionHTML;
    }

    /**
     * @return The URL to the subreddit's icon
     */
    public String getIconImage() {
        return iconImage;
    }

    /**
     * @return The URL to the subreddit's header image
     */
    public String getHeaderImage() {
        return headerImage;
    }

    /**
     * @return True if the subreddit is quarantined
     */
    public boolean isQuarantined() {
        return quarantine;
    }

    /**
     * @return True if the currently logged in user has favorited the subreddit
     */
    public boolean userHasFavorited() {
        return userHasFavorited;
    }

    /**
     * @return The text to display when submitting a post to the subreddit
     */
    public String getSubmitText() {
        return submitText;
    }

    /**
     * @return The type of subreddit this is. For user subreddit this is "user"
     */
    public String getSubredditType() {
        return subredditType;
    }
}
