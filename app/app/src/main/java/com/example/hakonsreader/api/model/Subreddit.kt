package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.example.hakonsreader.api.persistence.PostConverter
import com.google.gson.annotations.SerializedName

/**
 * Class representing a Subreddit
 *
 * This class has local database support with Room
 */
@Entity(tableName = "subreddits")
@TypeConverters(PostConverter::class)
class Subreddit : RedditListing() {

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
     * The amount of active users at the time the information was retrieved from the API
     */
    @SerializedName("accounts_active")
    var activeUsers = 0


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
    var descriptionHtml: String? = null

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
    var publicDescriptionHtml: String? = null


    /**
     * The URL pointing to the subreddits icon image
     */
    @SerializedName("icon_img")
    var icon: String? = null

    /**
     * The URL pointing to the subreddits community icon
     */
    @SerializedName("community_icon")
    var communityIcon = ""

    /**
     * The URL pointing to the subreddits header image
     */
    @SerializedName("header_img")
    var headerImage: String? = null

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
     * A hex representation of the key color of the Subreddit
     */
    @SerializedName("key_color")
    var keyColor = ""

    @SerializedName("banner_background_color")
    var bannerBackgroundColor = ""

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
    var isSubscribed = false

    /**
     * True if the currently logged in user has favorited the Subreddit. This being true
     * implies that [isSubscribed] is also true
     */
    @SerializedName("user_has_favorited")
    var isFavorited = false

    /**
     * True if the currently logged in user is a moderator of the Subreddit
     */
    @SerializedName("user_is_moderator")
    var isModerator = false

    /**
     * True if the wiki on the subreddit is enabled
     */
    @SerializedName("wiki_enabled")
    var wikiEnabled = false

    @SerializedName("show_media")
    var showMedia = true



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
    var submitTextHtml: String? = null

    /**
     * The Subreddit type (eg. "user" for user subreddits)
     */
    @SerializedName("subreddit_type")
    var subredditType = ""

    /**
     * The amount of minutes posts should have their score hidden on this subreddit
     */
    @SerializedName("comment_score_hide_mins")
    var hideScoreTime = 0


    /**
     * True if users can assign flairs on the subreddit
     */
    @SerializedName("can_assign_user_flair")
    var canAssignUserFlair = false

    /**
     * The text color for the users flair (if there is a logged in user with a flair)
     */
    @SerializedName("user_flair_text_color")
    var userFlairTextColor: String? = null

    /**
     * The background color of the user flair (if there is a logged in user with a flair)
     */
    @SerializedName("user_flair_background_color")
    var userFlairBackgroundColor: String? = null

    /**
     * The list of [RichtextFlair] the users flair is combined of (if there is a logged in user with a flair)
     */
    @SerializedName("user_flair_richtext")
    var userFlairRichText: ArrayList<RichtextFlair>? = null

    /**
     * The raw text of the user flair (if there is a logged in user with a flair)
     */
    @SerializedName("user_flair_text")
    var userFlairText: String? = null

    /**
     * The ID of the user flair, if set
     */
    @SerializedName("user_flair_template_id")
    var userFlairTemplateId: String? = null
}