package com.example.hakonsreader.constants;

public class OAuthConstants {

    /**
     * URL used for OAuth authentication with Reddit
     * <p>URL parameters needed: client_id, response_type, state, redirect_uri, duration, scope</p>
     *
     * @see <a href="https://github.com/reddit-archive/reddit/wiki/OAuth2#authorization">Reddit documentation</a>
     */
    public static final String REDDIT_OAUTH_URL = "https://www.reddit.com/api/v1/authorize";

    /**
     * Client ID for OAuth
     *
     */
    public static final String CLIENT_ID = "UZ37q9US0H2EoQ";

    /**
     * The callback URL used for OAuth authorization
     */
    public static final String CALLBACK_URL = "hakonreader://callback";

    public static final String RESPONSE_TYPE = "code";

    public static final String DURATION = "permanent";

    public static final String SCOPE = "identity";
}
