package com.example.hakonsreader.api.model;

import androidx.room.Entity;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * Class representing a subreddit
 */
@Entity(tableName = "subreddits")
public class Subreddit extends RedditListing {

    /**
     * Create a basic object with only the name of the subreddit set
     *
     * @param name The subreddit name
     */
    public Subreddit(String name) {
        this.name = name;
    }

    @SerializedName("display_name")
    private String name;

    @SerializedName("title")
    private String title;

    @SerializedName("subscribers")
    private int subscribers;

    @SerializedName("description")
    private String description;

    @SerializedName("description_html")
    private String descriptionHtml;

    @SerializedName("public_description")
    private String publicDesription;

    @SerializedName("public_description_html")
    private String publicDesriptionHtml;

    @SerializedName("icon_img")
    private String iconImage;

    @SerializedName("community_icon")
    private String communityIcon;

    @SerializedName("header_img")
    private String headerImage;

    @SerializedName("banner_background_image")
    private String bannerImage;

    @SerializedName("quarantine")
    private boolean quarantine;

    @SerializedName("user_has_favorited")
    private boolean favorited;

    @SerializedName("user_is_subscriber")
    private boolean subscribed;

    @SerializedName("submit_text")
    private String submitText;

    @SerializedName("subreddit_type")
    private String subredditType;

    @SerializedName("comment_score_hide_mins")
    private int hideScoreTime;


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
     * For HTML see {@link Subreddit#getDescriptionHtml()}
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
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    /**
     * @return The public description (this is typically a shorter summary)
     */
    public String getPublicDesription() {
        return publicDesription;
    }

    /**
     * @return The public description in HTML (this is typically a shorter summary)
     */
    public String getPublicDesriptionHtml() {
        return publicDesriptionHtml;
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
     * @return The URL to the subreddits banner background image
     */
    public String getBannerImage() {
        return bannerImage;
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
    public boolean isFavorited() {
        return favorited;
    }

    /**
     * @return True if the currently logged in user is subscribed to the subreddit
     */
    public boolean isSubscribed() {
        return subscribed;
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

    /**
     * @return The amount of minutes scores should be hidden for in this subreddit
     */
    public int getHideScoreTime() {
        return hideScoreTime;
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

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public void setPublicDesription(String publicDesription) {
        this.publicDesription = publicDesription;
    }

    public void setPublicDesriptionHtml(String publicDesriptionHtml) {
        this.publicDesriptionHtml = publicDesriptionHtml;
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

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public void setSubmitText(String submitText) {
        this.submitText = submitText;
    }

    public void setSubredditType(String subredditType) {
        this.subredditType = subredditType;
    }

    public void setHideScoreTime(int hideScoreTime) {
        this.hideScoreTime = hideScoreTime;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subreddit)) return false;
        Subreddit subreddit = (Subreddit) o;
        return subscribers == subreddit.subscribers &&
                quarantine == subreddit.quarantine &&
                favorited == subreddit.favorited &&
                subscribed == subreddit.subscribed &&
                hideScoreTime == subreddit.hideScoreTime &&
                name.equals(subreddit.name) &&
                title.equals(subreddit.title) &&
                Objects.equals(description, subreddit.description) &&
                Objects.equals(descriptionHtml, subreddit.descriptionHtml) &&
                Objects.equals(publicDesription, subreddit.publicDesription) &&
                Objects.equals(publicDesriptionHtml, subreddit.publicDesriptionHtml) &&
                Objects.equals(iconImage, subreddit.iconImage) &&
                Objects.equals(communityIcon, subreddit.communityIcon) &&
                Objects.equals(headerImage, subreddit.headerImage) &&
                Objects.equals(submitText, subreddit.submitText) &&
                Objects.equals(subredditType, subreddit.subredditType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, title, subscribers, description, descriptionHtml, publicDesription, publicDesriptionHtml, iconImage, communityIcon, headerImage, quarantine, favorited, subscribed, submitText, subredditType, hideScoreTime);
    }
}
