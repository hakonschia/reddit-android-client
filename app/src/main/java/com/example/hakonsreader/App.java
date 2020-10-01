package com.example.hakonsreader;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.misc.OAuthStateGenerator;
import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.misc.TokenManager;

import java.util.UUID;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Entry point for the application. Sets up various static variables used throughout the app
 */
public class App extends Application {
    private static final String TAG = "App";

    /**
     * The width of the screen of the current device
     */
    private static int screenWidth;

    // The random string generated for OAuth authentication
    private static String oauthState;
    private static RedditApi redditApi;
    private static SharedPreferences settings;
    private static Context context;


    @Override
    public void onCreate() {
        super.onCreate();

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        SharedPreferences prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferencesManager.create(prefs);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        context = this;

        setupRedditApi();
        updateTheme();
    }


    /**
     * @return The width of the screen in pixels
     */
    public static int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Sets up the reddit API object
     */
    private void setupRedditApi() {
        // Set the previously stored token, and the listener for new tokens
        redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT, NetworkConstants.CLIENT_ID);
        redditApi.setToken(TokenManager.getToken());
        redditApi.setOnNewToken(TokenManager::saveToken);
        //redditApi.setLoggingLevel(HttpLoggingInterceptor.Level.BODY);
        redditApi.setCallbackURL(NetworkConstants.CALLBACK_URL);
        redditApi.setDeviceID(UUID.randomUUID().toString());
    }

    public static RedditApi getApi() {
        return redditApi;
    }

    /**
     * Retrieves the current OAuth state. To generate a new state use {@link App#generateAndGetOAuthState()}
     *
     * @return The current OAuth state
     */
    public static String getOAuthState() {
        return oauthState;
    }

    /**
     * Clears the OAuth state.
     *
     * <p>Use this when the state has been verified</p>
     */
    public static void clearOAuthState() {
        oauthState = null;
    }

    /**
     * Generates a new OAuth state that is used for validation
     *
     * @return A random string to use in the request for access
     */
    public static String generateAndGetOAuthState() {
        oauthState = OAuthStateGenerator.generate();
        return oauthState;
    }


    /**
     * Updates the theme (night mode) based on what is in the default SharedPreferences
     */
    public static void updateTheme() {
        if (settings.getBoolean(context.getString(R.string.prefs_key_theme), false)) {
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
    public static boolean cacheNSFW() {
        return settings.getBoolean(context.getString(R.string.prefs_key_cache_nsfw), false);
    }

    /**
     * Returns if videos should be automatically played or not
     *
     * @return True if videos should be automatically played
     */
    public static boolean autoPlayVideos() {
        return settings.getBoolean(context.getString(R.string.prefs_key_auto_play_videos), false);
    }

    /**
     * Returns if videos should be muted by default
     *
     * @return True if the video should be muted
     */
    public static boolean muteVideoByDefault() {
        return settings.getBoolean(context.getString(R.string.prefs_key_play_muted_videos), false);
    }
}
