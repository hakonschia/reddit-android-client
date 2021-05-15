package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.requestmodels.AccessTokenModelImpl.TokenType
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.AccessTokenService
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception


/**
 * Interface for communication about access tokens
 */
interface AccessTokenModel {
    /**
     * Gets a new access token. This must be called after the initial login process is completed and
     * a `authorization_code` is retrieved from `https://www.reddit.com/api/v1/authorize`
     *
     * @param code The authorization code received from the initial login process
     * @return A response with the new token
     */
    suspend fun get(code: String) : ApiResponse<AccessToken>

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
    suspend fun revoke(accessToken: AccessToken, type: TokenType = TokenType.REFRESH_TOKEN) : ApiResponse<Unit>
}


/**
 * Standard [AccessTokenModel] implementation
 */
class AccessTokenModelImpl(
        private val api: AccessTokenService,
        private val callbackUrl: String,
        private val onNewToken: (AccessToken) -> Unit
) : AccessTokenModel {

    /**
     * Token types used when revoking an access token
     */
    enum class TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN
    }


    override suspend fun get(code: String) : ApiResponse<AccessToken> {
        if (callbackUrl.isBlank()) {
            throw IllegalStateException("Callback URL is not set. Use RedditApi.callbackUrl")
        }

        return try {
            val response = api.getAccessToken(code, callbackUrl)
            val token = response.body()

            if (token != null) {
                // Need to notify the API itself (this is kinda bad I guess since it needs to know that
                // the API won't call its own callback)
                onNewToken.invoke(token)
                ApiResponse.Success(token)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun revoke(accessToken: AccessToken, type: TokenType) : ApiResponse<Unit> {
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
                onNewToken.invoke(AccessToken())
                ApiResponse.Success(Unit)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}