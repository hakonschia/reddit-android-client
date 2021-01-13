package com.example.hakonsreader.constants;

import com.example.hakonsreader.BuildConfig;

public class NetworkConstants {

    /**
     * User-Agent header sent to Reddit
     */
    public static final String USER_AGENT = "android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)";

    /**
     * Client ID for OAuth
     */
    public static final String CLIENT_ID = BuildConfig.REDDIT_CLIENT_ID;

    /**
     * The callback URL used for OAuth authorization
     */
    public static final String CALLBACK_URL = "hakonreader://callback";

    /**
     * Client ID for Imgur OAuth application
     */
    public static final String IMGUR_CLIENT_ID = BuildConfig.IMGUR_CLIENT_ID;



    /**
     * The response type to retrieve authorization tokens
     */
    public static final String RESPONSE_TYPE = "code";

    /**
     * The duration of the OAuth access token
     */
    public static final String DURATION = "permanent";

    /**
     * A space separated string containing the OAuth scopes needed for the application
     */
    public static final String SCOPE = "identity read vote submit privatemessages mysubreddits subscribe history flair edit save account modposts";
}
