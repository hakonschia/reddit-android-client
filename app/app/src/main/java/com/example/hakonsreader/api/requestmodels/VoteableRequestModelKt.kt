package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.VoteServiceKt
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception


/**
 * Request class to cast a vote. This is a convenience class that other request classes that allow
 * for voting can use, and is not exposed outside the API package
 */
internal class VoteableRequestModelKt(
        private val accessToken: AccessToken,
        private val api: VoteServiceKt
) {


    /**
     * Cast a vote on something
     *
     * OAuth scope required: *vote*
     *
     * @param thing The type of thing to vote on
     * @param id The ID of the thing
     * @param type The type of vote to cast
     */
    suspend fun vote(thing: Thing, id: String, type: VoteType) : ApiResponse<Nothing?> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Voting requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.vote(Util.createFullName(thing, id), type.value)

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