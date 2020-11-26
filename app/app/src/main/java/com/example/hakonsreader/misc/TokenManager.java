package com.example.hakonsreader.misc;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.SharedPreferencesConstants;

import org.jetbrains.annotations.Nullable;

/**
 * Class for managing the access token for the logged in user
 */
public class TokenManager {
    private static AccessToken token;

    private TokenManager() {}


    /**
     * Retrieves the access token stored in the application
     *
     * @return The stored access token. If no token is stored (ie. no user logged in) null is returned
     */
    @Nullable
    public static AccessToken getToken() {
        // If null, try to get from shared preferences
        if (token == null) {
            token = SharedPreferencesManager.get(SharedPreferencesConstants.ACCESS_TOKEN, AccessToken.class);
        }

        return token;
    }

    /**
     * Saves the token to SharedPreferences and notifies the Reddit API instance of the change
     *
     * @param newToken The new token to save
     */
    public static void saveToken(AccessToken newToken) {
        token = newToken;

        // Save in shared prefs
        SharedPreferencesManager.put(SharedPreferencesConstants.ACCESS_TOKEN, newToken);
    }

    /**
     * Clears the stored token
     *
     * <p>This call removes the token from SharedPreferences immediately</p>
     */
    public static void removeToken() {
        token = new AccessToken();
        SharedPreferencesManager.removeNow(SharedPreferencesConstants.ACCESS_TOKEN);
    }
}
