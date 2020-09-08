package com.example.hakonsreader.api.constants;

/**
 * Constants used for OAuth authentication
 */
public class OAuthConstants {

    public static final String TOKEN_TYPE_REFRESH = "refresh_token";

    /**
     * The grant_type used to retrieve access tokens from the initial login process
     */
    public static final String GRANT_TYPE_AUTHORIZATION = "authorization_code";

    /**
     * The grant_type used to refresh access tokens
     */
    public static final String GRANT_TYPE_REFRESH = "refresh_token";
}
