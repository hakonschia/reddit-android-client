package com.example.hakonsreader.constants;

/**
 * Constants used for OAuth authentication
 */
public class OAuthConstants {

    /**
     * Client ID for OAuth
     *
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
     * A space separated string containing the OAuth scopes available
     */
    public static final String SCOPE = "identity read";


    /**
     * The grant_type used to authorize users (in the initial login process)
     */
    public static final String GRANT_TYPE_AUTHORIZATION = "authorization_code";

    /**
     * The grant_type used to refresh access tokens
     */
    public static final String GRANT_TYPE_REFRESH = "refresh_token";
}
