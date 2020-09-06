package com.example.hakonsreader.api.model;


import com.example.hakonsreader.misc.SharedPreferencesManager;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing an OAuth access token from Reddit
 */
public class AccessToken {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private int expiresIn;

    private long retrievedAt;

    private String scope;

    @SerializedName("refresh_token")
    private String refreshToken;


    /**
     * Sets the unix timestamp of when the access token was retrieved
     *
     * @param unixTimestamp The timestamp the token was retrieved
     */
    public void setRetrievedAt(long unixTimestamp) {
        this.retrievedAt = unixTimestamp;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Generates a string that can be used directly in an authorization header
     *
     * @return "tokenType tokenValue". If either the type or value is null an empty string is returned
     */
    public String generateHeaderString() {
        String ret;
        if (this.tokenType == null || this.accessToken == null) {
            ret = "";
        } else {
            ret = this.tokenType + " " + this.accessToken;
        }

        return ret;
    }


    @Override
    public String toString() {
        return "AccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", retrievedAt=" + retrievedAt +
                ", scope='" + scope + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }

    /**
     * @return The access token stored in SharedPreferences
     */
    public static AccessToken getStoredToken() {
        return SharedPreferencesManager.get(SharedPreferencesConstants.ACCESS_TOKEN, AccessToken.class);
    }

    /**
     * Stores a token in SharedPreferences
     *
     * @param token The token to store
     */
    public static void storeToken(AccessToken token) {
        SharedPreferencesManager.put(SharedPreferencesConstants.ACCESS_TOKEN, token);
    }
}
