package com.example.hakonsreader.misc;

import android.content.SharedPreferences;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;

/**
 * Class for managing the access token for the logged in user. The token in this class
 * represents the token for the currently active user and is the one that should be used when
 * multiple users are stored on the device
 */
public class TokenManager {
    private static AccessToken token;

    private TokenManager() {}

    private static SharedPreferences prefs;
    private static Gson gson;


    public static void init(SharedPreferences prefs) {
        TokenManager.prefs = prefs;
        gson = new Gson();
    }

    /**
     * Retrieves the access token stored in the application
     *
     * @return The stored access token. If no token is stored (ie. no user logged in) null is returned
     */
    @Nullable
    public static AccessToken getToken() {
        // If null, try to get from shared preferences
        if (token == null) {
            token = gson.fromJson(prefs.getString(SharedPreferencesConstants.ACCESS_TOKEN, ""), AccessToken.class);
        }

        return token;
    }

    /**
     * Saves the token to SharedPreferences
     *
     * @param newToken The new token to save
     */
    public static void saveToken(AccessToken newToken) {
        token = newToken;

        String tokenAsJson = gson.toJson(newToken);

        prefs.edit()
                // Save the token as the active token in shared prefs
                .putString(SharedPreferencesConstants.ACCESS_TOKEN, tokenAsJson)
                // Save in general by its user ID
                .putString(newToken.getUserId(), tokenAsJson)
                .apply();
        SharedPreferencesManager.put(SharedPreferencesConstants.ACCESS_TOKEN, newToken);
    }

    /**
     * Clears the stored token
     *
     * <p>This call removes the token from SharedPreferences immediately</p>
     */
    public static void removeToken() {
        token = new AccessToken();
        prefs.edit()
                .remove(SharedPreferencesConstants.ACCESS_TOKEN)
                .apply();
    }

    /**
     * Removes a token by a given user ID
     */
    public static void removeTokenByUserId(String userId) {
        prefs.edit()
                .remove(userId)
                .apply();
    }

    /**
     * Attempts to retrieve a stored access token by a given user ID
     *
     * @param userId The user ID to retrieve the token for
     * @return The token if found, otherwise null
     */
    @Nullable
    public static AccessToken getTokenByUserId(String userId) {
        String tokenAsJson = prefs.getString(userId, null);
        if (tokenAsJson == null) {
            return null;
        }

        return gson.fromJson(tokenAsJson, AccessToken.class);
    }
}
