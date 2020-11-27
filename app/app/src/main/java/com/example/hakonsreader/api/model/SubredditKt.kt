package com.example.hakonsreader.api.model

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

/**
 * Class representing a Subreddit
 *
 * This class has local database support with Room
 */
@Entity(tableName = "subreddits")
class SubredditKt : RedditListingKt() {

    /**
     * The name of the Subreddit
     */
    @SerializedName("display_name")
    var name = ""

    /**
     * The title of the subreddit
     */
    @SerializedName("title")
    var title = ""

    /**
     * The amount of subscribers the user has
     */
    @SerializedName("subscribers")
    var subscribers = 0


    /**
     * The description for the Subreddit in Markdown
     *
     * @see descriptionHtml
     */
    @SerializedName("description")
    var description = ""

    /**
     * The description for the Subreddit in HTML
     *
     * @see description
     */
    @SerializedName("description_html")
    var descriptionHtml = ""

    /**
     * The public description of the Subreddit in Markdown.
     *
     * This is typically a short summary of the Subreddit, for the full description see [description]
     */
    @SerializedName("public_description")
    var publicDescription = ""

    /**
     * The public description of the subreddit in HTML
     *
     * This is typically a short summary of the Subreddit, for the full description see [descriptionHtml]
     */
    @SerializedName("public_description_html")
    var publicDescriptionHtml = ""


    /**
     * The URL pointing to the subreddits icon image
     */
    @SerializedName("icon_img")
    var icon = ""

    /**
     * The URL pointing to the subreddits community icon
     */
    @SerializedName("community_icon")
    var communityIcon = ""

    /**
     * The URL pointing to the subreddits header image
     */
    @SerializedName("header_img")
    var headerImage = ""

    /**
     * The URL pointing to the subreddits banner background image
     */
    @SerializedName("banner_background_image")
    var bannerBackgroundImage = ""

    /**
     * A hex representation of the primary color of the Subreddit
     */
    @SerializedName("primary_color")
    var primaryColor = ""


    /**
     * True if the Subreddit is quarantined
     */
    @SerializedName("quarantine")
    var isQuarantined = false

    /**
     * True if the Subreddit is marked as Not Safe For Work (18+)
     */
    @SerializedName("over18")
    var isNsfw = false


    /**
     * True if video submissions are allowed to the Subreddit
     */
    @SerializedName("allow_videos")
    var allowVideos = false

    /**
     * True if gifs submissions are allowed to the Subreddit
     */
    @SerializedName("allow_videogifs")
    var allowGifs = false

    /**
     * True if poll submissions are allowed to the Subreddit
     */
    @SerializedName("allow_polls")
    var allowPolls = false

    /**
     * True if image submissions are allowed to the Subreddit
     */
    @SerializedName("allow_image")
    var allowImages = false


    /**
     * True if the currently logged in user has subscribed to the subreddit
     */
    @SerializedName("user_is_subscriber")
    var userHasSubscribed = false

    /**
     * True if the currently logged in user has favorited the Subreddit. This being true
     * implies that [userHasSubscribed] is also true
     */
    @SerializedName("user_has_favorited")
    var userHasFavorited = false

    /**
     * True if the currently logged in user is a moderator of the Subreddit
     */
    @SerializedName("user_is_moderator")
    var userIsModerator = false


    /**
     * The text in Markdown that should be displayed on a submit page for the Subreddit
     *
     * @see submitTextHtml
     */
    @SerializedName("submit_text")
    var submitText = ""

    /**
     * The text in HTML that should be displayed on a submit page for the Subreddit
     *
     * @see submitText
     */
    @SerializedName("submit_text_html")
    // TODO this can be null in the response, check if this should be String? instead
    var submitTextHtml = ""

    /**
     * The Subreddit type (eg. "user" for user subreddits)
     */
    @SerializedName("subreddit_type")
    var subredditType = ""
}