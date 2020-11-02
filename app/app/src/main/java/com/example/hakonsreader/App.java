package com.example.hakonsreader;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.api.utils.MarkdownAdjuster;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.markwonplugins.LinkPlugin;
import com.example.hakonsreader.markwonplugins.SuperscriptPlugin;
import com.example.hakonsreader.markwonplugins.ThemePlugin;
import com.example.hakonsreader.misc.OAuthStateGenerator;
import com.example.hakonsreader.markwonplugins.RedditLinkPlugin;
import com.example.hakonsreader.markwonplugins.RedditSpoilerPlugin;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrPosition;

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
        unregisterReceiver(wifiStateReceiver);
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
                 .loggerLevel(HttpLoggingInterceptor.Level.BODY)
                 .callbackUrl(NetworkConstants.CALLBACK_URL)
                 .deviceId(UUID.randomUUID().toString())
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
     * Generates a new OAuth state that is used for validation
     *
     * @return A random string to use in the request for access
     */
    public String generateAndGetOAuthState() {
        oauthState = OAuthStateGenerator.generate();
        return oauthState;
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
        if (settings.getBoolean(getApplicationContext().getString(R.string.prefs_key_theme), getResources().getBoolean(R.bool.prefs_default_theme))) {
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
        Context context = getApplicationContext();
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
     * Retrieves the {@link SlidrConfig.Builder} to use for to slide away videos and images based
     * on the users setting. This also adjusts the threshold needed to perform a swipe. This builder can
     * be continued to set additional values
     *
     * @return The SlidrConfig that should be used for videos and images
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
}
