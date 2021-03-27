package com.example.hakonsreader

import android.app.*
import android.content.*
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.hakonsreader.activities.InvalidAccessTokenActivity
import com.example.hakonsreader.activities.MainActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.utils.MarkdownAdjuster
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.markwonplugins.*
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.TokenManager
import com.example.hakonsreader.states.LoggedInState
import com.squareup.picasso.Picasso
import dagger.hilt.android.HiltAndroidApp
import io.noties.markwon.*
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.logging.HttpLoggingInterceptor
import org.commonmark.node.Image
import java.io.File
import java.security.SecureRandom
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


/**
 * Entry point for the application
 */
@HiltAndroidApp
class App : Application() {

    companion object {
        private const val TAG = "App"

        /**
         * The key used in SharedPreferences to store if the API should be in private browsing from startup
         */
        private const val PRIVATELY_BROWSING_KEY = "privatelyBrowsing"
        const val NOTIFICATION_CHANNEL_INBOX_ID = "notificationChannelInbox"


        private lateinit var app: App
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
     * The default [SharedPreferences] that holds the users settings
     */
    private val settings by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }


    /**
     * A [RedditApi] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var api: RedditApi

    /**
     * A [RedditDatabase] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var database: RedditDatabase

    /**
     * A [RedditUserInfoDatabase] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var userInfoDatabase: RedditUserInfoDatabase

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
            return if (Settings.dataSavingEnabled()) {
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
            return if (Settings.dataSavingEnabled()) {
                adjusterDataSaving
            } else {
                adjusterNoDataSaving
            }
        }

    private val _loggedInState = MutableLiveData<LoggedInState>()

    /**
     * A LiveData observable for the user state of the application. This will update either if
     * some user info was updated for the current user, the current user account changed, or if the
     * current user logged out
     */
    val loggedInState: LiveData<LoggedInState> = _loggedInState

    override fun onCreate() {
        super.onCreate()
        set()
        Settings.init(this)

        val privatelyBrowsing = settings.getBoolean(PRIVATELY_BROWSING_KEY, false)
        api.enablePrivateBrowsing(privatelyBrowsing)

        val token = TokenManager.getToken()

        // We have a token, and it is for a user
        val state = if (token != null && token.userId != AccessToken.NO_USER_ID) {

            // This database allows for main thread queries, since this has to be set before anything
            // is started to ensure that the state is correct (this might be kind of bad, but the query
            // should be fast enough that it doesn't impact startup by any noticeable amount)
            val info = userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token)
            if (api.isPrivatelyBrowsing()) {
                LoggedInState.PrivatelyBrowsing(info)
            } else {
                LoggedInState.LoggedIn(info)
            }
        } else {
            LoggedInState.LoggedOut
        }
        _loggedInState.value = state

        CoroutineScope(IO).launch {
            // Remove records that are older than 2 days, as they likely won't be used again
            val maxAge = 60L * 60 * 24 * 2
            val count = database.posts().getCount()
            val deleted = database.posts().deleteOld(maxAge)

            Log.d(TAG, "onCreate: # of records=$count; # of deleted=$deleted")

            // Calling this from the Coroutine because then we can pretend to be smart about performance :)
            removeOldPostOpenedPreferences()
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
     * Removes keys older than 2 weeks from the SharedPreferences holding the opened post values
     */
    private fun removeOldPostOpenedPreferences() {
        // Having these keys in SharedPreferences is kind of weird, but it's way easier than creating
        // a database for it, so it works I guess

        val twoWeeksAgo = (System.currentTimeMillis() / 1000L) - 60 * 60 * 24 * 14
        val keysToRemove = ArrayList<String>()

        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME_POST_OPENED, MODE_PRIVATE)

        val sizeBefore = prefs.all.size
        prefs.all.forEach { (key, value) ->
            value as Long

            if (twoWeeksAgo > value) {
                keysToRemove.add(key)
            }
        }

        val editor = prefs.edit()

        keysToRemove.forEach {
            editor.remove(it)
        }

        editor.apply()
        Log.d(TAG, "removeOldPostOpenedPreferences: removed ${keysToRemove.size} keys from $sizeBefore total")
    }

    /**
     * Gets the current user info, or null if there is no user logged in.
     *
     * Note this can be null even with a logged in user and should not be used as verification for
     * the logged in state. Use [loggedInState] for that.
     */
    fun getUserInfo() : RedditUserInfo? {
        return when (val state = _loggedInState.value) {
            is LoggedInState.LoggedIn -> state.userInfo
            is LoggedInState.PrivatelyBrowsing -> state.userInfo
            else -> null
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
     * Switches which account is the active account.
     *
     * @param token The token to use for the new active account
     * @param activity The activity currently active. The activity will be recreated
     */
    fun switchAccount(token: AccessToken, activity: AppCompatActivity) {
        CoroutineScope(IO).launch {
            // Ensure no user state from one account is used for the new one
            database.clearUserState()

            api.switchAccessToken(token)
            TokenManager.saveToken(token)

            settings.edit().putBoolean(PRIVATELY_BROWSING_KEY, false).commit()

            withContext(Main) {
                _loggedInState.value = LoggedInState.LoggedIn(getUserInfoFromToken(token))

                if (activity is MainActivity) {
                    activity.recreateAsNewUser()
                } else {
                    activity.recreate()
                }
            }
        }
    }

    /**
     * Gets a [RedditUserInfo] object corresponding to an access token.
     */
    // Database operations must be suspended
    @Suppress("RedundantSuspendModifier")
    private suspend fun getUserInfoFromToken(token: AccessToken) : RedditUserInfo {
        return userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token)
    }

    /**
     * Callback for when new access tokens are received. This will save the token to [TokenManager]
     *
     * @param token The new token
     */
    fun onNewToken(token: AccessToken) {
        TokenManager.saveToken(token)
        if (token.userId != AccessToken.NO_USER_ID) {
            CoroutineScope(IO).launch {
                getUserInfoFromToken(token).apply {
                    accessToken = token

                    // New token is for a user
                    if (userInfoDatabase.userInfo().userExists(userId)) {
                        userInfoDatabase.userInfo().update(this)
                    } else {
                        userInfoDatabase.userInfo().insert(this)
                    }
                }
            }
        }
    }

    /**
     * Adds a new user
     */
    // Database operations must be suspended
    @Suppress("RedundantSuspendModifier")
    suspend fun addNewUser(token: AccessToken) {
        TokenManager.saveToken(token)
        val userInfo = RedditUserInfo(token)

        withContext(Main) {
            _loggedInState.value = LoggedInState.LoggedIn(userInfo)
        }

        userInfoDatabase.userInfo().insert(userInfo)
    }

    /**
     * Saves user info and updates the local database for the currently logged in user. This uses
     * the value from [loggedInState] to determine the user
     *
     * Pass parameters to this function to update the relevant values.
     *
     * @param info The information about the user
     * @param subreddits The list of subreddit IDs the user is subscribed to
     */
    suspend fun updateUserInfo(info: RedditUser? = null, subreddits: List<String>? = null, nsfwAccount: Boolean? = null) {
        val state = loggedInState.value
        val userInfo = when (state) {
            is LoggedInState.LoggedIn -> state.userInfo
            is LoggedInState.PrivatelyBrowsing -> state.userInfo
            else -> return
        }

        if (info != null) {
            userInfo.userInfo = info
        }
        if (subreddits != null) {
            userInfo.subscribedSubreddits = subreddits
        }
        if (nsfwAccount != null) {
            userInfo.nsfwAccount = nsfwAccount
        }

        withContext(Main) {
            // The state should not change, just update the information
            if (state is LoggedInState.LoggedIn) {
                _loggedInState.value = LoggedInState.LoggedIn(userInfo)
            } else {
                _loggedInState.value = LoggedInState.PrivatelyBrowsing(userInfo)
            }
        }

        if (userInfoDatabase.userInfo().userExists(userInfo.userId)) {
            userInfoDatabase.userInfo().update(userInfo)
        } else {
            userInfoDatabase.userInfo().insert(userInfo)
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
     * Toggles private browsing. This is just a convenience method for [enablePrivateBrowsing] with
     * null passed as the argument
     */
    fun togglePrivateBrowsing() {
        enablePrivateBrowsing()
    }

    /**
     * Enable or disable the APIs private browsing and stores locally that private browsing is enabled/disabled
     *
     * @param enable True to enable private browsing, false to disable. Use null to toggle
     */
    fun enablePrivateBrowsing(enable: Boolean? = null) {
        val enableActual = enable ?: !api.isPrivatelyBrowsing()

        api.enablePrivateBrowsing(enableActual)
        settings.edit().putBoolean(PRIVATELY_BROWSING_KEY, enableActual).apply()

        // When enabling/disabling private browsing we *should* have a user, but if not return
        val userInfo = getUserInfo() ?: return
        val state = if (enableActual) {
            LoggedInState.PrivatelyBrowsing(userInfo)
        } else {
            LoggedInState.LoggedIn(userInfo)
        }

        _loggedInState.postValue(state)
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
     * Handles when the API notifies that the access token is no longer valid, and the user should
     * be logged out and prompted to log back in.
     */
    fun onInvalidAccessToken() {
        logOut()

        Intent(this, InvalidAccessTokenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
        }
    }

    /**
     * Clears any user information stored, logging a user out. The application will be restarted
     *
     * @param context The context requesting the log out. If this is a [MainActivity] then [MainActivity.recreateAsNewUser]
     * will be called. Otherwise, if it is an activity then [AppCompatActivity.recreate] will be called
     */
    fun logOut(context: Context? = null) {
        CoroutineScope(IO).launch {
            // Revoke token, the response to this never holds any data. If it fails we could potentially
            // store that it failed and retry again later
            TokenManager.getToken()?.let { api.accessToken().revoke(it) }

            api.logOut()

            // Clear any user specific state from database records (such as vote status on posts)
            database.clearUserState()

            getUserInfo()?.let {
                userInfoDatabase.userInfo().delete(it)
            }

            settings.edit().remove(PRIVATELY_BROWSING_KEY).commit()
            TokenManager.removeToken()

            withContext(Main) {
                // We could potential look for other users stored here, but we might not want to
                // assume which account to use then, so it's better to just let the user choose later
                _loggedInState.value = LoggedInState.LoggedOut

                if (context is MainActivity) {
                    context.recreateAsNewUser()
                } else if (context is AppCompatActivity) {
                    context.recreate()
                }
            }

        }
    }
}