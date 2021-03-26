package com.example.hakonsreader.misc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.*
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.exceptions.ArchivedException
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.exceptions.RateLimitException
import com.example.hakonsreader.api.exceptions.ThreadLockedException
import com.example.hakonsreader.api.model.Image
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.utils.LinkUtils
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.fragments.bottomsheets.PeekLinkBottomSheet
import com.example.hakonsreader.states.LoggedInState.PrivatelyBrowsing
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.*

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
 * @return A URL pointing to the image to use for a post, depending on [Settings.showNsfwPreview]. If this is null
 * then no image should be shown ([ShowNsfwPreview.NO_IMAGE])
 */
private fun getNsfw(post: RedditPost) : String? {
    return when (Settings.showNsfwPreview()) {
        ShowNsfwPreview.NORMAL -> getNormal(post)
        ShowNsfwPreview.BLURRED -> getObfuscated(post)
        ShowNsfwPreview.NO_IMAGE -> null
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
            .appendQueryParameter("redirect_uri", NetworkConstants.CALLBACK_URL)
            .appendQueryParameter("client_id", NetworkConstants.CLIENT_ID)
            .appendQueryParameter("scope", NetworkConstants.SCOPE)
            .appendQueryParameter("state", state)
            .build()

    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, uri), null)
}


private val IMAGE_FORMATS = Collections.unmodifiableList(listOf("png", "jpg", "jpeg"))

/**
 * Options class used for [createIntent]. All values for this class has a default of `true`
 */
class CreateIntentOptions(val openLinksInternally: Boolean = true, val openYoutubeVideosInternally: Boolean = true)

/**
 * Creates an intent based on the passed URL
 *
 * @param url The URL to create an intent for. The URL will be converted in multiple ways:
 * * It will be passed through [LinkUtils.convertToDirectUrl]
 * * If it does not match https/http it will be assumed this link is a Reddit link and reddit.com will be added
 * @param options The options for the intent
 * @param context The context to create the intent with
 * @return An [Intent]
 */
fun createIntent(url: String, options: CreateIntentOptions, context: Context) : Intent {
    // If the URL can be converted to a direct link (eg. as an image) ensure it is
    var convertedUrl = LinkUtils.convertToDirectUrl(url)

    // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
    // and add the full url so it doesn't have to be handled separately, and potential links we don't
    // handle are sent out correctly to the browser
    if (!convertedUrl.matches("^http(s)?.*".toRegex())) {
        convertedUrl = "https://reddit.com" + (if (convertedUrl[0] == '/') "" else "/") + convertedUrl
    }

    return createIntentInternal(convertedUrl, options, context)
}

/**
 * Creates an intent based on the passed URL
 *
 * @param url The URL to create an intent for
 * @param options The options for the intent
 * @param context The context to create the intent with
 * @return An [Intent]
 */
fun createIntentInternal(url: String, options: CreateIntentOptions, context: Context): Intent {
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
                    putExtra(MainActivity.EXTRAS_START_SUBREDDIT, subreddit)
                }
            } else {
                Intent(context, SubredditActivity::class.java).apply {
                    putExtra(SubredditActivity.EXTRAS_SUBREDDIT_KEY, subreddit)
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
                putExtra(SubredditActivity.EXTRAS_SUBREDDIT_KEY, subreddit)

                // These might be null, but that does not matter
                putExtra(SubredditActivity.EXTRAS_SORT, sort)
                putExtra(SubredditActivity.EXTRAS_TIME_SORT, timeSort)
            }
        }

        // Subreddits with rules: https://reddit.com/r/GlobalOffensive/about/rules
        url.matches(LinkUtils.SUBREDDIT_RULES_REGEX_WITH_HTTPS.toRegex()) -> {
            // First is "r", second is the subreddit
            val subreddit = pathSegments[1]

            Intent(context, SubredditActivity::class.java).apply {
                putExtra(SubredditActivity.EXTRAS_SUBREDDIT_KEY, subreddit)
                putExtra(SubredditActivity.EXTRAS_SHOW_RULES, true)
            }
        }


        // Users: https://reddit.com/user/hakonschia OR https://reddit.com/u/hakonschia
        url.matches(LinkUtils.USER_REGEX.toRegex()) -> {
            // Same as with subreddits, first is "u", second is the username
            val username = pathSegments[1]
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.EXTRAS_USERNAME_KEY, username)
            }
        }

        // Posts: https://reddit.com/r/GlobalOffensive/comments/gwcxmm/....
        url.matches(LinkUtils.POST_REGEX.toRegex()) -> {
            // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/...
            val postId = pathSegments[3]

            Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.EXTRAS_POST_ID_KEY, postId)

                // Add the ID of the comment chain specified, if available
                if (pathSegments.size >= 6) {
                    putExtra(PostActivity.EXTRAS_COMMENT_ID_CHAIN, pathSegments[5])
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
                putExtra(PostActivity.EXTRAS_POST_ID_KEY, postId)
            }
        }

        // Posts from shortened urls: https://redd.it/gwcxmm
        url.matches(LinkUtils.POST_SHORTENED_URL_REGEX.toRegex()) -> {
            // The URL will look like: redd.it/<postId>
            val postId = pathSegments[0]
            Intent(context, PostActivity::class.java).apply {
                putExtra(PostActivity.EXTRAS_POST_ID_KEY, postId)
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
                putExtra(ImageActivity.EXTRAS_IMAGE_URL, url)
            }
        }

        // YouTube links, open in activity if user wants to
        options.openYoutubeVideosInternally &&
                (!youtubeVideoId.isNullOrEmpty() || url.matches(".*youtu.be.*".toRegex())) -> {

            // https://www.youtube.com/watch?v=90X5NJleYJQ or
            // https://youtu.be/90X5NJleYJQ

            val videoId = youtubeVideoId ?: pathSegments.first()

            Intent(context, VideoYoutubeActivity::class.java).apply {
                putExtra(VideoYoutubeActivity.EXTRAS_VIDEO_ID, videoId)
                putExtra(VideoYoutubeActivity.EXTRAS_TIMESTAMP, asUri.getQueryParameter("t")?.toFloat())
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
            if (!appActivityFound && options.openLinksInternally) {
                Intent(context, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRAS_URL, url)
                }
            } else {
                baseIntent
            }
        }
    }
}

/**
 * Gets a string description of a sorting method
 */
fun getSortText(sort: SortingMethods, context: Context) : String {
    return when (sort) {
        SortingMethods.NEW -> context.getString(R.string.sortNew)
        SortingMethods.HOT -> context.getString(R.string.sortHot)
        SortingMethods.TOP -> context.getString(R.string.sortTop)
        SortingMethods.CONTROVERSIAL -> context.getString(R.string.sortControversial)
    }
}

/**
 * Gets a string description of a time sorting method
 */
fun getTimeSortText(timeSort: PostTimeSort, context: Context) : String {
    return when (timeSort) {
        PostTimeSort.HOUR -> context.getString(R.string.sortNow)
        PostTimeSort.DAY -> context.getString(R.string.sortToday)
        PostTimeSort.WEEK -> context.getString(R.string.sortWeek)
        PostTimeSort.MONTH -> context.getString(R.string.sortMonth)
        PostTimeSort.YEAR -> context.getString(R.string.sortYear)
        PostTimeSort.ALL_TIME -> context.getString(R.string.sortAllTime)
    }
}


/**
 * Handles generic errors that are common for all API responses and shows a snackbar to the user
 *
 * @param parent The view to attach the snackbar to
 * @param error The error for the request
 * @param t Throwable from the request
 * @param anchor Optionally, the anchor of the snackbar
 */
fun handleGenericResponseErrors(parent: View, error: GenericError, t: Throwable, anchor: View? = null) {
    val code = error.code
    val reason = error.reason
    t.printStackTrace()

    when {
        t is IOException -> {
            if (t is SocketTimeoutException) {
                showNetworkTimeoutException(parent, anchor)
            } else {
                showNoInternetSnackbar(parent, anchor)
            }
        }

        t is InvalidAccessTokenException -> {
            if (App.get().loggedInState.value is PrivatelyBrowsing) {
                showPrivatelyBrowsingSnackbar(parent, anchor)
            } else {
                showNotLoggedInSnackbar(parent, anchor)
            }
        }

        t is ThreadLockedException -> {
            showThreadLockedException(parent, anchor)
        }

        t is ArchivedException -> {
            showArchivedException(parent, anchor)
        }

        reason == GenericError.REQUIRES_REDDIT_PREMIUM -> {
            showRequiresRedditPremiumSnackbar(parent, anchor)
        }

        code == 400 -> {
            // 400 requests are "Bad request" which means something went wrong (Reddit are generally pretty
            // "secretive" with their error responses, they only give a code)
            showBadRequestSnackbar(parent, anchor)
        }

        code == 403 -> {
            showForbiddenErrorSnackbar(parent, anchor)
        }

        code == 429 || t is RateLimitException -> {
            // 429 = Too many requests. Reddit sometimes returns a 429, or 200 with a "RATELIMIT" error message
            showTooManyRequestsSnackbar(parent, anchor)
        }

        code in 500..599 -> {
            showGenericServerErrorSnackbar(parent, anchor)
        }

        else -> {
            showUnknownError(parent, anchor)
        }
    }
}

/**
 * Creates and shows a snackbar for errors caused by no internet connection
 *
 * @param parent The view to attach the snackbar to
 */
fun showNoInternetSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, parent.resources.getString(R.string.noInternetConnection), BaseTransientBottomBar.LENGTH_LONG)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by a network timeout
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showNetworkTimeoutException(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.networkTimeout, BaseTransientBottomBar.LENGTH_LONG)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for when an action was attempted that requires a logged in user,
 * but private browsing is currently enabled.
 *
 *
 * The snackbar includes a button to disable private browsing
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showPrivatelyBrowsingSnackbar(parent: View, anchor: View? = null) {
    val context = parent.context
    Snackbar.make(parent, R.string.privatelyBrowsingError, BaseTransientBottomBar.LENGTH_LONG)
            .setAnchorView(anchor)
            .setAction(R.string.disable) { App.get().enablePrivateBrowsing(false) }
            .setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            .show()
    }

/**
 * Creates and shows a snackbar for when an action was attempted that requires the user to be logged in
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showNotLoggedInSnackbar(parent: View, anchor: View? = null) {
    val context = parent.context
    Snackbar.make(parent, R.string.notLoggedInError, BaseTransientBottomBar.LENGTH_LONG)
            .setAnchorView(anchor)
            .setAction(R.string.log_in) {
                // If getContext instance of MainActivity we can set the nav bar item to profile and, otherwise create activity for logging in
                if (context is MainActivity) {
                    context.selectProfileNavBar()
                } else {
                    // Otherwise we can open an activity showing a login fragment
                    context.startActivity(Intent(context, LogInActivity::class.java))
                }
            }
            .setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by a thread being locked
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showThreadLockedException(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.threadLockedError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by a listing being archived
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showArchivedException(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.listingArchivedError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by a 400 bad request error
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showBadRequestSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.badRequestError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by no internet connection
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showForbiddenErrorSnackbar(parent: View, anchor: View? = null) {
    // 403 errors are generally when the access token is outdated and new functionality has been
    // added that requires more OAuth scopes
    Snackbar.make(parent, R.string.forbiddenError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for generic server errors
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showGenericServerErrorSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.genericServerError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by too many requests sent
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showTooManyRequestsSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.tooManyRequestsError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by too many requests sent
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showErrorLoggingInSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.errorLoggingIn, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by no internet connection
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showUnknownError(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.unknownError, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}

/**
 * Creates and shows a snackbar for errors caused by an action being attempted that requires
 * Reddit premium
 *
 * @param parent The view to attach the snackbar to
 * @param anchor Optionally, the anchor of the snackbar
 */
fun showRequiresRedditPremiumSnackbar(parent: View, anchor: View? = null) {
    Snackbar.make(parent, R.string.requiresRedditPremium, BaseTransientBottomBar.LENGTH_SHORT)
            .setAnchorView(anchor)
            .show()
}


/**
 * Creates the text for text age text fields. For a shorter text see
 * [createAgeTextShortened]
 *
 * Formats to make sure that it says 3 hours, 5 minutes etc. based on what makes sense
 *
 * @param resources Resources to retrieve strings from
 * @param time The time to format as
 * @return The time formatted as a string
 */
fun createAgeText(resources: Resources, time: Duration): String {
    var t: Long

    val format = when {
        time.toDays().also { t = it } > 0L -> {
            resources.getQuantityString(R.plurals.postAgeDays, t.toInt())
        }

        time.toHours().also { t = it } > 0 -> {
            resources.getQuantityString(R.plurals.postAgeHours, t.toInt())
        }

        else -> {
            t = time.toMinutes()
            if (t < 1) {
                resources.getString(R.string.postAgeJustPosted)
            } else {
                resources.getQuantityString(R.plurals.postAgeMinutes, t.toInt(),  t.toInt())
            }
        }
    }

    return String.format(Locale.getDefault(), format, t)
}


/**
 * Creates the text for text age text fields with a shorter text than with
 * [createAgeText]
 *
 * Formats to make sure that it says 3h, 5m etc. based on what makes sense
 *
 * @param resources Resources to retrieve strings from
 * @param time The time to format as
 * @return The time formatted as a string
 */
fun createAgeTextShortened(resources: Resources, time: Duration): String {
    var t: Long

    val format = when {
        time.toDays().also { t = it } > 0L -> {
            resources.getString(R.string.postAgeDaysShortened, t.toInt())
        }

        time.toHours().also { t = it } > 0 -> {
            resources.getString(R.string.postAgeHoursShortened, t.toInt())
        }

        else -> {
            t = time.toMinutes()
            if (t < 1) {
                resources.getString(R.string.postAgeJustPostedShortened)
            } else {
                resources.getString(R.string.postAgeMinutesShortened, t.toInt())
            }
        }
    }

    return String.format(Locale.getDefault(), format, t)
}

/**
 * Creates the text for text age on trending subreddits
 *
 * Formats to make sure that it says 3 hours, 5 minutes etc. based on what makes sense
 *
 * @param tv The text view to set the text on
 * @param time The time to format as
 */
fun setAgeTextTrendingSubreddits(tv: TextView, time: Duration) {
    val resources = tv.resources
    var t: Long

    val format = when {
        time.toDays().also { t = it } > 0L -> {
            resources.getQuantityString(R.plurals.postAgeDays, t.toInt())
        }

        time.toHours().also { t = it } > 0 -> {
            resources.getQuantityString(R.plurals.postAgeHours, t.toInt())
        }

        else -> {
            t = time.toMinutes()
            if (t < 1) {
                tv.setText(R.string.trendingSubredditsLastUpdatedNow)
                return
            }
            resources.getQuantityString(R.plurals.postAgeMinutes, t.toInt())
        }
    }

    val str = String.format(Locale.getDefault(), format, t)
    tv.text = resources.getString(R.string.trendingSubredditsLastUpdated, str)
}

/**
 * Create a duration string in the format of "mm:ss" that can be used in videos
 *
 * @param seconds The amount of seconds to display
 * @return A string formatted as "mm:ss"
 */
fun createVideoDuration(seconds: Int): String {
    return String.format("%02d:%02d", seconds % 3600 / 60, seconds % 60)
}

/**
 * Converts dp to pixels
 * @param dp The amount of dp to convert
 * @param res The resources
 * @return The pixel amount of *dp*
 */
fun dpToPixels(dp: Float, res: Resources): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            res.displayMetrics
    ).toInt()
}

/**
 * Shows a bottom sheet to peek a URL
 *
 * @param activity The activity to show the fragment from
 * @param text The text of the URL
 * @param url The URL
 */
fun showPeekUrlBottomSheet(activity: AppCompatActivity, text: String, url: String) {
    PeekLinkBottomSheet.newInstance(text = text, url = url).run {
        show(activity.supportFragmentManager, "Peek URL")
    }
}
