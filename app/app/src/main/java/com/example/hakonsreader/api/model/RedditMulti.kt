package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Class representing a Multi (custom combination of subreddits) from a user
 */
class RedditMulti {

    /**
     * The username of the owner of the Multi
     */
    @SerializedName("owner")
    var owner = ""

    /**
     * The fullname of the owner of the Multi
     */
    @SerializedName("owner_id")
    var ownerFullname = ""

    /**
     * The name of the Multi
     */
    @SerializedName("name")
    var name = ""

    /**
     * The display name of the Multi
     */
    @SerializedName("display_name")
    var displayName = ""

    /**
     * The URL pointing to the icon of the Multi
     */
    @SerializedName("icon_url")
    var iconUrl = ""

    /**
     * The Markdown description of the Multi
     */
    @SerializedName("description_md")
    var description = ""

    /**
     * The HTML description of the Multi
     */
    @SerializedName("description_html")
    var descriptionHtml = ""

    /**
     * The amount of subscribers the Multi has
     */
    @SerializedName("num_subscribers")
    var subscribers = 0

    /**
     * True if the currently logged in user is subscribed to the Multi
     */
    @SerializedName("is_subscribed")
    var isSubscribed = false

    /**
     * True if the currently logged in user has favorited the Multi
     */
    @SerializedName("is_favorited")
    var isFavorited = false

    /**
     * True if the currently logged in user is allowed to edit the Multi
     */
    @SerializedName("can_edit")
    var canEdit = false

    /**
     * True if the Multi is marked as NSFW (18+)
     */
    @SerializedName("over_18")
    var isNsfw = false

    /**
     * The path to the Multi, eg. "/user/hakonschia/m/<multi_name>"
     */
    @SerializedName("path")
    var path = ""

    /**
     * The visibility of the Multi ("private" or "public")
     */
    @SerializedName("visibility")
    var visiblity = ""

    /**
     * The timestamp the Multi was created
     */
    @SerializedName("created")
    var created = 0L

    /**
     * The UTC timestamp the Multi was created
     */
    @SerializedName("created_utc")
    var createdUtc = 0L

    /**
     * The key color of the Multi
     */
    @SerializedName("key_color")
    var keyColor: String? = null

    /**
     * Where the Multi was copied from (or null if it wasn't copied)
     */
    @SerializedName("copied_from")
    var copiedFrom: String? = null

    /**
     * The subreddits in the Multi
     *
     * This is a list of [Any], and will either be a list of [String] where the elements are the subreddit names,
     * or a list of [Subreddit] objects
     */
    var subreddits: List<Any> = emptyList()
}