package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.interfaces.*
import com.example.hakonsreader.api.jsonadapters.BooleanAsIntAdapter
import com.example.hakonsreader.api.jsonadapters.NullAsIntAdapter
import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.example.hakonsreader.api.persistence.PostConverter
import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap


@Entity(tableName = "posts")
@TypeConverters(PostConverter::class)
class RedditPost : RedditListing(), VoteableListing, ReplyableListing, ReportableListing, AwardableListing {

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
     * The author of the post
     */
    @SerializedName("author")
    override var author = ""

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
    // The HTML selftext is nullable, but selftext will just be empty if no selftext
    var selftextHtml: String? = null


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
    override var isArchived = false

    /**
     * True if the post is locked
     *
     * Locked posts cannot be commented on
     */
    @SerializedName("locked")
    var isLocked = false

    /**
     * True if the post is stickied
     */
    @SerializedName("stickied")
    var isStickied = false

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
    var isSelf = false

    /**
     * Don't use this directly, use [getPostType]
     *
     * True if the post is a video post
     */
    @SerializedName("is_video")
    var isVideo = false

    /**
     * Don't use this directly, use [getPostType]
     *
     * True if the post is a gallery post
     */
    @SerializedName("is_gallery")
    var isGallery = false


    /**
     * The permalink to the post
     */
    @SerializedName("permalink")
    var permalink = ""

    /**
     * The URL for the content of the post
     */
    @SerializedName("url")
    var url = ""

    /**
     * How the comment is distinguished
     *
     * @see isMod
     * @see isAdmin
     */
    @SerializedName("distinguished")
    var distinguished: String? = null

    /**
     * @return True if the post is made by, and distinguished as, a moderator
     * @see distinguished
     */
    fun isMod() : Boolean = distinguished == "moderator"

    /**
     * @return True if the post is made by, and distinguished as, an admin (Reddit employee)
     * @see distinguished
     */
    fun isAdmin() : Boolean = distinguished == "admin"


    /**
     * The domain the post is posted in (eg. "imgur.com" or "self.GlobalOffensive")
     */
    @SerializedName("domain")
    var domain = ""

    /**
     * For when the post has been removed, this says which category (mod, author etc.) removed the post
     */
    @SerializedName("removed_by_category")
    var removedByCategory: String? = null

    /**
     * Don't use this directly, use [getPostType]
     *
     * The hint for what kind of post this is
     */
    @SerializedName("post_hint")
    var postHint = ""

    /**
     * The hex color of the background of the authors flair
     *
     * This might be the string "transparent" for transparent backgrounds
     */
    @SerializedName("author_flair_background_color")
    var authorFlairBackgroundColor: String? = null

    /**
     * The text color for the authors flair
     */
    @SerializedName("author_flair_text_color")
    var authorFlairTextColor: String? = null

    /**
     * The text for the authors flair
     */
    @SerializedName("author_flair_text")
    var authorFlairText: String? = null

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
    var linkFlairBackgroundColor: String? = null

    /**
     * The text color for the links flair
     */
    @SerializedName("link_flair_text_color")
    var linkFlairTextColor: String? = null

    /**
     * The text for the links flair
     */
    @SerializedName("link_flair_text")
    var linkFlairText: String? = null

    /**
     * The list of [RichtextFlair] the links flair is combined of
     */
    @SerializedName("link_flair_richtext")
    var linkRichtextFlairs = ArrayList<RichtextFlair>()


    /**
     * The ID of the post this is a crosspost of
     */
    @SerializedName("crosspost_parent")
    var crosspostParentId: String? = null

    /**
     * The list of crossposts
     */
    @SerializedName("crosspost_parent_list")
    @Ignore
    var crossposts: List<RedditPost>? = null

    /**
     * The list of crosspost IDs (must be set manually)
     */
    var crosspostIds: List<String>? = null


    /**
     * The thumbnail for the post (for selfposts this will be "self")
     */
    @SerializedName("thumbnail")
    var thumbnail = ""

    @SerializedName("media")
    var media: Media? = null

    /**
     * Data for video posts
     */
    class Media {
        @SerializedName("reddit_video")
        val redditVideo: RedditVideo? = null
    }


    /**
     * Don't use this directly, use [galleryImages]
     *
     * This holds the data for gallery items. The objects in this are either strings or other
     * [LinkedTreeMap]. The source image is found in a [LinkedTreeMap] called "s"
     */
    @SerializedName("media_metadata")
    var mediaMetadata: LinkedTreeMap<String, Any>? = null

    /**
     * Don't use this directly, use [galleryImages]
     *
     * Internal gallery data
     */
    @SerializedName("gallery_data")
    var galleryData: GalleryDataOuter? = null

    var galleryImages: MutableList<Image>? = null
        get() {
            // Return the field directly if it already has been created
            if (field != null) {
                return field
            }

            mediaMetadata?.let { meta ->
                field = java.util.ArrayList(meta.size)

                val gson = Gson()

                galleryData?.data?.forEach {
                    val metaData = meta[it.mediaId]

                    val converted = metaData as LinkedTreeMap<String, Any>
                    // TODO this always gets the source, we should redo this so a GalleryItem
                    //  class exposes all the possible resolutions (which is basically a wrapper for a list of Image)
                    val asJson = gson.toJson(converted["s"])

                    val image = gson.fromJson(asJson, Image::class.java)

                    image.caption = it.caption
                    image.outboundUrl = it.outboundUrl

                    (field as java.util.ArrayList<Image>).add(image)
                }
            }

            return field
        }

    class GalleryDataOuter {
        @SerializedName("items")
        var data: List<GalleryData>? = null
    }

    class GalleryData {
        @SerializedName("media_id")
        var mediaId = ""

        @SerializedName("id")
        var id = 0

        @SerializedName("caption")
        var caption: String? = null

        @SerializedName("outbound_url")
        var outboundUrl: String? = null
    }

    @SerializedName("preview")
    var preview: Preview? = null

    class Preview {
        @SerializedName("images")
        var images: List<ImagesWrapper>? = null

        /**
         * For gifs uploaded to gfycat this object holds an object pointing to the DASH url for the video
         */
        @SerializedName("reddit_video_preview")
        val videoPreview: RedditVideo? = null
    }

    fun getVideo() : RedditVideo? {
        return media?.redditVideo
    }

    fun getSourcePreview() : Image? {
        return preview?.images?.get(0)?.source
    }

    fun getPreviewImages() : List<Image>? {
        return preview?.images?.get(0)?.resolutions
    }

    fun getObfuscatedPreviewImages() : List<Image>? {
        return preview?.images?.get(0)?.variants?.obfuscated?.resolutions
    }

    /**
     * Retrieves the video for GIF posts from sources such as Gfycat
     *
     *
     * Note: not all GIFs will be found here. Some GIFs will be returned as a [Image]
     * with a link to the external URL. See [RedditPost.getMp4Source] and [RedditPost.getMp4Previews]
     *
     * @return The [RedditVideo] holding the data for GIF posts
     */
    fun getVideoGif(): RedditVideo? {
        return preview?.videoPreview
    }

    /**
     * Gets the MP4 source for the post. Note that this is only set when the post is a GIF
     * uploaded to reddit directly.
     *
     *
     * Note: Some GIF posts will be as a separate [RedditVideo] that is retrieved with
     * [RedditPost.getVideoGif]
     *
     * For a list of other resolutions see [RedditPost.getMp4Previews]
     *
     * @return The source MP4
     */
    fun getMp4Source(): Image? {
        return preview?.images?.get(0)?.variants?.mp4?.source
    }

    /**
     * Gets the list of MP4 resolutions for the post. Note that this is only set when the post is a GIF
     * uploaded to reddit directly.
     *
     *
     * For the source resolution see [RedditPost.getMp4Source]
     *
     * @return The list of MP4 resolutions
     */
    fun getMp4Previews(): List<Image?>? {
        return preview?.images?.get(0)?.variants?.mp4?.resolutions
    }


    /**
     * The score of the post
     */
    @SerializedName("score")
    override var score = 0

    /**
     * True if the score should be hidden
     */
    @SerializedName("score_hidden")
    override var isScoreHidden = false

    /**
     * The upvote ratio of the post (0-1)
     */
    @SerializedName("upvote_ratio")
    var upvoteRatio = 0f

    /**
     * Don't use this directly, use [voteType]
     */
    @SerializedName("likes")
    var liked: Boolean? = null

    /**
     * The user reports on the post.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    @SerializedName("user_reports")
    override var userReports: Array<Array<Any>>? = null

    /**
     * The dismissed user reports on the post.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    @SerializedName("user_reports_dismissed")
    override var userReportsDismissed: Array<Array<Any>>? = null

    /**
     * The amount of reports the post has
     */
    @JsonAdapter(NullAsIntAdapter::class)
    @SerializedName("num_reports")
    override var numReports = 0

    /**
     * True if reports are set to be ignored on the post
     */
    @SerializedName("ignore_reports")
    override var ignoreReports = false

    /**
     * The timestamp the post was edited. If this is negative the post hasn't been edited
     */
    @JsonAdapter(BooleanAsIntAdapter::class)
    @SerializedName("edited")
    var edited = -1


    @SerializedName("all_awardings")
    override var awardings: List<RedditAward>? = null


    /**
     * If the post has loaded information from a third party API, the data will be set on this variable
     */
    var thirdPartyObject: Any? = null

    /**
     * The vote type the post has
     *
     * Setting this value will automatically update [score], and is idempotent
     */
    override var voteType: VoteType
        get() {
            return when (liked) {
                true -> VoteType.UPVOTE
                false -> VoteType.DOWNVOTE
                null -> VoteType.NO_VOTE
            }
        }
        set(value) {
            // Don't do anything if there is no update to the vote
            if (value == voteType) {
                return
            }

            // Going from upvote to downvote: -1 - 1 = -2
            // Going from downvote to upvote: 1 - (-1) = 2
            // Going from downvote to no vote: 0 - (-1) = 1

            // Going from upvote to downvote: -1 - 1 = -2
            // Going from downvote to upvote: 1 - (-1) = 2
            // Going from downvote to no vote: 0 - (-1) = 1
            val difference: Int = value.value - voteType.value

            score += difference

            // Update the internal data as that is used in getVoteType
            liked = when (value) {
                VoteType.UPVOTE -> true
                VoteType.DOWNVOTE -> false
                VoteType.NO_VOTE -> null
            }
        }


    /**
     * @return The type of post (image, video, text, or link)
     */
    fun getPostType(): PostType? {
        // TODO make this less bad
        if (isVideo || thirdPartyObject is ThirdPartyGif) {
            return PostType.VIDEO
        } else if (isSelf) {
            return PostType.TEXT
        } else if (isGallery || !galleryImages.isNullOrEmpty()) {
            // If the gallery images are set from imgurRequest isGallery would be false
            return PostType.GALLERY
        }

        // Usually no hint means it's a text post, but sometimes it means it's a link post
        // If the url for the post isn't to reddit, it's a link post (these link posts don't have a thumbnail for some reason)
        if (postHint.isBlank()) {
            return if (url.matches("https://www\\.reddit\\.com".toRegex())) {
                PostType.TEXT
            } else {
                PostType.LINK
            }
        }
        if (postHint == "link") {
            // If link matches "imgur.com/...", add a .png to the end and it will redirect to the direct link

            // If we have a link post that is a link to imgur, redirect it to get the image directly
            // The IDs are (as far as I know) alphanumerical (upper and lowercase) and 0-9 with 5 or 7 digits
            // (it's currently between 5 and 7 characters but it works good enough I guess)
            if (url.matches("^https://imgur.com/[A-Za-z0-9]{5,7}$".toRegex())) {
                // Technically the URL needs to be "i.imgur.com/...", but imgur does the redirection for us
                // Which suffix is added (.png or .jpg or .jpeg) seemingly doesn't matter
                url += ".png"
            }

            // Link posts might be images not uploaded to reddit
            // Technically this doesn't match just URLs as anything can precede the .png/.jpg/.jpeg
            // but that shouldn't really matter
            if (url.matches(".+(.png|.jpeg|.jpg)$".toRegex()) || domain.matches("prnt.sc".toRegex())) {
                return PostType.IMAGE
            }  else if (url.endsWith(".gifv") || domain.matches("giphy.com".toRegex())) {
                return PostType.GIF
            }
            return PostType.LINK
        }
        return when (postHint) {
            "image" -> {
                if (url.endsWith(".gif")) {
                    PostType.GIF
                } else PostType.IMAGE
            }
            "hosted:video" -> PostType.VIDEO
            "rich:video" -> PostType.RICH_VIDEO
            else -> null
        }
    }
}