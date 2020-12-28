package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.ReplyService
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.apiListingErrors
import java.lang.Exception

class ReplyableRequestModel(
        private val accessToken: AccessToken,
        private val api: ReplyService
) {


    /**
     * Submit a new comment as a reply to another comment. Note: The depth of the new comment for replies is not
     * set (it is -1) and must be set manually with [RedditComment.setDepth] (as the parents depth + 1)
     *
     * OAuth scopes required:
     * 1. For comments to post and replies: *submit*
     * 2. For private messages: *privatemessage*
     *
     * @param thing The type of thing to comment on
     * @param id The ID of the thing being commented on
     * @param commentText The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     */
    suspend fun postComment(thing: Thing, id: String, commentText: String) : ApiResponse<RedditComment> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Posting a comment requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.postComment(
                    commentText,
                    Util.createFullName(thing, id),
                    RedditApi.API_TYPE,
                    false
            )

            val body = resp.body()
            if (body?.hasErrors() == true) {
                val errors = body.errors()
                apiListingErrors(errors as List<List<String>>)
            } else {
                val comment = body?.getListings()?.get(0)
                if (comment != null) {
                    comment.depth = if (thing == Thing.POST) {
                        0
                    } else {
                        -1
                    }

                    ApiResponse.Success(comment)
                } else {
                    apiError(resp)
                }
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}