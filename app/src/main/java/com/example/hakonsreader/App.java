package com.example.hakonsreader;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.hakonsreader.api.RedditApi;
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


    @Override
    public void onCreate() {
        super.onCreate();

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        SharedPreferences prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferencesManager.create(prefs);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean(SharedPreferencesConstants.NIGHT_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setupRedditApi();
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
}
