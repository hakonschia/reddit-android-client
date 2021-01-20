package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.SaveService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

class SaveableRequestModel(
        private val accessToken: AccessToken,
        private val api: SaveService
) {

    /**
     * Unsave a post or comment
     *
     * OAuth scope required: *save*
     *
     * @param thing The type of thing to unsave
     * @param id The ID of the thing to unsave
     * @return The Response returned will not include any data
     */
    suspend fun save(thing: Thing, id: String) : ApiResponse<Nothing?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Saving a comment or post requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.save(createFullName(thing, id))

            if (resp.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Unsave a post or comment
     *
     * OAuth scope required: *save*
     *
     * @param thing The type of thing to unsave
     * @param id The ID of the thing to unsave
     * @return The Response returned will not include any data
     */
    suspend fun unsave(thing: Thing, id: String) : ApiResponse<Nothing?>  {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Unsaving a comment or post requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.unsave(createFullName(thing, id))

            if (resp.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

}