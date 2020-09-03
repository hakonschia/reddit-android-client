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
     * The header value for basic authentication.
     * <p>The value is generated as: base64(clientId + ":"), but can't be generated at runtime</p>
     * <p>This is used when retrieving OAuth access tokens</p>
     */
    public static final String BASIC_AUTH = "Basic VVozN3E5VVMwSDJFb1E6";

    public static final String TOKEN_TYPE_ACCESS = "access_token";

    public static final String TOKEN_TYPE_REFRESH = "refresh_token";

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
    public static final String SCOPE = "identity read vote";


    /**
     * The grant_type used to authorize users (in the initial login process)
     */
    public static final String GRANT_TYPE_AUTHORIZATION = "authorization_code";

    /**
     * The grant_type used to refresh access tokens
     */
    public static final String GRANT_TYPE_REFRESH = "refresh_token";
}
