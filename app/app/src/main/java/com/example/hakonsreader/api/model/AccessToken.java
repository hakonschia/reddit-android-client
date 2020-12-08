package com.example.hakonsreader.api.model;


import com.google.gson.annotations.SerializedName;

/**
 * Class representing an OAuth access token from Reddit
 */
public class AccessToken {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    private String scope;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("device_id")
    private String deviceId;


    /**
     * Sets the refresh token for this token
     *
     * @param refreshToken The token to use to refresh the access token when expired
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }


    /**
     * @return The actual token this {@link AccessToken} represents
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return The type of token this is
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * @return The token used to refresh the token value when expired
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * @return The amount of seconds the token is valid for. The value returned does not say the timestamp
     * the token expires, but how many seconds it is valid for from when the token was retrieved
     */
    public int getExpiresIn() {
        return expiresIn;
    }

    /**
     * @return The device ID the token is for. This is only applicable for access tokens for non-logged in
     *      * users. See <a href="https://github.com/reddit-archive/reddit/wiki/OAuth2#application-only-oauth">Reddit OAuth documentation</a> for more information
     */
    public String getDeviceId() {
        return deviceId;
    }


    /**
     * Returns the space separated string of the scopes the access token is valid for
     *
     * @return A string
     * @see #getScopesAsArray()
     */
    public String getScope() {
        return scope;
    }

    /**
     * Retrieves an array representation of the scopes the access token is valid for.
     *
     * @return An array of the scopes
     * @see #getScope()
     */
    public String[] getScopesAsArray() {
        return scope.split(" ");
    }


    /**
     * Generates a string that can be used directly in an authorization header
     *
     * @return "tokenType tokenValue". If either the type or value is null an empty string is returned
     */
    public String generateHeaderString() {
        if (tokenType == null || accessToken == null) {
            return  "";
        } else {
            return tokenType + " " + accessToken;
        }
    }


    @Override
    public String toString() {
        return "AccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", scope='" + scope + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", deviceID='" + deviceId + '\'' +
                '}';
    }
}
