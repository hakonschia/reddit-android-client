package com.example.hakonsreader.activites

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.utils.LinkUtils
import com.jakewharton.processphoenix.ProcessPhoenix

class DispatcherActivity : AppCompatActivity() {
    companion object {
        private val TAG = "DispatcherActivity"

        /**
         * The key used to transfer the URL to dispatch
         *
         *
         * Example URL: https://www.reddit.com/r/
         */
        const val URL_KEY = "url"

        /**
         * Matches variants of "reddit.com"
         *
         * Matches:
         * - http
         * - https
         * - .com
         * - .com/
         */
        private const val REDDIT_HOME_PAGE_URL = "^http(s)?://(\\*.)?reddit\\.com(/)?$"

        /**
         * Matches variants of "https://redd.it"
         *
         * Matches:
         * - http
         * - https
         * - www.redd.it
         * - www.redd.it/
         * - redd.it
         * - redd.it/
         */
        private const val REDDIT_HOME_PAGE_SHORTENED_URL = "^http(s)?://(www.)?redd.it(/)?$"
    }


    private var fadeTransition = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIntent: Intent = intent
        val uri = startIntent.data
        var url: String?

        // Activity started from a URL intent
        url = if (uri != null) {
            uri.toString()
        } else {
            // Activity started from a manual intent
            val data = startIntent.extras
            if (data == null) {
                finish()
                return
            }
            data.getString(URL_KEY)
        }

        if (url == null) {
            finish()
            return
        }

        Log.d(TAG, "Dispatching $url")

        // If the URL can be converted to a direct link (eg. as an image) ensure it is
        url = LinkUtils.convertToDirectUrl(url)

        // URLs sent here might be of "/r/whatever", so assume those are links to within reddit.com
        // and add the full url so it doesn't have to be handled separately, and potential links we don't
        // handle are sent out correctly to the browser
        if (!url.matches("^http(s)?.*".toRegex())) {
            url = "https://reddit.com" + (if (url[0] == '/') "" else "/") + url
        }

        val intent = url?.let { createIntent(it) }

        if (fadeTransition) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        startActivity(intent)
    }


    /**
     * Creates an intent based on the passed URL
     *
     * @param url The URL to create an intent for
     * @return An [Intent]
     */
    private fun createIntent(url: String): Intent {
        val asUri = Uri.parse(url)
        val pathSegments = asUri.pathSegments

        // Get the last segment to check for file extensions
        val lastSegment = if (pathSegments.isNotEmpty()) {
            pathSegments.last()
        } else ""

        return when {
            // "reddit.com", which is in a sense front page, but it makes more sense to treat this
            // as a "start the app" intent
            url.matches(REDDIT_HOME_PAGE_URL.toRegex()) || url.matches(REDDIT_HOME_PAGE_SHORTENED_URL.toRegex()) -> {
                Intent(this, MainActivity::class.java)
            }

            // Subreddits: https://reddit.com/r/GlobalOffensive
            url.matches(LinkUtils.SUBREDDIT_REGEX_COMBINED.toRegex(RegexOption.IGNORE_CASE)) -> {
                // First is "r", second is the subreddit
                val subreddit = pathSegments[1]

                Intent(this, SubredditActivity::class.java).apply {
                    putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit)
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

                Intent(this, SubredditActivity::class.java).apply {
                    putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit)

                    // These might be null, but that does not matter
                    putExtra(SubredditActivity.SORT, sort)
                    putExtra(SubredditActivity.TIME_SORT, timeSort)
                }
            }

            // Users: https://reddit.com/user/hakonschia OR https://reddit.com/u/hakonschia
            url.matches(LinkUtils.USER_REGEX.toRegex()) -> {
                // Same as with subreddits, first is "u", second is the username
                val username = pathSegments[1]
                Intent(this, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.USERNAME_KEY, username)
                }
            }

            // Posts: https://reddit.com/r/GlobalOffensive/comments/gwcxmm/....
            url.matches(LinkUtils.POST_REGEX.toRegex()) -> {
                // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/...
                val postId = pathSegments[3]

                Intent(this, PostActivity::class.java).apply {
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
                Intent(this, PostActivity::class.java).apply {
                    putExtra(PostActivity.POST_ID_KEY, postId)
                }
            }

            // Posts from shortened urls: https://redd.it/gwcxmm
            url.matches(LinkUtils.POST_SHORTENED_URL_REGEX.toRegex()) -> {
                // The URL will look like: redd.it/<postId>
                val postId = pathSegments[0]
                Intent(this, PostActivity::class.java).apply {
                    putExtra(PostActivity.POST_ID_KEY, postId)
                }
            }

            // Private messages: https://reddit.com/message/compose?to=hakonschia&subject=hello
            url.matches("https://(.*\\.)?reddit.com/message/compose.*".toRegex()) -> {
                val recipient = asUri.getQueryParameter("to")
                val subject = asUri.getQueryParameter("subject")
                val message = asUri.getQueryParameter("message")

                Intent(this, SendPrivateMessageActivity::class.java).apply {
                    putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, recipient)
                    putExtra(SendPrivateMessageActivity.EXTRAS_SUBJECT, subject)
                    putExtra(SendPrivateMessageActivity.EXTRAS_MESSAGE, message)
                }
            }

            // Images, load directly in the app
            lastSegment.matches(".+(.png|.jpeg|.jpg)$".toRegex()) -> {
                fadeTransition = true
                Intent(this, ImageActivity::class.java).apply {
                    putExtra(ImageActivity.IMAGE_URL, url)
                }
            }

            // No direct handling, redirect to an app if found, otherwise to WebViewActivity/browser
            else -> {
                val baseIntent = Intent(Intent.ACTION_VIEW, asUri)

                // Find all activities this intent would resolve to
                val intentActivities = packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)

                // To check if the intent matches an app we need to find the default browser as that
                // will usually be in the list of intent activities
                val defaultBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
                val defaultBrowserInfo = packageManager.resolveActivity(defaultBrowserIntent, PackageManager.MATCH_DEFAULT_ONLY)

                var appActivityFound = false
                val appPackageName = applicationContext.packageName

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
                    Intent(this, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.URL, url)
                    }
                } else {
                    baseIntent
                }
            }
        }
    }


    // onResume is called when activity is returned to by exiting another, and when it starts initially
    // onPause is only called when the activity pauses, such as when starting another activity, so if
    // we have paused previously when in onResume we can finish the activity
    var paused = false

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            finish()
        }
    }
}