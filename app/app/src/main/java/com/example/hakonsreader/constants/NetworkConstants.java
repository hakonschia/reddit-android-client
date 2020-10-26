package com.example.hakonsreader.constants;

public class NetworkConstants {

    /**
     * User-Agent header sent to Reddit
     */
    public static final String USER_AGENT = "android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)";

    /**
     * Client ID for OAuth
     */
    public static final String CLIENT_ID = "UZ37q9US0H2EoQ";

    /**
     * The callback URL used for OAuth authorization
     */
    public static final String CALLBACK_URL = "hakonreader://callback";



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
    public static final String SCOPE = "identity read vote submit mysubreddits subscribe history flair";
}
