package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Service towards Reddit's OAuth authentication
 */
public interface RedditOAuthService {
    /**
     * The API path used to retrieve an access token
     */
    String ACCESS_TOKEN_PATH = "v1/access_token";

    /**
     * The API path used to revoke access/refresh tokens
     */
    String REVOKE_TOKEN_PATH = "v1/revoke_token";


    /**
     * Retrieves the OAuth access token
     *
     * @param basicAuth The basic authorization header with the client ID and secret
     * @param code The authorization token retrieved from the initial login process
     * @param grantType The string "authorization_code"
     * @param redirectUri The callback URL for the OAuth application
     *
     * @return A Call object to be used to retrieve an access token
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    Call<AccessToken> getAccessToken(
            @Header("Authorization") String basicAuth,

            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri
    );

    /**
     * Refreshes the OAuth access token
     *
     * @param basicAuth The basic authorization header with the client ID and secret
     * @param refreshToken The refresh token received in the initial access token retrieval
     * @param grantType The grant type (refresh_token)
     *
     * @return A Call object to be used to refresh the access token
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    Call<AccessToken> refreshToken(
            @Header("Authorization") String basicAuth,

            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );

    /**
     * Revokes a token. Can be used for both access and refresh tokens. Revoking a refresh
     * token will also invalidate any access tokens.
     *
     * @param basicAuth The basic authorization header with the client ID and secret
     * @param token The token to revoke
     * @param tokenType The token type, either "refresh_token" or "access_token"
     *
     * @return A void Call. This API call only returns a status code to indicate if the request
     * was successful or not
     */
    @POST(REVOKE_TOKEN_PATH)
    @FormUrlEncoded
    Call<Void> revokeToken(
            @Header("Authorization") String basicAuth,

            @Field("token") String token,
            @Field("token_type_hint") String tokenType
    );
}
