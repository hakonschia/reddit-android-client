package com.example.hakonsreader

import android.app.*
import android.content.*
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.hakonsreader.activities.InvalidAccessTokenActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.utils.MarkdownAdjuster
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.markwonplugins.*
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.TokenManager
import com.jakewharton.processphoenix.ProcessPhoenix
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import com.squareup.picasso.Picasso
import io.noties.markwon.*
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.logging.HttpLoggingInterceptor
import org.commonmark.node.Image
import java.io.File
import java.security.SecureRandom
import java.util.*

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

        const val NOTIFICATION_CHANNEL_INBOX_ID = "notificationChannelInbox"

        /**
         * Retrieve the instance of the application that can be used to access various methods
         *
         * @return The static App instance
         */
        fun get(): App {
            return app
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
     * @return The [RedditApi] object to use for API calls
     */
    val api: RedditApi by lazy {
        // This requires SharedPreferencesManager to be set, so lazy initialize it
        createApi()
    }

    /**
     * A [RedditDatabase] instance
     */
    val database: RedditDatabase by lazy {
        RedditDatabase.getInstance(this)
    }

    /**
     * A [RedditUserInfoDatabase] instance
     */
    val userInfoDatabase: RedditUserInfoDatabase by lazy {
        RedditUserInfoDatabase.getInstance(this)
    }

    /**
     * The [Markwon] object used to format markdown text. This object has custom plugins for Reddit
     * specific markdown, and defines a custom theme
     */
    val markwonNoDataSaving: Markwon by lazy {
        // This requires the resources to be set, so lazy initialize it
        createMarkwon()
    }

    /**
     * The [Markwon] object used to format markdown text. This object has custom plugins for Reddit
     * specific markdown, and defines a custom theme. This will not look for images to inline, so
     * if images aren't going to be inlined this should be used over [markwon] to avoid some unnecessary
     * processing
     */
    val markwonDataSaving: Markwon by lazy {
        // This requires the resources to be set, so lazy initialize it
        createDataSavingMarkwon()
    }

    /**
     * Gets the markwon object to use based on if data saving is enabled
     *
     * @see markwonNoDataSaving
     * @see markwonDataSaving
     */
    val markwon: Markwon
        get() {
            return if (dataSavingEnabled()) {
                markwonDataSaving
            } else {
                markwonNoDataSaving
            }
        }

    /**
     * An instance of a [MarkdownAdjuster]. This instance checks the following:
     * * Header spaces
     * * URL encoding in Markdown links
     * * Image conversions for inline images
     *
     * @see adjuster
     * @see adjusterDataSaving
     */
    val adjusterNoDataSaving: MarkdownAdjuster = createMarkdownAdjuster()

    /**
     * An instance of a [MarkdownAdjuster] for when data saving is enabled. This instance checks the following:
     * * Header spaces
     * * URL encoding in Markdown links
     *
     * @see adjuster
     * @see adjusterNoDataSaving
     */
    val adjusterDataSaving: MarkdownAdjuster = createMarkdownAdjusterDataSaving()

    /**
     * The markdown adjuster based on if data saving is enabled. When data saving is enabled
     * images shouldn't be inlined, so the image link adjustment is skipped to not perform unnecessary
     * processing
     *
     * @see adjusterNoDataSaving
     * @see adjusterDataSaving
     */
    val adjuster: MarkdownAdjuster
        get() {
            return if (dataSavingEnabled()) {
                adjusterDataSaving
            } else {
                adjusterNoDataSaving
            }
        }

    /**
     * The user info for the user that is currently active in the application (this will not be
     * nulled if private browsing is enabled)
     */
    var currentUserInfo: RedditUserInfo? = null

    private val _privatelyBrowsing = MutableLiveData<Boolean>()

    /**
     * A LiveData observable for the state of the APIs private browsing context
     */
    val privatelyBrowsing: LiveData<Boolean> = _privatelyBrowsing

    override fun onCreate() {
        super.onCreate()
        set()

        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE)
        SharedPreferencesManager.create(prefs)

        CoroutineScope(IO).launch {
            // There should probably be no issue with retrieving the user info in this way
            // It should happen fast enough to be set before it is used
            TokenManager.getToken()?.run {
                val userId = this.userId
                if (userId != AccessToken.NO_USER_ID) {
                    currentUserInfo = userInfoDatabase.userInfo().getById(userId)
                }
            }

            // Remove records that are older than 2 days, as they likely won't be used again
            val maxAge = 60L * 60 * 24 * 2
            val count = database.posts().getCount()
            val deleted = database.posts().deleteOld(maxAge)

            Log.d(TAG, "onCreate: # of records=$count; # of deleted=$deleted")
        }

        createInboxNotificationChannel()

        val dm = resources.displayMetrics
        // Technically this could go outdated if the user changes their resolution while the app is running
        // but I highly doubt that would ever be a problem (worst case is posts wouldn't fit the screen)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels

        updateTheme()
    }

    /**
     * Sets the app instance
     */
    private fun set() {
        app = this
    }

    private fun createInboxNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notificationChannelInbox)
            val descriptionText = getString(R.string.notificationChannelInboxDescription)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(NOTIFICATION_CHANNEL_INBOX_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
                .usePlugin(PicassoImagesPlugin.create(Picasso.get()))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                        builder.appendFactory(Image::class.java) { configuration, props ->

                            // this is the destination of image, you can additionally process it
                            val url = ImageProps.DESTINATION.require(props)

                            LinkSpan(
                                    configuration.theme(),
                                    url,
                                    // Not sure how this is supposed to work as even if I define my own custom
                                    // resolver it still uses the default
                                    // Might have something to do with how I set the LinkMovementMethod?
                                    // I want to open the image with a SharedElementTransition like other images
                                    // Might be impossible since it seems as the images are actually a TextView, not
                                    // ImageView (from layout inspector)
                                    configuration.linkResolver()
                            )
                        }
                    }
                })

                // Custom plugins
                .usePlugin(RedditSpoilerPlugin())
                .usePlugin(RedditLinkPlugin())
                .usePlugin(SuperscriptPlugin())
                .usePlugin(LinkPlugin())
                .usePlugin(EnlargeLinkPlugin())
                .usePlugin(ThemePlugin(this))
                .build()
    }

    private fun createDataSavingMarkwon(): Markwon {
        return Markwon.builder(this)
                // Headers, blockquote etc. are a part of the core
                .usePlugin(CorePlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(StrikethroughPlugin.create())

                // Custom plugins
                .usePlugin(RedditSpoilerPlugin())
                .usePlugin(RedditLinkPlugin())
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
        // 25MB cache size for network requests to third party
        val thirdPartyCacheSize = 25 * 1024 * 1024L
        val thirdPartyCache = Cache(File(cacheDir, "third_party_http_cache"), thirdPartyCacheSize)

        // 1 week cache size (this cache size could really be as long as time itself, the mutable ata
        // in the requests aren't used anyways)
        val thirdPartyCacheAge = 60 * 60 * 24 * 7L

        // If the Imgur client ID is omitted from secrets.properties it is parsed as a string with the value "null"
        val imgurClientId = if (NetworkConstants.IMGUR_CLIENT_ID != "null") {
            NetworkConstants.IMGUR_CLIENT_ID
        } else null

        val privatelyBrowsing = settings.getBoolean(PRIVATELY_BROWSING_KEY, false)

        return RedditApi(
                userAgent = NetworkConstants.USER_AGENT,
                clientId = NetworkConstants.CLIENT_ID,

                accessToken = TokenManager.getToken(),
                onNewToken = { newToken -> onNewToken(newToken) },
                onInvalidToken = { _: GenericError?, _: Throwable? -> onInvalidAccessToken() },

                loggerLevel = HttpLoggingInterceptor.Level.BODY,

                callbackUrl = NetworkConstants.CALLBACK_URL,
                deviceId = UUID.randomUUID().toString(),
                imgurClientId = imgurClientId,

                thirdPartyCache = thirdPartyCache,
                thirdPartyCacheAge = thirdPartyCacheAge
        ).apply {
            enablePrivateBrowsing(privatelyBrowsing)
        }
    }

    /**
     * Switches which account is the active account. The app will be restarted
     *
     * @param token The token to use for the new active account
     */
    fun switchAccount(token: AccessToken) {
        CoroutineScope(IO).launch {
            // Ensure no user state from one account is used for the new one
            database.clearUserState()

            api.switchAccessToken(token)
            TokenManager.saveTokenNow(token)

            // The API also changes this (although it is recreated so it doesn't really matter)
            // The observers also don't have to be notified since everything is recreated
            settings.edit().putBoolean(PRIVATELY_BROWSING_KEY, false).commit()

            ProcessPhoenix.triggerRebirth(this@App)
        }
    }

    /**
     * Gets a [RedditUserInfo] object corresponding to an access token and sets the object
     * to [currentUserInfo]. If the token is for the same account as [currentUserInfo] is now, then
     * [currentUserInfo] is returned as is. Otherwise the value stored in the database is retrieved,
     * or a new one is created.
     *
     * Note that the object will not be modified in any way (ie. [token] is not set on the object
     * automatically)
     */
    // Database operations must be suspended
    @Suppress("RedundantSuspendModifier")
    private suspend fun getAndSetCurrentUserInfo(token: AccessToken) : RedditUserInfo {
        val current = currentUserInfo
        return if (current != null) {
            val currentId = current.accessToken.userId
            // The current user is for the same user, return it
            if (currentId != AccessToken.NO_USER_ID && currentId == token.userId) {
                current
            } else {
                // Either get from the database, or create a new one
                userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token).also {
                    currentUserInfo = it
                }
            }
        } else {
            // currentUserInfo == null, Either get from the database, or create a new one
            userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token).also {
                currentUserInfo = it
            }
        }

    }

    /**
     * Callback for when new access tokens are received. This will save the token to [TokenManager]
     * and update [currentUserInfo] with the token, setting a new object on the variable if needed
     * to correctly represent the user the token is for
     *
     * @param token The new token
     */
    private fun onNewToken(token: AccessToken) {
        TokenManager.saveToken(token)
        if (token.userId != AccessToken.NO_USER_ID) {
            CoroutineScope(IO).launch {
                getAndSetCurrentUserInfo(token).apply {
                    accessToken = token

                    // New token is for a user
                    if (token.userId != AccessToken.NO_USER_ID) {
                        userInfoDatabase.userInfo().insert(this)
                    }
                }
            }
        }
    }

    /**
     * Saves user information to [currentUserInfo] and updates the local database.
     * Pass parameters to this function to update the relevant values.
     *
     * @param info The information about the user
     * @param subreddits The list of subreddit IDs the user is subscribed to
     */
    fun updateUserInfo(info: RedditUser? = null, subreddits: List<String>? = null, nsfwAccount: Boolean? = null) {
        CoroutineScope(IO).launch {
            TokenManager.getToken()?.let {
                getAndSetCurrentUserInfo(it).apply {
                    if (info != null) {
                        userInfo = info
                    }
                    if (subreddits != null) {
                        subscribedSubreddits = subreddits
                    }
                    if (nsfwAccount != null) {
                        this.nsfwAccount = nsfwAccount
                    }

                    if (userInfoDatabase.userInfo().userExists(it.userId)) {
                        userInfoDatabase.userInfo().update(this)
                    } else {
                        userInfoDatabase.userInfo().insert(this)
                    }
                }
            }
        }
    }

    /**
     * Returns a new instance of a [MarkdownAdjuster]
     */
    private fun createMarkdownAdjuster() : MarkdownAdjuster {
        return MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .checkUrlEncoding()
                .convertImageLinksToMarkdown()
                .build()
    }

    /**
     * Returns a new instance of a [MarkdownAdjuster]
     */
    private fun createMarkdownAdjusterDataSaving() : MarkdownAdjuster {
        return MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .checkUrlEncoding()
                .build()
    }

    /**
     * Clears the OAuth state.
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
        return isUserLoggedIn() && api.isPrivatelyBrowsing()
    }

    /**
     * Enable or disable the APIs private browsing and stores locally that private browsing is enabled/disabled
     *
     * @param enable True to enable private browsing, false to disable
     */
    fun enablePrivateBrowsing(enable: Boolean) {
        api.enablePrivateBrowsing(enable)
        settings.edit().putBoolean(PRIVATELY_BROWSING_KEY, enable).apply()

        // To be certain it doesn't crash if this is called from a background thread
        _privatelyBrowsing.postValue(enable)
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
        return currentUserInfo?.nsfwAccount == true ||
                settings.getBoolean(getString(R.string.prefs_key_auto_play_nsfw_videos), resources.getBoolean(R.bool.prefs_default_autoplay_nsfw_videos))
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
     * Retrieve the percentage of the screen a post should at maximum take when opened and post
     * collapse has been disabled
     *
     * @return The percentage of the screen to take (0-100)
     */
    fun getMaxPostSizePercentageWhenCollapseDisabled(): Int {
        return settings.getInt(
                getString(R.string.prefs_key_max_post_size_percentage_when_collapsed),
                resources.getInteger(R.integer.prefs_default_max_post_size_percentage_when_collapsed)
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
        if (currentUserInfo?.nsfwAccount == true) {
            return ShowNsfwPreview.NORMAL
        }

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
     * Returns if the entire link should be shown in link previews, or if it should cut off
     * to save layout space
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
     * Returns if links with identical text should show a preview for the link
     *
     * Eg. if this returns false, a hyperlink with text "https://reddit.com" and link "https://reddit.com"
     * should not display a preview
     *
     * @see showLinkPreview
     */
    fun showLinkPreviewForIdenticalLinks() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_comment_link_preview_show_identical_links),
                resources.getBoolean(R.bool.prefs_default_comment_link_preview_show_identical_links)
        )
    }

    /**
     * Returns if posts should automatically be collapsed when scrolling comments
     */
    fun collapsePostsByDefaultWhenScrollingComments() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_post_collapse_by_default),
                resources.getBoolean(R.bool.prefs_default_post_collapse_by_default)
        )
    }

    /**
     * @return True if YouTube videos should be opened directly in the app, false to open in YouTube
     * app/browser
     */
    fun openYouTubeVideosInApp() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_play_youtube_videos_in_app),
                resources.getBoolean(R.bool.prefs_default_play_youtube_videos_in_app)
        )
    }

    /**
     * @return True if comments should show a button directly in the layout that peeks the parent comment
     */
    fun showPeekParentButtonInComments() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_show_peek_parent_button_in_comments),
                resources.getBoolean(R.bool.prefs_default_show_peek_parent_button_in_comments)
        )
    }

    /**
     * @return True if banners should be loaded on subreddits. This does not take into account data saving
     */
    fun loadSubredditBanners() : Boolean {
        return settings.getBoolean(
                getString(R.string.prefs_key_subreddits_load_banners),
                resources.getBoolean(R.bool.prefs_default_subreddits_load_banners)
        )
    }

    /**
     * @return True if a warning should be displayed when opening NSFW subreddits
     */
    fun warnNsfwSubreddits() : Boolean {
        return currentUserInfo?.nsfwAccount != true && settings.getBoolean(
                getString(R.string.prefs_key_subreddits_warn_nsfw),
                resources.getBoolean(R.bool.prefs_default_subreddits_warn_nsfw)
        )
    }

    /**
     * Handles when the API notifies that the access token is no longer valid, and the user should
     * be logged out and prompted to log back in.
     */
    private fun onInvalidAccessToken() {
        logOut()

        Intent(this, InvalidAccessTokenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
        }
    }

    /**
     * Clears any user information stored, logging a user out. The application will be restarted
     */
    fun logOut() {
        CoroutineScope(IO).launch {
            // Revoke token, the response to this never holds any data. If it fails we could potentially
            // store that it failed and retry again later
            TokenManager.getToken()?.let { api.accessToken().revoke(it) }

            // Clear any user specific state from database records (such as vote status on posts)
            database.clearUserState()

            currentUserInfo?.let {
                userInfoDatabase.userInfo().delete(it)
            }

            settings.edit().remove(PRIVATELY_BROWSING_KEY).commit()
            TokenManager.removeToken()

            // Might be bad to just restart the app? Easiest way to ensure everything is reset though
            ProcessPhoenix.triggerRebirth(this@App)
        }
    }
}