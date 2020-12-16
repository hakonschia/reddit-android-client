package com.example.hakonsreader

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.*
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.utils.MarkdownAdjuster
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.markwonplugins.*
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.TokenManager
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel
import com.facebook.stetho.Stetho
import com.jakewharton.processphoenix.ProcessPhoenix
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.util.*
import java.util.function.Consumer

/**
 * Entry point for the application. Sets up various static variables used throughout the app
 */
class App : Application() {

    companion object {
        private const val TAG = "App"

        /**
         * The key used in SharedPreferences to store if the API should be in private browsing from startup
         */
        private const val PRIVATELY_BROWSING_KEY = "privatelyBrowsing"
        private lateinit var app: App

        /**
         * Retrieve the instance of the application that can be used to access various methods
         *
         * @return The static App instance
         */
        fun get(): App {
            return app
        }

        /**
         * @return Retrieves the user information stored in SharedPreferences
         */
        val storedUser: RedditUser?
            get() = SharedPreferencesManager.get(SharedPreferencesConstants.USER_INFO, RedditUser::class.java)

        /**
         * Stores information about a user in SharedPreferences
         *
         * @param user The object to store
         */
        fun storeUserInfo(user: RedditUser?) {
            SharedPreferencesManager.put(SharedPreferencesConstants.USER_INFO, user)
        }
    }


    /**
     * @return The width of the screen in pixels
     */
    var screenWidth = 0
        private set

    /**
     * @return The height of the screen in pixels
     */
    var screenHeight = 0
        private set

    /**
     * Retrieves the current OAuth state. To generate a new state use [App.generateAndGetOAuthState]
     *
     * @return The current OAuth state
     */
    var oauthState: String? = null
        private set

    /**
     * The default [SharedPreferences] that holds the users settings
     */
    private val settings by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    /**
     * True if WiFi is currently enabled
     */
    private var wifiConnected = false

    /**
     * The [Markwon] object used to format markdown text. This object has custom plugins for Reddit
     * specific markdown, and defines a custom theme
     */
    val markwon: Markwon by lazy {
        // This requires the resources to be set, so lazy initialize it
        createMarkwon()
    }

    /**
     * @return The [RedditApi] object to use for API calls
     */
    val api: RedditApi by lazy {
        // This requires SharedPreferencesManager to be set, so lazy initialize it
        createApi()
    }

    /**
     * An instance of a [MarkdownAdjuster]. This instance checks the following:
     * * Header spaces
     * * URL encoding in Markdown links
     * *
     */
    val adjuster: MarkdownAdjuster = createMarkdownAdjuster()

    private val privateBrowsingObservables: MutableList<PrivateBrowsingObservable> = ArrayList()
    private var activeActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        set()
        val dm = resources.displayMetrics

        // Technically this could go outdated if the user changes their resolution while the app is running
        // but I highly doubt that would ever be a problem (worst case is posts wouldn't fit the screen)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels

        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE)
        SharedPreferencesManager.create(prefs)

        updateTheme()

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        val db = RedditDatabase.getInstance(this)
        // Remove records that are older than 12 hours, as they likely won't be used again
        CoroutineScope(IO).launch {
            val maxAge = 60.toLong() * 60 * 12
            val count = db.posts().count
            val deleted = db.posts().deleteOld(maxAge)

            Log.d(TAG, "onCreate: # of records=$count; # of deleted=$deleted")
        }
    }

    /**
     * Sets the app instance
     */
    private fun set() {
        app = this
    }


    /**
     * Unregisters any receivers that have been registered
     */
    fun registerReceivers() {
        // Register Wi-Fi broadcast receiver to be notified when Wi-Fi is enabled/disabled
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiStateReceiver, intentFilter)
    }

    /**
     * Unregisters any receivers that have been registered
     */
    fun unregisterReceivers() {
        // The receiver might not have been registered, but since there isn't a way to check if
        // a receiver has been registered this will do
        try {
            unregisterReceiver(wifiStateReceiver)
        } catch (ignored: IllegalArgumentException) {
            // Ignored
        }
    }

    /**
     * Broadcast receiver for Wi-Fi state
     */
    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                val connected = info!!.isConnected
                wifiConnected = connected
            }
        }
    }


    /**
     * Creates a [Markwon] object with plugins set
     *
     * @return A [Markwon] object ready to format some markdown :)
     */
    private fun createMarkwon(): Markwon {
        return Markwon.builder(this)
                // Headers, blockquote etc. are a part of the core
                .usePlugin(CorePlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(StrikethroughPlugin.create())

                // Custom plugins
                .usePlugin(RedditSpoilerPlugin())
                .usePlugin(RedditLinkPlugin(this))
                .usePlugin(SuperscriptPlugin())
                .usePlugin(LinkPlugin())
                .usePlugin(EnlargeLinkPlugin())
                .usePlugin(ThemePlugin(this))
                .build()
    }

    /**
     * Creates a new [RedditApi] instance.
     *
     * [RedditApi.enablePrivateBrowsing] is called based on the value stored in SharedPreferences
     * with the key [App.PRIVATELY_BROWSING_KEY]
     */
    private fun createApi() : RedditApi {
        val api = RedditApi.Builder(NetworkConstants.USER_AGENT, NetworkConstants.CLIENT_ID)
                .accessToken(TokenManager.getToken())
                .onNewToken { newToken: AccessToken? -> TokenManager.saveToken(newToken) }
                .loggerLevel(HttpLoggingInterceptor.Level.BODY)
                .callbackUrl(NetworkConstants.CALLBACK_URL)
                .deviceId(UUID.randomUUID().toString())
                .onInvalidToken { error: GenericError, throwable: Throwable -> onInvalidAccessToken(error, throwable) }
                .loadImgurAlbumsAsRedditGalleries(NetworkConstants.IMGUR_CLIENT_ID)
                .build()

        val privatelyBrowsing = settings.getBoolean(PRIVATELY_BROWSING_KEY, false)
        api.enablePrivateBrowsing(privatelyBrowsing)

        return api
    }

    /**
     * Returns a new instance of a [MarkdownAdjuster]
     */
    private fun createMarkdownAdjuster() : MarkdownAdjuster {
        return MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .checkUrlEncoding()
                .build()
    }

    /**
     * Clears the OAuth state.
     *
     *
     * Use this when the state has been verified
     */
    fun clearOAuthState() {
        oauthState = null
    }

    /**
     * Generates a random string to use for OAuth requests
     *
     * @return A new random string
     */
    private fun generateOauthState(): String {
        val characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val rnd: Random = SecureRandom()
        val state = StringBuilder()
        for (i in 0..34) {
            state.append(characters[rnd.nextInt(characters.length)])
        }
        return state.toString()
    }

    /**
     * Generates a new OAuth state that is used for validation
     *
     * @return A random string to use in the request for access
     */
    fun generateAndGetOAuthState(): String? {
        oauthState = generateOauthState()
        return oauthState
    }

    /**
     * Checks if there currently is a user logged in
     *
     * @return True if there is a user logged in
     * @see App.isUserLoggedInPrivatelyBrowsing
     */
    fun isUserLoggedIn(): Boolean {
        val accessToken = TokenManager.getToken()
        // Only logged in users have a refresh token
        return accessToken != null && accessToken.refreshToken != null
    }

    /**
     * Checks if there currently is a user logged in that is privately browsing
     *
     * @return True if there is a user logged in and private browsing is enabled
     * @see App.isUserLoggedIn
     */
    fun isUserLoggedInPrivatelyBrowsing(): Boolean {
        return isUserLoggedIn() && api.isPrivatelyBrowsing
    }

    /**
     * Toggles the API to private browsing and stores locally that private browsing is enabled/disabled
     *
     * @param enable True to enable private browsing, false to disable
     */
    fun enablePrivateBrowsing(enable: Boolean) {
        api.enablePrivateBrowsing(enable)
        settings.edit().putBoolean(PRIVATELY_BROWSING_KEY, enable).apply()
        privateBrowsingObservables.forEach(Consumer { observable: PrivateBrowsingObservable -> observable.privateBrowsingStateChanged(enable) })
    }

    /**
     * Registers an observer for private browsing changes
     *
     *
     * The observable will be called automatically when registered
     *
     * @param observable The observable to register
     */
    fun registerPrivateBrowsingObservable(observable: PrivateBrowsingObservable) {
        privateBrowsingObservables.add(observable)
        observable.privateBrowsingStateChanged(isUserLoggedInPrivatelyBrowsing())
    }

    /**
     * Unregisters an observer from the observers of private browsing changes
     *
     * @param observable The observable to remove
     */
    fun unregisterPrivateBrowsingObservable(observable: PrivateBrowsingObservable) {
        privateBrowsingObservables.remove(observable)
    }

    /**
     * Updates the theme (night mode) based on what is in the default SharedPreferences
     */
    fun updateTheme() {
        if (settings.getBoolean(getString(R.string.prefs_key_theme), resources.getBoolean(R.bool.prefs_default_theme))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Returns if NSFW videos/images should be cached
     *
     * @return True if videos/images should be cached
     */
    fun dontCacheNSFW(): Boolean {
        return !settings.getBoolean(applicationContext.getString(R.string.prefs_key_cache_nsfw), resources.getBoolean(R.bool.prefs_default_value_cache_nsfw))
    }

    /**
     * Returns if videos should be automatically played or not
     *
     *
     * The value returned here checks based on the setting and Wi-Fi status, so the value
     * can be used directly
     *
     * @return True if videos should be automatically played
     */
    fun autoPlayVideos(): Boolean {
        val value = settings.getString(
                getString(R.string.prefs_key_auto_play_videos),
                getString(R.string.prefs_default_value_auto_play_videos)
        )
        if (value == getString(R.string.prefs_key_auto_play_videos_always)) {
            return true
        } else if (value == getString(R.string.prefs_key_auto_play_videos_never)) {
            return false
        }

        // If we get here we are on Wi-Fi only, return true if Wi-Fi is connected, else false
        return wifiConnected
    }

    /**
     * Returns if NSFW videos should be automatically played or not.
     *
     * @return True if NSFW videos should be automatically played
     */
    fun autoPlayNsfwVideos(): Boolean {
        return settings.getBoolean(getString(R.string.prefs_key_auto_play_nsfw_videos), resources.getBoolean(R.bool.prefs_default_autoplay_nsfw_videos))
    }

    /**
     * Returns if videos should be muted by default
     *
     * @return True if the video should be muted
     */
    fun muteVideoByDefault(): Boolean {
        return settings.getBoolean(applicationContext.getString(R.string.prefs_key_play_muted_videos), resources.getBoolean(R.bool.prefs_default_play_muted_videos))
    }

    /**
     * Returns if videos should be muted by default when viewed in fullscreen
     *
     * @return True if the video should be muted
     */
    fun muteVideoByDefaultInFullscreen(): Boolean {
        return settings.getBoolean(
                applicationContext.getString(R.string.prefs_key_play_muted_videos_fullscreen),
                resources.getBoolean(R.bool.prefs_default_play_muted_videos_fullscreen)
        )
    }

    /**
     * Returns if videos should be automatically looped when finished
     *
     * @return True if videos should be looped
     */
    fun autoLoopVideos(): Boolean {
        return settings.getBoolean(
                applicationContext.getString(R.string.prefs_key_loop_videos),
                resources.getBoolean(R.bool.prefs_default_loop_videos)
        )
    }

    /**
     * Retrieves the score threshold for comments to be automatically hidden
     *
     * @return An int with the score threshold
     */
    fun getAutoHideScoreThreshold(): Int {
        // The value is stored as a string, not an int
        val defaultValue = resources.getInteger(R.integer.prefs_default_hide_comments_threshold)
        val value = settings.getString(
                getString(R.string.prefs_key_hide_comments_threshold), defaultValue.toString())
        return value!!.toInt()
    }

    /**
     * Retrieves the [SlidrConfig.Builder] to use for to slide away videos and images based
     * on the users setting. This also adjusts the threshold needed to perform a swipe. This builder can
     * be continued to set additional values
     *
     * @return A SlidrConfig builder that will build the SlidrConfig to be used for videos and images
     */
    fun getVideoAndImageSlidrConfig(): SlidrConfig.Builder {
        val direction = settings.getString(
                getString(R.string.prefs_key_fullscreen_swipe_direction),
                getString(R.string.prefs_default_fullscreen_swipe_direction)
        )
        // The SlidrPosition is kind of backwards, as SlidrPosition.BOTTOM means to "swipe from bottom" (which is swiping up)

        val pos = when (direction) {
            getString(R.string.prefs_key_fullscreen_swipe_direction_up) -> SlidrPosition.BOTTOM
            getString(R.string.prefs_key_fullscreen_swipe_direction_down) -> SlidrPosition.TOP
            else -> SlidrPosition.LEFT
        }

        // TODO this doesn't seem to work, if you start by going left, then it works, but otherwise it doesnt
        /*else {
            pos = SlidrPosition.RIGHT;
            Log.d(TAG, "getVideoAndImageSlidrConfig: RIGHT");
        }
         */
        return SlidrConfig.Builder().position(pos).distanceThreshold(0.15f)
    }

    /**
     * Retrieve the percentage of the screen a post should at maximum take when opened
     *
     * @return The percentage of the screen to take (0-100)
     */
    fun getMaxPostSizePercentage(): Int {
        return settings.getInt(
                getString(R.string.prefs_key_max_post_size_percentage),
                resources.getInteger(R.integer.prefs_default_max_post_size_percentage)
        )
    }

    /**
     * Retrieve the setting for whether or not links should be opened in the app (WebView) or
     * in the external browser
     *
     * @return True to open links in a WebView
     */
    fun openLinksInApp(): Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_opening_links_in_app),
                resources.getBoolean(R.bool.prefs_default_opening_links_in_app)
        )
    }

    /**
     * Checks if data saving is enabled, based on a combination of the users setting and current
     * Wi-Fi connection
     *
     * @return True if data saving is enabled
     */
    fun dataSavingEnabled(): Boolean {
        val value = settings.getString(
                getString(R.string.prefs_key_data_saving),
                getString(R.string.prefs_default_data_saving)
        )
        if (value == getString(R.string.prefs_key_data_saving_always)) {
            return true
        } else if (value == getString(R.string.prefs_key_data_saving_never)) {
            return false
        }

        // If we get here we are on Mobile Data only, return false if Wi-Fi is connected, else true
        return !wifiConnected
    }

    /**
     * Retrieves how NSFW images/thumbnails should be filtered
     *
     * @return An enum representing how to filter the images/thumbnails
     */
    fun showNsfwPreview(): ShowNsfwPreview {
        return when (settings.getString(getString(R.string.prefs_key_show_nsfw_preview), getString(R.string.prefs_default_show_nsfw))) {
            getString(R.string.prefs_key_show_nsfw_preview_normal) -> ShowNsfwPreview.NORMAL
            getString(R.string.prefs_key_show_nsfw_preview_blur) -> ShowNsfwPreview.BLURRED
            else -> ShowNsfwPreview.NO_IMAGE
        }
    }

    /**
     * Retrieves the user setting for if comments that have been posted after the last time a post was opened
     * should be highlighted
     *
     * @return True if comments should be highlighted
     */
    fun highlightNewComments(): Boolean {
        return settings.getBoolean(getString(R.string.prefs_key_highlight_new_comments), resources.getBoolean(R.bool.prefs_default_highlight_new_comments))
    }

    /**
     * Gets the comment threshold for when navigating in comments should smoothly scroll
     *
     * @return The max amount of comments for smooth scrolling to happen
     */
    fun commentSmoothScrollThreshold(): Int {
        return settings.getInt(getString(R.string.prefs_key_comment_smooth_scroll_threshold), resources.getInteger(R.integer.prefs_default_comment_smooth_scroll_threshold))
    }

    /**
     * Returns the array of subreddits the user has selected to filter from front page/popular/all
     *
     * @return An array of lowercased subreddit names
     * @see addSubredditToPostFilters
     */
    fun subredditsToFilterFromDefaultSubreddits(): Array<String> {
        val asString = settings.getString(getString(R.string.prefs_key_filter_posts_from_default_subreddits), "")
        return asString!!.toLowerCase().split("\n").toTypedArray()
    }

    /**
     * Adds a subreddit to the filters
     *
     * @param subreddit The name of the subreddit to add
     * @see subredditsToFilterFromDefaultSubreddits
     */
    fun addSubredditToPostFilters(subreddit: String) {
        var previous = settings.getString(getString(R.string.prefs_key_filter_posts_from_default_subreddits), "")
        // Only add newline before if necessary
        if (previous!!.isNotBlank() && previous.takeLast(1) != "\n") {
            previous += "\n"
        }
        previous += "$subreddit\n"

        settings.edit().putString(getString(R.string.prefs_key_filter_posts_from_default_subreddits), previous).apply()
    }

    /**
     * Returns if the user wants to show all sidebars, or just one colored at the last indent
     *
     * @return True to show all sidebars, false to show one colored
     */
    fun showAllSidebars(): Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_show_all_sidebars),
                resources.getBoolean(R.bool.prefs_default_show_all_sidebars)
        )
    }

    /**
     * Gets the integer value of the link scale. This should be divided by 100 to get the actual
     * float scale to use
     */
    fun linkScale() : Int {
        return settings.getInt(
                getString(R.string.prefs_key_link_scale),
                resources.getInteger(R.integer.prefs_default_link_scale)
        )
    }

    /**
     * Returns if the icon badge should be shown for unread messages in the navbar
     */
    fun showUnreadMessagesBadge() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_inbox_show_badge),
                resources.getBoolean(R.bool.prefs_default_inbox_show_badge)
        )
    }

    /**
     * Gets the update frequency for the inbox
     *
     * @return The update frequency in minutes, if this is -1 then the automatic updates are disabled
     */
    fun inboxUpdateFrequency() : Int {
        val updateFrequencySetting = settings.getString(
                getString(R.string.prefs_key_inbox_update_frequency),
                getString(R.string.prefs_default_inbox_update_frequency)
        )

        return when (updateFrequencySetting) {
            getString(R.string.prefs_key_inbox_update_frequency_5_min) -> 5
            getString(R.string.prefs_key_inbox_update_frequency_15_min) -> 15
            getString(R.string.prefs_key_inbox_update_frequency_30_min) -> 30
            getString(R.string.prefs_key_inbox_update_frequency_60_min) -> 50
            else -> -1
        }
    }

    /**
     * Returns if the user wants to show a preview of links in comments
     *
     * @see showEntireLinkInLinkPreview
     */
    fun showLinkPreview() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_comment_show_link_preview),
                resources.getBoolean(R.bool.prefs_default_comment_show_link_preview)
        )
    }

    /**
     * Returns if
     *
     * @see showLinkPreview
     */
    fun showEntireLinkInLinkPreview() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_comment_link_preview_show_entire_link),
                resources.getBoolean(R.bool.prefs_default_comment_link_preview_show_entire_link)
        )
    }


    /**
     * Sets the activity currently active. This is used to show a dialog on the rare occasion that
     * the user has revoked the applications access and a dialog must be shown that they must log in again
     *
     * @param activeActivity The activity currently active
     */
    fun setActiveActivity(activeActivity: Activity?) {
        this.activeActivity = activeActivity
    }

    /**
     * Handles when the API notifies that the access token is no longer valid, and the user should
     * be logged out and prompted to log back in.
     *
     * @param error The GenericError received
     * @param throwable The throwable received
     */
    private fun onInvalidAccessToken(error: GenericError, throwable: Throwable) {
        get().clearUserInfo()

        // Should we also recreate the app? We could have posts for the user, be in the profile etc

        // Storing an activity like this is probably very bad? What happens if we forget to use
        // setActiveActivity()? I don't know how else to overlay a dialog from an Application class
        activeActivity?.runOnUiThread {
            AlertDialog.Builder(activeActivity)
                    .setTitle(R.string.applicationAccessRevokedHeader)
                    .setMessage(R.string.applicationAccessRevokedContent)
                    .show()
        }
    }

    /**
     * Clears any user information stored, logging a user out. The application will be restarted
     */
    fun logOut() {
        // Clear shared preferences
        clearUserInfo()

        CoroutineScope(IO).launch {
            // Revoke token, the response to this never holds any data. If it fails we could potentially
            // store that it failed and retry again later
            TokenManager.getToken()?.let { api.accessToken().revoke(it) }

            // Clear any user specific state from database records (such as vote status on posts)
            RedditDatabase.getInstance(this@App).clearUserState()

            // Might be bad to just restart the app? Easiest way to ensure everything is reset though
            ProcessPhoenix.triggerRebirth(this@App)
        }
    }

    /**
     * Clears any information stored locally about a logged in user from SharedPreferences.
     *
     *
     * The preferences the user have chosen from the settings screen are not changed
     */
    private fun clearUserInfo() {
        TokenManager.removeToken()
        SharedPreferencesManager.removeNow(SharedPreferencesConstants.USER_INFO)
        SharedPreferencesManager.removeNow(SelectSubredditsViewModel.SUBSCRIBED_SUBREDDITS_KEY)
        settings.edit().remove(PRIVATELY_BROWSING_KEY).commit()
    }
}