package com.example.hakonsreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.utils.MarkdownAdjuster;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.markwonplugins.LinkPlugin;
import com.example.hakonsreader.markwonplugins.SuperscriptPlugin;
import com.example.hakonsreader.markwonplugins.ThemePlugin;
import com.example.hakonsreader.markwonplugins.RedditLinkPlugin;
import com.example.hakonsreader.markwonplugins.RedditSpoilerPlugin;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel;
import com.facebook.stetho.Stetho;
import com.jakewharton.processphoenix.ProcessPhoenix;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import okhttp3.logging.HttpLoggingInterceptor;


/**
 * Entry point for the application. Sets up various static variables used throughout the app
 */
public class App extends Application {
    private static final String TAG = "App";

    /**
     * The width of the screen of the current device
     */
    private int screenWidth;
    /**
     * The height of the screen of the current device
     */
    private int screenHeight;

    // The random string generated for OAuth authentication
    private String oauthState;
    private RedditApi redditApi;
    private SharedPreferences settings;
    private boolean wifiConnected;

    private Markwon markwon;
    private MarkdownAdjuster adjuster;

    private Activity activeActivity;

    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        set();

        DisplayMetrics dm = getResources().getDisplayMetrics();

        // Technically this could go outdated if the user changes their resolution while the app is running
        // but I highly doubt that would ever be a problem (worst case is posts wouldn't fit the screen)
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        SharedPreferences prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferencesManager.create(prefs);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setupRedditApi();
        updateTheme();

        AppDatabase db = AppDatabase.getInstance(this);

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }

        // Remove records that are older than 12 hours, as they likely won't be used again
        new Thread(() -> {
            long maxAge = (long)60 * 60 * 12;

            int count = db.posts().getCount();
            int deleted = db.posts().deleteOld(maxAge);

            Log.d(TAG, "onCreate: # of records=" + count + "; # of deleted=" + deleted);
        }).start();
    }

    /**
     * Unregisters any receivers that have been registered
     */
    public void registerReceivers() {
        // Register Wi-Fi broadcast receiver to be notified when Wi-Fi is enabled/disabled
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
    }

    /**
     * Unregisters any receivers that have been registered
     */
    public void unregisterReceivers() {
        // The receiver might not have been registered, but since there isn't a way to check if
        // a receiver has been registered this will do
        try {
            unregisterReceiver(wifiStateReceiver);
        } catch (IllegalArgumentException ignored) {
            // Ignored
        }
    }

    /**
     * Broadcast receiver for Wi-Fi state
     */
    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                boolean connected = info.isConnected();

                wifiConnected = connected;
            }
        }
    };


    /**
     * Sets the app instance
     */
    private synchronized void set() {
        app = this;
    }

    /**
     * Retrieve the instance of the application that can be used to access various methods
     *
     * @return The static App instance
     */
    public static App get() {
        return app;
    }

    /**
     * @return The width of the screen in pixels
     */
    public int getScreenWidth() {
        return screenWidth;
    }
    /**
     * @return The height of the screen in pixels
     */
    public int getScreenHeight() {
        return screenHeight;
    }


    /**
     * Sets up {@link App#redditApi}
     */
    private void setupRedditApi() {
         redditApi = new RedditApi.Builder(NetworkConstants.USER_AGENT, NetworkConstants.CLIENT_ID)
                 .accessToken(TokenManager.getToken())
                 .onNewToken(TokenManager::saveToken)
                 //.loggerLevel(HttpLoggingInterceptor.Level.BODY)
                 .callbackUrl(NetworkConstants.CALLBACK_URL)
                 .deviceId(UUID.randomUUID().toString())
                 .onInvalidToken(this::onInvalidAccessToken)
                 .loadImgurAlbumsAsRedditGalleries(NetworkConstants.IMGUR_CLIENT_ID)
                 .build();
    }

    /**
     * @return The {@link RedditApi} object to use for API calls
     */
    public RedditApi getApi() {
        return redditApi;
    }

    /**
     * Retrieves the current OAuth state. To generate a new state use {@link App#generateAndGetOAuthState()}
     *
     * @return The current OAuth state
     */
    public String getOAuthState() {
        return oauthState;
    }

    /**
     * Clears the OAuth state.
     *
     * <p>Use this when the state has been verified</p>
     */
    public void clearOAuthState() {
        oauthState = null;
    }

    /**
     * Generates a random string to use for OAuth requests
     *
     * @return A new random string
     */
    private String generateOauthState() {
        final String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        Random rnd = new SecureRandom();
        StringBuilder state = new StringBuilder();

        for (int i = 0; i < 35; i++) {
            state.append(characters.charAt(rnd.nextInt(characters.length())));
        }

        return state.toString();
    }

    /**
     * Generates a new OAuth state that is used for validation
     *
     * @return A random string to use in the request for access
     */
    public String generateAndGetOAuthState() {
        oauthState = generateOauthState();
        return oauthState;
    }

    /**
     * Checks if there currently is a user logged in
     *
     * @return True if there is a user logged in
     */
    public boolean isUserLoggedIn() {
        // Only logged in users have a refresh token
        return TokenManager.getToken().getRefreshToken() != null;
    }

    /**
     * Retrieve the {@link Markwon} object that is used to format markdown text
     *
     * <p>The object has various plugins set, such as support for tables</p>
     *
     * @return A {@link Markwon} object used to format text in TextViews with markdown text
     */
    public Markwon getMark() {
        if (markwon == null) {
            markwon = createMark();
        }

        return markwon;
    }

    /**
     * Creates a {@link Markwon} object with plugins set
     *
     * @return A {@link Markwon} object ready to format some markdown :)
     */
    private Markwon createMark() {
        return Markwon.builder(this)
                // Headers, blockquote etc. are a part of the core
                .usePlugin(CorePlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(StrikethroughPlugin.create())

                // Custom plugins
                .usePlugin(new RedditSpoilerPlugin())
                .usePlugin(new RedditLinkPlugin(this))
                .usePlugin(new SuperscriptPlugin())
                .usePlugin(new LinkPlugin(this))
                .usePlugin(new ThemePlugin(this))
                .build();
    }

    /**
     * Retrieves the markdown adjuster
     *
     * <p>The adjuster only checks for missing header spaces</p>
     *
     * @return The markdown adjuster
     */
    public MarkdownAdjuster getAdjuster() {
        if (adjuster == null) {
            adjuster = new MarkdownAdjuster.Builder()
                    .checkHeaderSpaces()
                    .build();
        }

        return adjuster;
    }

    /**
     * Updates the theme (night mode) based on what is in the default SharedPreferences
     */
    public void updateTheme() {
        if (settings.getBoolean(getString(R.string.prefs_key_theme), getResources().getBoolean(R.bool.prefs_default_theme))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Returns if NSFW videos/images should be cached
     *
     * @return True if videos/images should be cached
     */
    public boolean dontCacheNSFW() {
        return !settings.getBoolean(getApplicationContext().getString(R.string.prefs_key_cache_nsfw), getResources().getBoolean(R.bool.prefs_default_value_cache_nsfw));
    }

    /**
     * Returns if videos should be automatically played or not
     *
     * <p>The value returned here checks based on the setting and Wi-Fi status, so the value
     * can be used directly</p>
     *
     * @return True if videos should be automatically played
     */
    public boolean autoPlayVideos() {
        String value = settings.getString(
                getString(R.string.prefs_key_auto_play_videos),
                getString(R.string.prefs_default_value_auto_play_videos)
        );

        if (value.equals(getString(R.string.prefs_key_auto_play_videos_always))) {
            return true;
        } else if (value.equals(getString(R.string.prefs_key_auto_play_videos_never))) {
            return false;
        }

        // If we get here we are on Wi-Fi only, return true if Wi-Fi is connected, else false
        return wifiConnected;
    }


    /**
     * Returns if videos should be muted by default
     *
     * @return True if the video should be muted
     */
    public boolean muteVideoByDefault() {
        return settings.getBoolean(getApplicationContext().getString(R.string.prefs_key_play_muted_videos), getResources().getBoolean(R.bool.prefs_default_play_muted_videos));
    }

    /**
     * Returns if videos should be muted by default when viewed in fullscreen
     *
     * @return True if the video should be muted
     */
    public boolean muteVideoByDefaultInFullscreen() {
        return settings.getBoolean(
                getApplicationContext().getString(R.string.prefs_key_play_muted_videos_fullscreen),
                getResources().getBoolean(R.bool.prefs_default_play_muted_videos_fullscreen)
        );
    }

    /**
     * Returns if videos should be automatically looped when finished
     *
     * @return True if videos should be looped
     */
    public boolean autoLoopVideos() {
        return settings.getBoolean(
                getApplicationContext().getString(R.string.prefs_key_loop_videos),
                getResources().getBoolean(R.bool.prefs_default_loop_videos)
        );
    }

    /**
     * Retrieves the score threshold for comments to be automatically hidden
     *
     * @return An int with the score threshold
     */
    public int getAutoHideScoreThreshold() {
        // The value is stored as a string, not an int

        int defaultValue = getResources().getInteger(R.integer.prefs_default_hide_comments_threshold);
        String value = settings.getString(
                getString(R.string.prefs_key_hide_comments_threshold),
                String.valueOf(defaultValue)
        );

        return Integer.parseInt(value);
    }

    /**
     * Retrieves the {@link SlidrConfig.Builder} to use for to slide away videos and images based
     * on the users setting. This also adjusts the threshold needed to perform a swipe. This builder can
     * be continued to set additional values
     *
     * @return A SlidrConfig builder that will build the SlidrConfig to be used for videos and images
     */
    public SlidrConfig.Builder getVideoAndImageSlidrConfig() {
        String direction = settings.getString(
                getString(R.string.prefs_key_fullscreen_swipe_direction),
                getString(R.string.prefs_default_fullscreen_swipe_direction)
        );

        SlidrPosition pos;

        // The SlidrPosition is kind of backwards, as SlidrPosition.BOTTOM means to "swipe from bottom" (which is swiping up)
        if (direction.equals(getString(R.string.prefs_key_fullscreen_swipe_direction_up))) {
            pos = SlidrPosition.BOTTOM;
        } else if (direction.equals(getString(R.string.prefs_key_fullscreen_swipe_direction_down))) {
            pos = SlidrPosition.TOP;
        } else {
            pos = SlidrPosition.LEFT;
        }
        // TODO this doesn't seem to work, if you start by going left, then it works, but otherwise it doesnt
        /*else {
            pos = SlidrPosition.RIGHT;
            Log.d(TAG, "getVideoAndImageSlidrConfig: RIGHT");
        }
         */
        return new SlidrConfig.Builder().position(pos).distanceThreshold(0.15f);
    }

    /**
     * Retrieve the percentage of the screen a post should at maximum take when opened
     *
     * @return The percentage of the screen to take (0-100)
     */
    public int getMaxPostSizePercentage() {
        return settings.getInt(
                getString(R.string.prefs_key_max_post_size_percentage),
                getResources().getInteger(R.integer.prefs_default_max_post_size_percentage)
        );
    }

    /**
     * Retrieve the setting for whether or not links should be opened in the app (WebView) or
     * in the external browser
     *
     * @return True to open links in a WebView
     */
    public boolean openLinksInApp() {
        return settings.getBoolean(
                getString(R.string.prefs_key_opening_links_in_app),
                getResources().getBoolean(R.bool.prefs_default_opening_links_in_app)
        );
    }

    /**
     * Sets the activity currently active. This is used to show a dialog on the rare occasion that
     * the user has revoked the applications access and a dialog must be shown that they must log in again
     *
     * @param activeActivity The activity currently active
     */
    public void setActiveActivity(Activity activeActivity) {
        this.activeActivity = activeActivity;
    }


    /**
     * @return Retrieves the user information stored in SharedPreferences
     */
    public static RedditUser getStoredUser() {
        return SharedPreferencesManager.get(SharedPreferencesConstants.USER_INFO, RedditUser.class);
    }

    /**
     * Stores information about a user in SharedPreferences
     *
     * @param user The object to store
     */
    public static void storeUserInfo(RedditUser user) {
        SharedPreferencesManager.put(SharedPreferencesConstants.USER_INFO, user);
    }

    /**
     * Handles when the API notifies that the access token is no longer valid, and the user should
     * be logged out and prompted to log back in.
     *
     * @param error The GenericError received
     * @param throwable The throwable received
     */
    private void onInvalidAccessToken(GenericError error, Throwable throwable) {
        App.get().clearUserInfo();

        // Should we also recreate the app? We could have posts for the user, be in the profile etc

        // Storing an activity like this is probably very bad? What happens if we forget to use
        // setActiveActivity()? I don't know how else to overlay a dialog from an Application class
        activeActivity.runOnUiThread(() -> {
            new AlertDialog.Builder(activeActivity)
                    .setTitle(R.string.applicationAccessRevokedHeader)
                    .setMessage(R.string.applicationAccessRevokedContent)
                    .show();
        });
    }

    /**
     * Clears any user information stored, logging a user out. The application will be restarted
     */
    public void logOut() {
        // Revoke token, the response to this never holds any data. If it fails we could potentially
        // store that it failed and retry again later
        redditApi.revokeRefreshToken(response -> { }, (call, t) -> { });

        // Clear shared preferences
        this.clearUserInfo();

        new Thread(() -> {
            // Clear any user specific state from database records (such as vote status on posts)
            AppDatabase.getInstance(this).clearUserState();

            // Might be bad to just restart the app? Easiest way to ensure everything is reset though
            ProcessPhoenix.triggerRebirth(this);
        }).start();
    }

    /**
     * Clears any information stored locally about a logged in user from SharedPreferences.
     *
     * <p>The preferences the user have chosen from the settings screen are not changed</p>
     */
    public void clearUserInfo() {
        TokenManager.removeToken();
        SharedPreferencesManager.removeNow(SharedPreferencesConstants.USER_INFO);
        SharedPreferencesManager.removeNow(SelectSubredditsViewModel.SUBSCRIBED_SUBREDDITS_KEY);
    }
}
