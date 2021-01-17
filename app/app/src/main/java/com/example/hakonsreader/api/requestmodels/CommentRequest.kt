package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.interfaces.ReplyableRequest
import com.example.hakonsreader.api.interfaces.ReportableRequest
import com.example.hakonsreader.api.interfaces.SaveableRequest
import com.example.hakonsreader.api.interfaces.VoteableRequest
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.CommentService
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class CommentRequest(
        private val accessToken: AccessToken,
        private val api: CommentService,
        private val commentId: String
) : VoteableRequest, ReplyableRequest, SaveableRequest, ReportableRequest {

    private val voteRequest: VoteableRequestModel = VoteableRequestModel(accessToken, api)
    private val replyRequest: ReplyableRequestModel = ReplyableRequestModel(accessToken, api)
    private val saveRequest: SaveableRequestModel = SaveableRequestModel(accessToken, api)
    private val modRequest: ModRequestModel = ModRequestModel(accessToken, api)


    /**
     * Delete a comment. The comment being deleted must be posted by the user the access token represents
     *
     * @return The response will not hold any data. Note: Even if the comment couldn't be deleted because
     * it didn't belong to the user it will seem successful as Reddit returns a 200 OK for every request
     */
    suspend fun delete() : ApiResponse<Any?> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot delete comments without an access token for a logged in user", e))
        }

        return try {
            val response = api.delete(Util.createFullName(Thing.COMMENT, commentId))
            if (response.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    suspend fun edit(editedText: String) : ApiResponse<RedditComment> {
        // TODO this isn't tested
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot edit comments without an access token for a logged in user", e))
        }

        return try {
            val resp = api.edit(
                    Util.createFullName(Thing.COMMENT, commentId),
                    editedText
            )

            val comment = resp.body()?.getListings()?.get(0)
            if (comment != null) {
                ApiResponse.Success(comment)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Vote on the comment
     *
     * OAuth scope required: *vote*
     *
     * @param voteType The type of vote to cast
     * @return An [ApiResponse] with no success data
     */
    override suspend fun vote(voteType: VoteType): ApiResponse<Nothing?> {
        return voteRequest.vote(Thing.COMMENT, commentId, voteType)
    }

    /**
     * Submit a new comment as a reply to the post. For replies to other comments use {@link CommentRequest#reply(String, OnResponse, OnFailure)}
     *
     * OAuth scope required: *submit*
     *
     * @param text The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     * @return An [ApiResponse] with the new comment
     */
    override suspend fun reply(text: String): ApiResponse<RedditComment> {
        return replyRequest.postComment(Thing.COMMENT, commentId, text)
    }


    /**
     * Save the comment
     *
     * OAuth scope required: *save*
     *
     * @return An [ApiResponse] with no success data
     * @see unsave
     */
    override suspend fun save(): ApiResponse<Nothing?> {
        return saveRequest.save(Thing.COMMENT, commentId)
    }

    /**
     * Unsave the comment
     *
     * OAuth scope required: *save*
     *
     * @return An [ApiResponse] with no success data
     * @see save
     */
    override suspend fun unsave() : ApiResponse<Nothing?> {
        return saveRequest.unsave(Thing.COMMENT, commentId)
    }


    /**
     * Distinguish the comment as mod
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated comment
     * @see removeModDistinguish
     */
    suspend fun distinguishAsMod() : ApiResponse<RedditComment> {
        return modRequest.distinguishAsModComment(commentId, distinguish =  true, sticky = false)
    }

    /**
     * Removes the mod distinguish (and sticky if the comment is stickied) on the comment
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated comment
     * @see distinguishAsMod
     */
    suspend fun removeModDistinguish() : ApiResponse<RedditComment> {
        return modRequest.distinguishAsModComment(commentId, distinguish =  false, sticky = false)
    }

    /**
     * Sticky the comment. This will also distinguish the comment as a moderator. This only works
     * on top-level comments
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated comment
     * @see unsticky
     */
    suspend fun sticky() : ApiResponse<RedditComment> {
        return modRequest.distinguishAsModComment(commentId, distinguish =  true, sticky = true)
    }

    /**
     * Removes the sticky on the comment, keeps the mod distinguish. To remove both use
     * [removeModDistinguish]
     *
     * If the currently logged in user is not a moderator in the subreddit the post is in this will fail
     *
     * OAuth scope required: *modposts*
     *
     * @return An [ApiResponse] with the updated comment
     * @see sticky
     */
    suspend fun unsticky() : ApiResponse<RedditComment>  {
        // Well this is identical to distinguishAsMod, but the function name makes sense so keep it
        return modRequest.distinguishAsModComment(commentId, distinguish =  true, sticky = false)
    }

    /**
     * Ignores reports on the comment
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see unignoreReports
     */
    override suspend fun ignoreReports() : ApiResponse<Any?> {
        return modRequest.ignoreReports(Thing.COMMENT, commentId)
    }

    /**
     * Unignores reports on the comment
     *
     * OAuth scope required: `modposts`
     *
     * @return An [ApiResponse] with no success data
     * @see ignoreReports
     */
    override suspend fun unignoreReports() : ApiResponse<Any?> {
        return modRequest.unignoreReports(Thing.COMMENT, commentId)
    }

}