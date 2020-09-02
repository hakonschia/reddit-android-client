package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.NetworkConstants;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Service towards Reddit's OAuth authentication
 */
public interface RedditOauthService {
    /**
     * Retrieves the OAuth access token
     *
     * @param code The authorization token retrieved from the initial login process
     * @param grantType The string "authorization_code"
     * @param redirectUri The callback URL for the OAuth application
     * @return A Call object to be used to retrieve an access token
     */
    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6"})
    @FormUrlEncoded
    Call<AccessToken> getAccessToken(
            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri
    );

    /**
     * Refreshes the OAuth access token
     *
     * @param refreshToken The refresh token received in the initial access token retrieval
     * @param grantType The grant type (refresh_token)
     * @return A Call object to be used to refresh the access token
     */
    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6"})
    @FormUrlEncoded
    Call<AccessToken> refreshToken(
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );
}
