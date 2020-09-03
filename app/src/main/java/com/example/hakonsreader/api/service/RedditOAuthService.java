package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Service towards Reddit's OAuth authentication
 */
public interface RedditOAuthService {
    /**
     * Retrieves the OAuth access token
     *
     * @param code The authorization token retrieved from the initial login process
     * @param grantType The string "authorization_code"
     * @param redirectUri The callback URL for the OAuth application
     * @return A Call object to be used to retrieve an access token
     */
    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: " + OAuthConstants.BASIC_AUTH})
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
    @Headers({"Authorization: " + OAuthConstants.BASIC_AUTH})
    @FormUrlEncoded
    Call<AccessToken> refreshToken(
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );

    /**
     * Revokes a token. Can be used for both access and refresh tokens. Revoking a refresh
     * token will also invalidate any access tokens.
     *
     * @param token The token to revoke
     * @param tokenType The token type, either "refresh_token" or "access_token"
     * @return A void Call. This API call only returns a status code to indicate if the request
     * was successful or not
     */
    @POST(NetworkConstants.REVOKE_TOKEN_PATH)
    @Headers({"Authorization: " + OAuthConstants.BASIC_AUTH})
    @FormUrlEncoded
    Call<Void> revokeToken(
            @Field("token") String token,
            @Field("token_type_hint") String tokenType
    );
}
