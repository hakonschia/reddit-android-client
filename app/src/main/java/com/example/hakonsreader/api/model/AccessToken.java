package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

public class AccessToken {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private String expiresIn;

    private String scope;

    @SerializedName("refresh_token")
    private String refreshToken;

}
