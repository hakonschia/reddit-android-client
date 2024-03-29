package com.example.hakonsreader.api.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.interfaces.*
import com.example.hakonsreader.api.jsonadapters.BooleanAsIntAdapter
import com.example.hakonsreader.api.jsonadapters.NullAsIntAdapter
import com.example.hakonsreader.api.jsonadapters.ThirdPartyObjectAdapter
import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.example.hakonsreader.api.model.images.RedditGalleryItem
import com.example.hakonsreader.api.model.images.RedditImage
import com.example.hakonsreader.api.model.internal.GalleryData
import com.example.hakonsreader.api.model.internal.ImagesWrapper
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurAlbum
import com.example.hakonsreader.api.persistence.PostConverter
import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap


@Entity(tableName = "posts")
@TypeConverters(PostConverter::class)
class RedditPost : RedditListing(),
        VoteableListing,
        ReplyableListing,
        ReportableListing,
        AwardableListing,
        LockableListing,
        DistinguishableListing {

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
    override var isLocked = false

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
    override var distinguished: String? = null


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
    var galleryData: GalleryData? = null

    var galleryImages: List<RedditGalleryItem>? = null
        get() {
            // Return the field directly if it already has been created
            if (field != null) {
                return field
            }

            // JSON structure for galleries:

            /*
            At the top level "gallery_data" is given which is a simple list of what gallery images there
            are, where "media_id" references the ID in the part where the actual data is
            This also holds any potential caption/URL for the item
            "gallery_data": {
                "items": [
                    {
                        "media_id": "..."",
                        "id": 123
                ]
            }

            "media_metadata" is a top level object which itself holds other objects. This is really an
            array which for some reason holds objects with anonymous names. The name of an object is "media_id"
            from the "gallery_data" items
            "media_metadata" {
                "<media_id>": { // The name from the "gallery_data" items
                    // The actual gallery item data
                }
            }
             */

            // The outer object which holds objects
            mediaMetadata?.let { metaData ->
                field = ArrayList(metaData.size)

                val gson = Gson()

                galleryData?.data?.forEach { galleryItem ->
                    val jsonTree = gson.toJsonTree(metaData[galleryItem.mediaId] as LinkedTreeMap<String, Any>)
                    val image = gson.fromJson(jsonTree, RedditGalleryItem::class.java)

                    image.caption = galleryItem.caption
                    image.outboundUrl = galleryItem.outboundUrl

                    (field as ArrayList<RedditGalleryItem>).add(image)
                }
            }

            return field
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

    fun getSourcePreview() : RedditImage? {
        return preview?.images?.get(0)?.source
    }

    fun getPreviewImages() : List<RedditImage>? {
        return preview?.images?.get(0)?.resolutions
    }

    fun getObfuscatedSource() : RedditImage? {
        return preview?.images?.get(0)?.variants?.obfuscated?.source
    }

    /**
     * The list of obfuscated images for the post. This might be empty if the image is a very low resolution
     */
    fun getObfuscatedPreviewImages() : List<RedditImage>? {
        return preview?.images?.get(0)?.variants?.obfuscated?.resolutions
    }

    /**
     * Retrieves the video for GIF posts from sources such as Gfycat
     *
     * Note: not all GIFs will be found here. Some GIFs will be returned as a [RedditImage]
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
     * Note: Some GIF posts will be as a separate [RedditVideo] that is retrieved with
     * [RedditPost.getVideoGif]
     *
     * For a list of other resolutions see [RedditPost.getMp4Previews]
     *
     * @return The source MP4
     */
    fun getMp4Source(): RedditImage? {
        return preview?.images?.get(0)?.variants?.mp4?.source
    }

    /**
     * Gets the list of MP4 resolutions for the post. Note that this is only set when the post is a GIF
     * uploaded to reddit directly.
     *
     * For the source resolution see [RedditPost.getMp4Source]
     *
     * @return The list of MP4 resolutions
     */
    fun getMp4Previews(): List<RedditImage?>? {
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
    @SerializedName("hide_score")
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
    override var liked: Boolean? = null

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
    @JsonAdapter(ThirdPartyObjectAdapter::class)
    var thirdPartyObject: Any? = null


    /**
     * @return The type of post (image, video, text, or link)
     */
    fun getPostType(): PostType? {
        // TODO make this less bad
        if (isVideo || thirdPartyObject is ThirdPartyGif) {
            return PostType.VIDEO
        } else if (isSelf) {
            return PostType.TEXT
        } else if (isGallery || thirdPartyObject is ImgurAlbum) {
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