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

    @SerializedName("expires_in")
    private int expiresIn;

    private String scope;

    @SerializedName("refresh_token")
    private String refreshToken;


    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
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
                ", scope='" + scope + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }
}
