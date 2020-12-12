package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.interfaces.OnNewToken
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.AccessTokenService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.RedditApi
import java.lang.Exception

class AccessTokenModel(
        private val api: AccessTokenService,
        private val callbackUrl: String,
        private val onNewToken: OnNewToken
) {

    /**
     * Token types used when revoking an access token
     */
    enum class TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN
    }


    /**
     * Gets a new access token. This must be called after the initial login process is completed and
     * a `authorization_code` is retrieved from `https://www.reddit.com/api/v1/authorize`
     *
     * The new token is sent to the callback set when building the [RedditApi] object
     *
     * @param code The authorization code received from the initial login process
     * @return This will not return any data. If an access token was retrieved, it is sent to the
     * registered token listener
     */
    suspend fun get(code: String) : ApiResponse<Any?> {
        if (callbackUrl.isEmpty()) {
            throw IllegalStateException("Callback URL is not set. Use RedditApi.Builder.callbackUrl()")
        }

        return try {
            val response = api.getAccessToken(code, callbackUrl)
            val token = response.body()

            if (token != null) {
                onNewToken.newToken(token)
                ApiResponse.Success(null)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Revoke an access token.
     *
     * This can do 2 things:
     * 1. Revoke an access token: This will only revoke the current token, and can later be refreshed
     * with the refresh token linked to it
     * 2. Revoke a refresh token: This will invalidate the current token, as well as invalidating
     * a new token from being retrieved later. This will require the user to re-do the login process
     *
     * If the token is revoked, an empty access token is set and the callback for new tokens will be called
     *
     * @param accessToken The token to revoke
     * @param type The type of token to revoke (by default this is [TokenType.REFRESH_TOKEN])
     * @return This will not return any data, but should be checked to ensure the request was successful
     */
    suspend fun revoke(accessToken: AccessToken, type: TokenType = TokenType.REFRESH_TOKEN) : ApiResponse<Any?> {
        if (type == TokenType.REFRESH_TOKEN && accessToken.refreshToken.isNullOrEmpty()) {
            // Cant revoke
            return ApiResponse.Error(GenericError(-1), Throwable("No token to revoke"))
        }

        return try {
            val response = if (type == TokenType.REFRESH_TOKEN) {
                api.revokeToken(
                        accessToken.refreshToken,
                        "refresh_token"
                )
            } else {
                api.revokeToken(
                        accessToken.accessToken,
                        "access_token"
                )
            }

            if (response.isSuccessful) {
                onNewToken.newToken(AccessToken())
                ApiResponse.Success(null)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}