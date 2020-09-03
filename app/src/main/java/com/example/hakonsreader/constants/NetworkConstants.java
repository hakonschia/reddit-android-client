package com.example.hakonsreader.constants;

/**
 * Various networking  constants such as header information, URLs, and API access points
 */
public class NetworkConstants {

    /**
     * User-Agent header sent to Reddit
     */
    public static final String USER_AGENT = "android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)";

    /**
     * The URL for reddit.com
     */
    public static final String REDDIT_URL = "https://reddit.com/";

    /**
     *
     * The standard Reddit API URL.
     * <p>Do not use this for calls that should be authenticated with OAuth, see {@link NetworkConstants.REDDIT_OUATH_API_URL}</p>
     */
    public static final String REDDIT_API_URL = "https://www.reddit.com/api/";

    /**
     * The OAuth subdomain URL for Reddit.
     * <p>This is used to retrieve posts from reddit</p>
     */
    public static final String REDDIT_OUATH_URL = "https://oauth.reddit.com/";

    /**
     * The Reddit API URL used when authenticated with OAuth.
     * <p>This is used when making calls on behalf of a user with their access token</p>
     */
    public static final String REDDIT_OUATH_API_URL = REDDIT_OUATH_URL + "api/";



    /* --------------------- API paths --------------------- */
    /**
     * The API path used to retrieve an access token
     */
    public static final String ACCESS_TOKEN_PATH = "v1/access_token";

    /**
     * The API path used to retrieve user information
     */
    public static final String USER_INFO_PATH = "v1/me";
}
