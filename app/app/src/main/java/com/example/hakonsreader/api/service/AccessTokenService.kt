package com.example.hakonsreader.api.service

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

import com.example.hakonsreader.api.model.AccessToken
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Header

/**
 * The endpoint to use for access token calls (retrieval, refresh)
 */
private const val ACCESS_TOKEN_PATH = "api/v1/access_token"


interface AccessTokenService {

    /**
     * Gets a new access token for a logged in user
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it
     *
     * @param code The authorization code received from the initial login process
     * @param redirectUri The callback URL for the OAuth application, as defined under reddit.com/prefs/apps
     * @param grantType The grant_type. This has a default value and shouldn't be modified
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ) : Response<AccessToken>

    /**
     * Gets a new access token for a non-logged in user
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it
     *
     * @param deviceId A random ID for the current device (can be "DO_NOT_TRACK_THIS_DEVICE")
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    suspend fun getAccessTokenNoUser(
            @Field("device_id") deviceId: String,
            @Field("grant_type") grantType: String = "https://oauth.reddit.com/grants/installed_client"
    ) : Response<AccessToken>

    /**
     * Gets a new access token for a non-logged in user. This is not suspended, and will block the current thread
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it
     *
     * @param deviceId A random ID for the current device (can be "DO_NOT_TRACK_THIS_DEVICE")
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    fun getAccessTokenNoUserNoSuspend(
            @Field("device_id") deviceId: String,
            @Field("grant_type") grantType: String = "https://oauth.reddit.com/grants/installed_client"
    ) : Call<AccessToken>


    /**
     * Refreshes an access token token
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it
     *
     * @param refreshToken The refresh token (as retrieved from [AccessToken.refreshToken])
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    suspend fun refreshToken(
            @Header("Authorization") basicAuth: String,
            @Field("refresh_token") refreshToken: String,
            @Field("grant_type") grantType: String = "refresh_token"
    ) : Response<AccessToken>

    /**
     * Refreshes an access token token. This is not suspended, and will block the current thread
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it
     *
     * @param refreshToken The refresh token (as retrieved from [AccessToken.refreshToken])
     */
    @POST(ACCESS_TOKEN_PATH)
    @FormUrlEncoded
    fun refreshTokenNoSuspend(
            @Header("Authorization") basicAuth: String,
            @Field("refresh_token") refreshToken: String,
            @Field("grant_type") grantType: String = "refresh_token"
    ) : Call<AccessToken>


    /**
     * Revokes an access token. This can either revoke an access token, making only the current
     * access token invalid, or a refresh token, making it impossible to get a new access token later
     *
     * Note: The request must be made with an "Authorization: Basic " header attached to it with
     *
     * @param token The token to revoke (as retrieved from [AccessToken.accessToken] or [AccessToken.refreshToken])
     * @param tokenType To revoke an access token, use "access_token", to revoke a refresh token, use "refresh_token"
     */
    @POST("api/v1/revoke_token")
    @FormUrlEncoded
    suspend fun revokeToken(
            @Field("token") token: String,
            @Field("token_type_hint") tokenType: String
    ) : Response<Any?>
}