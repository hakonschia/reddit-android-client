package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.example.hakonsreader.api.persistence.PostConverter
import com.google.gson.annotations.SerializedName

@Entity(tableName = "posts")
@TypeConverters(PostConverter::class)
class RedditPostKt : RedditListingKt() {

    /**
     * The title of the post
     */
    @SerializedName("title")
    var title = ""

    /**
     * The name of the Subreddit the post is in
     */
    @SerializedName("subreddit")
    var subreddit = ""

    /**
     * The text of the post in Markdown if this is a text post
     *
     * @see selftextHtml
     */
    @SerializedName("selftext")
    var selftext = ""

    /**
     * The text of the post in HTML if this is a text post
     *
     * @see selftext
     */
    @SerializedName("selftext_html")
    var selftextHtml = ""


    /**
     * The amount of comments the post has
     *
     * Note: The amount of actual visible comments might differ from this value, as
     * Reddit might hide comments/comments have been removed by moderators, but these comments
     * are still counted in this value
     */
    @SerializedName("num_comments")
    var amountOfComments = 0


    /**
     * True if the post is marked as a spoiler
     */
    @SerializedName("spoiler")
    var isSpoiler = false

    /**
     * True if the post is marked as a Not Safe For Work (18+)
     */
    @SerializedName("over_18")
    var isNsfw = false

    /**
     * True if the post is archived
     *
     * Archived posts cannot be voted/commented on
     */
    @SerializedName("archived")
    var isArchived = false

    /**
     * True if the currently logged in user has saved the post
     */
    @SerializedName("saved")
    var isSaved = false

    /**
     * True if the currently logged in user is a mod in the subreddit the post is in
     */
    @SerializedName("can_mod_post")
    var isUserMod = false


    /**
     * True if the post is a self post (text post)
     */
    @SerializedName("is_self")
    private var isSelf = false

    /**
     * True if the post is a video post
     */
    @SerializedName("is_video")
    private var isVideo = false

    /**
     * True if the post is a gallery post
     */
    @SerializedName("is_gallery")
    private var isGallery = false


    /**
     * The hex color of the background of the authors flair
     *
     * This might be the string "transparent" for transparent backgrounds
     */
    @SerializedName("author_flair_background_color")
    var authorFlairBackgroundColor = ""

    /**
     * The text color for the authors flair
     */
    @SerializedName("author_flair_text_color")
    var authorFlairTextColor = ""

    /**
     * The text for the authors flair
     */
    @SerializedName("author_flair_text")
    var authorFlairText = ""

    /**
     * The list of [RichtextFlair] the authors flair is combined of
     */
    @SerializedName("author_flair_richtext")
    var authorRichtextFlairs = ArrayList<RichtextFlair>()

    /**
     * The hex color of the background of the authors flair
     *
     * This might be the string "transparent" for transparent backgrounds
     */
    @SerializedName("link_flair_background_color")
    var linkFlairBackgroundColor = ""

    /**
     * The text color for the links flair
     */
    @SerializedName("link_flair_text_color")
    var linkFlairTextColor = ""

    /**
     * The text for the links flair
     */
    @SerializedName("link_flair_text")
    var linkFlairText = ""

    /**
     * The list of [RichtextFlair] the links flair is combined of
     */
    @SerializedName("link_flair_richtext")
    var linkRichtextFlairs = ArrayList<RichtextFlair>()
}