package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.UserService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

/**
 * Request call for API calls related to logged in users
 *
 * @see UserRequests
 */
class UserRequestsLoggedInUser(
        private val accessToken: AccessToken,
        private val api: UserService,
) {

    /**
     * Retrieves information about the logged in user
     *
     * OAuth scope required: *identity*
     *
     * @return A [RedditUser] object representing the user if successful
     */
    suspend fun info() : ApiResponse<RedditUser> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Can't get user information without access token for a logged in user", e))
        }

        return try {
            val resp = api.getUserInfo()
            val user = resp.body()

            if (user != null) {
                ApiResponse.Success(user)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

}