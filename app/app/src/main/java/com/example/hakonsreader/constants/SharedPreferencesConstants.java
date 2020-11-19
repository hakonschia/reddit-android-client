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
     * The key for access tokens from the default SharedPreferences
     */
    public static final String ACCESS_TOKEN = "accessToken";

    /**
     * The key for user information from the default SharedPreferences
     */
    public static final String USER_INFO = "userInfo";

    /**
     * The key used to store information about when a post was last opened. This key is a general
     * key and the post ID should be a part of the key as well to differentiate the different post.
     *
     * <p>The value stored should be a Unix timestamp.</p>
     */
    public static final String POST_LAST_OPENED_TIMESTAMP = "postLastOpenedTimestamp";
}
