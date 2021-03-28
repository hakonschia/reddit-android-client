package com.example.hakonsreader.constants;

/**
 * Various constants used for SharedPreferences
 */
public class SharedPreferencesConstants {

    /**
     * The name of the default SharedPreferences
     */
    public static final String PREFS_NAME = "preferences";

    /**
     * The name of the SharedPreferences storing the post opened values
     */
    public static final String PREFS_NAME_POST_OPENED = "preferencesPostOpened";


    /**
     * The key for access tokens from the default SharedPreferences
     */
    public static final String ACCESS_TOKEN = "accessToken";

    /**
     * The key to store if the app is browsing privately in the SharedPreferences
     */
    public static final String PRIVATELY_BROWSING = "privatelyBrowsing";

    /**
     * The key used to store information about when a post was last opened. This key is a general
     * key and the post ID should be a part of the key as well to differentiate the different post.
     *
     * <p>The value stored should be a Unix timestamp.</p>
     */
    public static final String POST_LAST_OPENED_TIMESTAMP = "postLastOpenedTimestamp";

    /**
     * The key used to store the string of access tokens checked, to decide if the dialog
     * for "New permissions required" should be shown or not
     */
    public static final String ACCESS_TOKEN_SCOPES_CHECKED = "accessTokenScopesChecked";
}
