package com.example.hakonsreader.misc

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.view.View
import androidx.preference.PreferenceManager
import com.example.hakonsreader.R
import com.example.hakonsreader.enums.ShowNsfwPreview
import com.example.hakonsreader.misc.Settings.init
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.views.preferences.multicolor.MultiColorFragCompat
import com.google.android.material.snackbar.Snackbar
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import java.util.*
import kotlin.collections.ArrayList


/**
 * The preferences for the application. The preferences must be initialized with [init] before used, otherwise
 * an exception will be thrown
 */
object Settings {

    private var isWifiConnected = false

    // There doesn't seem to be an issue storing Resources in a static field (like with Context)
    private lateinit var resources: Resources
    private lateinit var preferences: SharedPreferences


    /**
     * Initializes the preferences. This should only be called during startup.
     *
     * @param context The context to create the preferences with. The default shared preferences from this
     * context will be used, as well as its resources
     */
    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        resources = context.resources

        registerNetworkCallbacks(context)
    }

    /**
     * Force check if WiFi is available, which is used for data saving settings.
     *
     * @param context The context to use to retrieve a [ConnectivityManager]
     */
    fun forceWiFiCheck(context: Context) {
        isWifiConnected = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .isWiFiAvailable()
    }

    /**
     * Registers network callbacks that modify [isWifiConnected] with API level in mind
     */
    private fun registerNetworkCallbacks(context: Context) {
        // Might have to be calling unregisterNetworkCallback to not cause leaks here
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {

            // onAvailable and onLost are called for any network, not just WiFi, so when one of them
            // is triggered we check if WiFi is still here

            override fun onAvailable(network: Network) {
                isWifiConnected = cm.isWiFiAvailable()
            }

            override fun onLost(network: Network) {
                isWifiConnected = cm.isWiFiAvailable()
            }
        }

        if (Build.VERSION.SDK_INT >= 24) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            cm.registerNetworkCallback(request, callback)
        }
    }

    /**
     * Returns if NSFW videos/images should be cached
     *
     * @return True if videos/images should be cached
     */
    fun cacheNsfw(): Boolean {
        return preferences.getBoolean(resources.getString(R.string.prefs_key_cache_nsfw), resources.getBoolean(R.bool.prefs_default_value_cache_nsfw))
    }

    /**
     * Returns if videos should be automatically played or not
     *
     * The value returned here checks based on the setting and data saving, so the value
     * can be used directly
     */
    fun autoPlayVideos(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_auto_play_videos_switch),
                resources.getBoolean(R.bool.prefs_default_value_auto_play_videos)
        ) && !dataSavingEnabled()
    }

    /**
     * Returns if NSFW videos should be automatically played or not.
     *
     * @return True if NSFW videos should be automatically played
     */
    fun autoPlayNsfwVideos(): Boolean {
        // If it's an NSFW account we want to use the normal auto play
        return if (AppState.getUserInfo()?.nsfwAccount == true) {
            autoPlayVideos()
        } else {
            preferences.getBoolean(
                resources.getString(R.string.prefs_key_auto_play_nsfw_videos),
                resources.getBoolean(R.bool.prefs_default_autoplay_nsfw_videos)
            ) && !dataSavingEnabled()
        }
    }

    /**
     * Returns if videos should be muted by default
     *
     * @return True if the video should be muted
     */
    fun muteVideosByDefault(): Boolean {
        return preferences.getBoolean(resources.getString(R.string.prefs_key_play_muted_videos), resources.getBoolean(R.bool.prefs_default_play_muted_videos))
    }

    /**
     * Returns if videos should be muted by default when viewed in fullscreen
     *
     * @return True if the video should be muted
     */
    fun muteVideoByDefaultInFullscreen(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_play_muted_videos_fullscreen),
                resources.getBoolean(R.bool.prefs_default_play_muted_videos_fullscreen)
        )
    }

    /**
     * Returns if videos should be automatically looped when finished
     *
     * @return True if videos should be looped
     */
    fun autoLoopVideos(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_loop_videos),
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
        val value = preferences.getString(
                resources.getString(R.string.prefs_key_hide_comments_threshold), defaultValue.toString())
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
        val direction = preferences.getString(
                resources.getString(R.string.prefs_key_fullscreen_swipe_direction),
                resources.getString(R.string.prefs_default_fullscreen_swipe_direction)
        )
        // The SlidrPosition is kind of backwards, as SlidrPosition.BOTTOM means to "swipe from bottom" (which is swiping up)

        val pos = when (direction) {
            resources.getString(R.string.prefs_key_fullscreen_swipe_direction_up) -> SlidrPosition.BOTTOM
            resources.getString(R.string.prefs_key_fullscreen_swipe_direction_down) -> SlidrPosition.TOP
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
        return preferences.getInt(
                resources.getString(R.string.prefs_key_max_post_size_percentage),
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
        return preferences.getInt(
                resources.getString(R.string.prefs_key_max_post_size_percentage_when_collapsed),
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
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_opening_links_in_app),
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
        val value = preferences.getString(
                resources.getString(R.string.prefs_key_data_saving),
                resources.getString(R.string.prefs_default_data_saving)
        )

        return when (value) {
            resources.getString(R.string.prefs_key_data_saving_always) -> true
            resources.getString(R.string.prefs_key_data_saving_never) -> false
            // If we get here we are on Mobile Data only, return false if Wi-Fi is connected, else true
            else -> !isWifiConnected
        }
    }

    /**
     * Retrieves how NSFW images/thumbnails should be filtered
     *
     * @return An enum representing how to filter the images/thumbnails
     */
    fun showNsfwPreview(): ShowNsfwPreview {
        if (AppState.getUserInfo()?.nsfwAccount == true) {
            return ShowNsfwPreview.NORMAL
        }

        return when (preferences.getString(resources.getString(R.string.prefs_key_show_nsfw_preview), resources.getString(R.string.prefs_default_show_nsfw))) {
            resources.getString(R.string.prefs_key_show_nsfw_preview_normal) -> ShowNsfwPreview.NORMAL
            resources.getString(R.string.prefs_key_show_nsfw_preview_blur) -> ShowNsfwPreview.BLURRED
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
        return preferences.getBoolean(resources.getString(R.string.prefs_key_highlight_new_comments), resources.getBoolean(R.bool.prefs_default_highlight_new_comments))
    }

    /**
     * Gets the comment threshold for when navigating in comments should smoothly scroll
     *
     * @return The max amount of comments for smooth scrolling to happen
     */
    fun commentSmoothScrollThreshold(): Int {
        return preferences.getInt(resources.getString(R.string.prefs_key_comment_smooth_scroll_threshold), resources.getInteger(R.integer.prefs_default_comment_smooth_scroll_threshold))
    }

    /**
     * Returns the array of subreddits the user has selected to filter from front page/popular/all
     *
     * @return An array of lowercased subreddit names
     * @see addSubredditToPostFilters
     */
    fun subredditsToFilterFromDefaultSubreddits(): Array<String> {
        val asString = preferences.getString(resources.getString(R.string.prefs_key_filter_posts_from_default_subreddits), "")
        return asString!!.toLowerCase(Locale.ROOT).split("\n").toTypedArray()
    }

    /**
     * Adds a subreddit to the filters
     *
     * @param subreddit The name of the subreddit to add
     * @param view To show a snackbar pass the view to attach the snackbar to here
     *
     * @see subredditsToFilterFromDefaultSubreddits
     */
    fun addSubredditToPostFilters(subreddit: String, view: View?) {
        val previous = preferences.getString(resources.getString(R.string.prefs_key_filter_posts_from_default_subreddits), "")!!

        val subs = previous.split("\n").toMutableList()

        if (!subs.contains(subreddit)) {
            subs.add(subreddit)

            // Mostly if the user has manually entered empty lines we can remove those now
            val asString = subs.filter { it.isNotBlank() }.joinToString(separator = "\n")

            preferences.edit()
                .putString(resources.getString(R.string.prefs_key_filter_posts_from_default_subreddits), asString)
                .apply()
        }

        if (view != null) {
            val snackbarString = view.context.getString(R.string.filterSubredditsSnackbar, subreddit)
            Snackbar.make(view, snackbarString, Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Returns if the user wants to show all sidebars, or just one colored at the last indent
     *
     * @return True to show all sidebars, false to show one colored
     */
    fun showAllSidebars(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_show_all_sidebars),
                resources.getBoolean(R.bool.prefs_default_show_all_sidebars)
        )
    }

    /**
     * Gets the integer value of the link scale. This should be divided by 100 to get the actual
     * float scale to use
     */
    fun linkScale(): Int {
        return preferences.getInt(
                resources.getString(R.string.prefs_key_link_scale),
                resources.getInteger(R.integer.prefs_default_link_scale)
        )
    }

    /**
     * Returns if the icon badge should be shown for unread messages in the navbar
     */
    fun showUnreadMessagesBadge(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_inbox_show_badge),
                resources.getBoolean(R.bool.prefs_default_inbox_show_badge)
        )
    }

    /**
     * Gets the update frequency for the inbox
     *
     * @return The update frequency in minutes, if this is -1 then the automatic updates are disabled
     */
    fun inboxUpdateFrequency(): Int {
        val updateFrequencySetting = preferences.getString(
                resources.getString(R.string.prefs_key_inbox_update_frequency),
                resources.getString(R.string.prefs_default_inbox_update_frequency)
        )

        return when (updateFrequencySetting) {
            resources.getString(R.string.prefs_key_inbox_update_frequency_15_min) -> 15
            resources.getString(R.string.prefs_key_inbox_update_frequency_30_min) -> 30
            resources.getString(R.string.prefs_key_inbox_update_frequency_60_min) -> 60
            else -> -1
        }
    }

    /**
     * Returns if the user wants to show a preview of links in comments
     *
     * @see showEntireLinkInLinkPreview
     */
    fun showLinkPreview(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_comment_show_link_preview),
                resources.getBoolean(R.bool.prefs_default_comment_show_link_preview)
        )
    }

    /**
     * Returns if the entire link should be shown in link previews, or if it should cut off
     * to save layout space
     *
     * @see showLinkPreview
     */
    fun showEntireLinkInLinkPreview(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_comment_link_preview_show_entire_link),
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
    fun showLinkPreviewForIdenticalLinks(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_comment_link_preview_show_identical_links),
                resources.getBoolean(R.bool.prefs_default_comment_link_preview_show_identical_links)
        )
    }

    /**
     * Returns if posts should automatically be collapsed when scrolling comments
     */
    fun collapsePostsByDefaultWhenScrollingComments(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_post_collapse_by_default),
                resources.getBoolean(R.bool.prefs_default_post_collapse_by_default)
        )
    }

    /**
     * @return True if YouTube videos should be opened directly in the app, false to open in YouTube
     * app/browser
     */
    fun openYouTubeVideosInApp(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_play_youtube_videos_in_app),
                resources.getBoolean(R.bool.prefs_default_play_youtube_videos_in_app)
        )
    }

    /**
     * @return True if comments should show a button directly in the layout that peeks the parent comment
     */
    fun showPeekParentButtonInComments(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_show_peek_parent_button_in_comments),
                resources.getBoolean(R.bool.prefs_default_show_peek_parent_button_in_comments)
        )
    }

    /**
     * @return True if banners should be loaded on subreddits. This does not take into account data saving
     */
    fun loadSubredditBanners(): Boolean {
        return preferences.getBoolean(
                resources.getString(R.string.prefs_key_subreddits_load_banners),
                resources.getBoolean(R.bool.prefs_default_subreddits_load_banners)
        )
    }

    /**
     * @return True if a warning should be displayed when opening NSFW subreddits
     */
    fun warnNsfwSubreddits(): Boolean {
        return AppState.getUserInfo()?.nsfwAccount != true && preferences.getBoolean(
                resources.getString(R.string.prefs_key_subreddits_warn_nsfw),
                resources.getBoolean(R.bool.prefs_default_subreddits_warn_nsfw)
        )
    }

    /**
     * @return The list of parsed colors that should be used for comment sidebars (where the index
     * of the color corresponds to the comment depth)
     */
    fun commentSidebarColors(): List<Int> {
        val colors = ArrayList<Int>()
        MultiColorFragCompat.getColors(preferences, resources.getString(R.string.prefs_key_comment_sidebar_colors)).forEach {
            colors.add(Color.parseColor("#$it"))
        }
        return colors
    }

    /**
     * @return True if the user wants to display notifications for inbox messages
     */
    fun showInboxNotifications(): Boolean {
        return preferences.getBoolean(
            resources.getString(R.string.prefs_key_inbox_show_notifications),
            resources.getBoolean(R.bool.prefs_default_inbox_show_notifications)
        )
    }

    /**
     * @return True if awards should be shown on posts and comments
     */
    fun showAwards(): Boolean {
        return preferences.getBoolean(
            resources.getString(R.string.prefs_key_show_awards),
            resources.getBoolean(R.bool.prefs_default_show_awards)
        )
    }

    /**
     * @return True if the button to open the subreddit info drawer should be shown. This checks
     * the system version code
     */
    fun showSubredditInfoButton(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            return true
        }

        return preferences.getBoolean(
            resources.getString(R.string.prefs_key_show_subreddit_info_button),
            resources.getBoolean(R.bool.prefs_default_show_subreddit_info_button)
        )
    }



    // ----------------------- Developer settings -----------------------

    /**
     * @return True if [AppState.isDevMode] is true and developer inbox notifications should be shown
     */
    fun devShowInboxNotifications(): Boolean {
        return AppState.isDevMode && preferences.getBoolean(
            resources.getString(R.string.prefs_key_dev_show_inbox_notification),
            resources.getBoolean(R.bool.prefs_default_dev_show_inbox_notification)
        )
    }

    /**
     * @return True if [AppState.isDevMode] is true and selected posts should be highlighted
     */
    fun devHighlightSelectedPosts(): Boolean {
        return AppState.isDevMode && preferences.getBoolean(
            resources.getString(R.string.prefs_key_dev_highlight_selected_views),
            resources.getBoolean(R.bool.prefs_default_dev_highlight_selected_views)
        )
    }

    /**
     * @return True if [AppState.isDevMode] is true and there should be shown content info on long presses
     * on a post
     */
    fun devShowContentInfoOnLongPress(): Boolean {
        return AppState.isDevMode && preferences.getBoolean(
            resources.getString(R.string.prefs_key_dev_show_content_info_on_long_press),
            resources.getBoolean(R.bool.prefs_default_dev_show_content_info_on_long_press)
        )
    }

}