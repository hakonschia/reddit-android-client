package com.example.hakonsreader.misc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.hakonsreader.App
import com.example.hakonsreader.activites.*
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.utils.LinkUtils
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import java.util.*

/**
 * Loads an image from a network URL with a preferred and a backup URL. The backup URL will only
 * be loaded if the preferred image is not cached
 *
 * @param preferredUrl The preferred URL to load, which will only be loaded if it is
 * in cache
 * @param backupUrl The backup URL that will be loaded if [preferredUrl] is not cached
 * @param into The ImageView to load the image into
 */
fun Picasso.loadIf(preferredUrl: String?, backupUrl: String?, into: ImageView) {
    this.load(preferredUrl).networkPolicy(NetworkPolicy.OFFLINE).into(into, object : Callback {
        override fun onSuccess() {
            // Not implemented
        }

        override fun onError(e: Exception?) {
            this@loadIf.load(backupUrl).into(into)
        }
    })
}

/**
 * Enable or disable cache when loading images with Picasso
 *
 * @param cache If true the image will be cached
 * @return A RequestCreator that will set the [RequestCreator.networkPolicy] with [NetworkPolicy.NO_STORE]
 * if [cache] is false. Otherwise, the creator is returned as is
 */
fun RequestCreator.cache(cache: Boolean) : RequestCreator {
    return if (cache) {
        this
    } else {
        this.networkPolicy(NetworkPolicy.NO_STORE)
    }
}

/**
 * Class for holding image URL variants for a [RedditPost]
 *
 * @param normal The URL pointing to the normal URL. If this is null, no image was found
 * @param nsfw The URL pointing to the NSFW image to use. If this is null, no image should be shown
 * as either chosen by the user, or because no image was found
 * @param spoiler The URL pointing to the spoiler image to use. If this is null, no image was found and no image
 * should be used
 */
data class PostImageVariants(var normal: String?, var nsfw: String?, var spoiler: String?)

/**
 * Gets image variants for a reddit post
 *
 * @param post The post to get images for
 * @return A [PostImageVariants] that holds the images to use for if the post is normal, nsfw, or spoiler
 */
fun getImageVariantsForRedditPost(post: RedditPost) : PostImageVariants {
    return PostImageVariants(getNormal(post), getNsfw(post), getObfuscated(post))
}

/**
 * Gets the normal
 */
private fun getNormal(post: RedditPost) : String? {
    val screenWidth = App.get().screenWidth
    var imageUrl: String? = null

    post.getPreviewImages()?.forEach {
        if (it.width <= screenWidth) {
            imageUrl = it.url
        }
    }

    return imageUrl
}

/**
 * Gets the NSFW image to use for a post
 *
 * @return A URL pointing to the image to use for a post, depending on [App.showNsfwPreview]. If this is null
 * then no image should be shown ([ShowNsfwPreview.NO_IMAGE])
 */
private fun getNsfw(post: RedditPost) : String? {
    return when (App.get().showNsfwPreview()) {
        ShowNsfwPreview.NORMAL -> getNormal(post)
        ShowNsfwPreview.BLURRED -> getObfuscated(post)
        ShowNsfwPreview.NO_IMAGE-> null
    }
}

/**
 * Gets an obfuscated image URL for a post
 *
 * @return A URL pointing to an obfuscated image, or null if no image is available
 */
private fun getObfuscated(post: RedditPost) : String? {
    val obfuscatedPreviews: List<Image>? = post.getObfuscatedPreviewImages()

    return if (obfuscatedPreviews?.isNotEmpty() == true) {
        // Obfuscated previews that are high res are still fairly showing sometimes, so
        // get the lowest quality one as that will not be very easy to tell what it is
        obfuscatedPreviews[0].url!!
    } else {
        null
    }
}

/**
 * Opens an intent to allow the user to log in. The OAuth state is generated with [App.generateAndGetOAuthState]
 * and will be stored there
 *
 * @param context The context to start the intent with
 */
fun startLoginIntent(context: Context) {
    // Generate a new state to validate when we get a response back
    val state = App.get().generateAndGetOAuthState()

    val uri = Uri.Builder()
            .scheme("https")
            .authority("reddit.com")
            .path("api/v1/authorize")
            .appendQueryParameter("response_type", NetworkConstants.RESPONSE_TYPE)
            .appendQueryParameter("duration", NetworkConstants.DURATION)
            .appendQueryParameter("redirect_uri" , NetworkConstants.CALLBACK_URL)
            .appendQueryParameter("client_id", NetworkConstants.CLIENT_ID)
            .appendQueryParameter("scope", NetworkConstants.SCOPE)
            .appendQueryParameter("state", state)
            .build()

    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, uri), null)
}


private val IMAGE_FORMATS = Collections.unmodifiableList(listOf("png", "jpg", "jpeg"))

/**
 * Creates an intent based on the passed URL
 *
 * @param url The URL to create an intent for. The URL will be converted in multiple ways:
 * * It will be passed through [LinkUtils.convertToDirectUrl]
 * * If it does not match https/http it will be assumed this link is a Reddit link and reddit.com will be added
 * @param context The context to create the intent with
 * @return An [Intent]
 */
fun createIntent(url: String, context: Context) : Intent {
    // If the URL can be converted to a direct link (eg. as an image) ensure it is
    var convertedUrl = LinkUtils.convertToDirectUrl(url)

    // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
    // and add the full url so it doesn't have to be handled separately, and potential links we don't
    // handle are sent out correctly to the browser
    if (!convertedUrl.matches("^http(s)?.*".toRegex())) {
        convertedUrl = "https://reddit.com" + (if (convertedUrl[0] == '/') "" else "/") + convertedUrl
    }

    return createIntentInternal(convertedUrl, context)
}

/**
 * Creates an intent based on the passed URL
 *
 * @param url The URL to create an intent for
 * @param context The context to create the intent with
 * @return An [Intent]
 */
fun createIntentInternal(url: String, context: Context): Intent {
    println("Dispatching $url")

    val asUri = Uri.parse(url)
    val pathSegments = asUri.pathSegments

    // Get the last segment to check for file extensions
    val lastSegment = if (pathSegments.isNotEmpty()) {
        pathSegments.last()
    } else ""

    val youtubeVideoId = asUri.getQueryParameter("v")

    return when {
        // "reddit.com", which is in a sense front page, but it makes more sense to treat this
        // as a "start the app" intent
        url.matches("^http(s)?://(.*)?reddit\\.com(/)?$".toRegex()) || url.matches("^http(s)?://(www.)?redd.it(/)?$".toRegex()) -> {
            Intent(context, MainActivity::class.java)
        }

        // Subreddits: https://reddit.com/r/GlobalOffensive
        url.matches(LinkUtils.SUBREDDIT_REGEX_COMBINED.toRegex(RegexOption.IGNORE_CASE)) -> {
            // First is "r", second is the subreddit
            val subreddit = pathSegments[1].toLowerCase()

            // TODO if the application is "new" we can also pass the subreddit to MainActivity since
            //  the subreddit will be sent to the navbar subreddit and we wont mess up anything else
            if (subreddit == "popular" || subreddit == "all") {
                Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.START_SUBREDDIT, subreddit)
                }
            } else {
                Intent(context, SubredditActivity::class.java).apply {
                    putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit)
                }
            }
        }

        // Subreddits with sort: https://reddit.com/r/GlobalOffensive/top?t=all
        url.matches(LinkUtils.SUBREDDIT_SORT_REGEX_WITH_HTTPS.toRegex()) -> {
            // First is "r", second is the subreddit
            val subreddit = pathSegments[1]

            // Either "r/GlobalOffensive/top" or "r/GlobalOffensive?sort=top"
            val sort = if (pathSegments.size >= 3) {
                pathSegments[2]
            } else {
                asUri.getQueryParameter("sort")
            }

            val timeSort = asUri.getQueryParameter("t")

            Intent(context, SubredditActivity::class.java).apply {
                putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit)

                // These might be null, but that does not matter
                putExtra(SubredditActivity.SORT, sort)
                putExtra(SubredditActivity.TIME_SORT, timeSort)
            }
        }

        // Subreddits with rules: https://reddit.com/r/GlobalOffensive/about/rules
        url.matches(LinkUtils.SUBREDDIT_RULES_REGEX_WITH_HTTPS.toRegex()) -> {
            // First is "r", second is the subreddit
            val subreddit = pathSegments[1]

            Intent(context, SubredditActivity::class.java).apply {
                putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit)
                putExtra(SubredditActivity.SHOW_RULES, true)
            }
        }


        // Users: https://reddit.com/user/hakonschia OR https://reddit.com/u/hakonschia
        url.matches(LinkUtils.USER_REGEX.toRegex()) -> {
            // Same as with subreddits, first is "u", second is the username
            val username = pathSegments[1]
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.USERNAME_KEY, username)
            }
        }

        // Posts: https://reddit.com/r/GlobalOffensive/comments/gwcxmm/....
        url.matches(LinkUtils.POST_REGEX.toRegex()) -> {
            // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/...
            val postId = pathSegments[3]

            Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.POST_ID_KEY, postId)

                // Add the ID of the comment chain specified, if available
                if (pathSegments.size >= 6) {
                    putExtra(PostActivity.COMMENT_ID_CHAIN, pathSegments[5])
                }
            }
            // TODO when the post is in a "user" subreddit it doesnt work
            //  eg: https://www.reddit.com/user/HyperBirchyBoy/comments/jbkw1f/moon_landing_with_benny_hill_and_sped_up/?utm_source=share&utm_medium=ios_app&utm_name=iossmf
        }

        // https://reddit.com/comments/gwcxmm
        url.matches(LinkUtils.POST_REGEX_NO_SUBREDDIT.toRegex()) -> {
            // The URL will look like: reddit.com/comments/<postId>
            val postId = pathSegments[1]
            Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.POST_ID_KEY, postId)
            }
        }

        // Posts from shortened urls: https://redd.it/gwcxmm
        url.matches(LinkUtils.POST_SHORTENED_URL_REGEX.toRegex()) -> {
            // The URL will look like: redd.it/<postId>
            val postId = pathSegments[0]
            Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.POST_ID_KEY, postId)
            }
        }

        // Private messages: https://reddit.com/message/compose?to=hakonschia&subject=hello
        url.matches("https://(.*\\.)?reddit.com/message/compose.*".toRegex()) -> {
            val recipient = asUri.getQueryParameter("to")
            val subject = asUri.getQueryParameter("subject")
            val message = asUri.getQueryParameter("message")

            Intent(context, SendPrivateMessageActivity::class.java).apply {
                putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, recipient)
                putExtra(SendPrivateMessageActivity.EXTRAS_SUBJECT, subject)
                putExtra(SendPrivateMessageActivity.EXTRAS_MESSAGE, message)
            }
        }

        // Images, load directly in the app
        lastSegment.matches(".+(.png|.jpeg|.jpg)$".toRegex())
                // Some links (like twitter images) don't end in ".png" but have "?format=png" so
                // if the format parameter is an image format assume this is an image
                || IMAGE_FORMATS.contains(asUri.getQueryParameter("format")) -> {
            Intent(context, ImageActivity::class.java).apply {
                putExtra(ImageActivity.IMAGE_URL, url)
            }
        }

        // YouTube links, open in activity if user wants to
        App.get().openYouTubeVideosInApp() &&
                (!youtubeVideoId.isNullOrEmpty() || url.matches(".*youtu.be.*".toRegex())) -> {

            // https://www.youtube.com/watch?v=90X5NJleYJQ or
            // https://youtu.be/90X5NJleYJQ

            val videoId = youtubeVideoId ?: pathSegments.first()

            Intent(context, VideoYoutubeActivity::class.java).apply {
                putExtra(VideoYoutubeActivity.VIDEO_ID, videoId)
                putExtra(VideoYoutubeActivity.TIMESTAMP, asUri.getQueryParameter("t")?.toFloat())
            }
        }

        // No direct handling, redirect to an app if found, otherwise to WebViewActivity/browser
        else -> {
            val baseIntent = Intent(Intent.ACTION_VIEW, asUri)

            // Find all activities context intent would resolve to
            val intentActivities = context.packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)

            // To check if the intent matches an app we need to find the default browser as that
            // will usually be in the list of intent activities
            val defaultBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
            val defaultBrowserInfo = context.packageManager.resolveActivity(defaultBrowserIntent, PackageManager.MATCH_DEFAULT_ONLY)

            var appActivityFound = false
            val appPackageName = context.applicationContext.packageName

            // Check if there are intents not leading to a browser
            for (intentActivity in intentActivities) {
                val packageName = intentActivity.activityInfo.packageName

                // Don't match our own app, as that would cause an infinite loop
                if (packageName != defaultBrowserInfo!!.activityInfo.packageName
                        && packageName != appPackageName) {
                    appActivityFound = true
                    break
                }
            }

            // If no activity found and user wants to open links in app, open in WebView (internal browser)
            if (!appActivityFound && App.get().openLinksInApp()) {
                Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.URL, url)
                }
            } else {
                baseIntent
            }
        }
    }
}