package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.VoteService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception


/**
 * Interface for voting on a [Thing]. This is a helper interface and should not be used outside
 * the API package
 */
interface VoteableRequestModel {
    /**
     * Cast a vote on something
     *
     * OAuth scope required: *vote*
     *
     * @param thing The type of thing to vote on
     * @param id The ID of the thing
     * @param type The type of vote to cast
     */
    suspend fun vote(thing: Thing, id: String, type: VoteType) : ApiResponse<Unit>
}

/**
 * Standard [VoteableRequestModel] implementation
 */
internal class VoteableRequestModelImpl(
        private val accessToken: AccessToken,
        private val api: VoteService
) : VoteableRequestModel {

    override suspend fun vote(thing: Thing, id: String, type: VoteType) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Voting requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.vote(createFullName(thing, id), type.value)

            if (resp.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}