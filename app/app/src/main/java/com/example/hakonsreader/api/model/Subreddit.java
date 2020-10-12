package com.example.hakonsreader.api.model;

import androidx.room.Entity;
import androidx.room.Index;

import com.google.gson.annotations.SerializedName;


/**
 * Class representing a subreddit
 */
@Entity(tableName = "subreddits")
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

    @SerializedName("community_icon")
    private String communityIcon;

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
     * Retrieve the icon image URL.
     *
     * <p>See also: {@link Subreddit#getCommunityIcon()}</p>
     *
     * @return The URL to the subreddit's icon
     */
    public String getIconImage() {
        return iconImage;
    }

    /**
     * Retrieve the community icon URL.
     *
     * <p>See also: {@link Subreddit#getIconImage()}</p>
     *
     * @return The URL to the subreddits community icon
     */
    public String getCommunityIcon() {
        return communityIcon;
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
    public boolean isQuarantine() {
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





    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubscribers(int subscribers) {
        this.subscribers = subscribers;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDescriptionHTML(String descriptionHTML) {
        this.descriptionHTML = descriptionHTML;
    }

    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }

    public void setCommunityIcon(String communityIcon) {
        this.communityIcon = communityIcon;
    }

    public void setHeaderImage(String headerImage) {
        this.headerImage = headerImage;
    }

    public void setQuarantine(boolean quarantine) {
        this.quarantine = quarantine;
    }

    public void setUserHasFavorited(boolean userHasFavorited) {
        this.userHasFavorited = userHasFavorited;
    }

    public void setSubmitText(String submitText) {
        this.submitText = submitText;
    }

    public void setSubredditType(String subredditType) {
        this.subredditType = subredditType;
    }
}
