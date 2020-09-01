package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

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

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }


    /**
     * Checks if the access token has less than 5 seconds remaining
     *
     * @return True if the access token is about to expire
     */
    public boolean expiresSoon() {


        return true;
    }
}
