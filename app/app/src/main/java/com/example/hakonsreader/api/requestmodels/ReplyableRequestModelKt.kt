package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.ReplyServiceKt
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class ReplyableRequestModelKt(
        private val accessToken: AccessToken,
        private val api: ReplyServiceKt
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
     * @param comment The comment to submit, formatted as <a href="https://en.wikipedia.org/wiki/Markdown">Markdown</a>
     */
    suspend fun postComment(thing: Thing, id: String, comment: String) : ApiResponse<RedditComment> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Posting a comment requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.postComment(
                    comment,
                    Util.createFullName(thing, id),
                    RedditApi.API_TYPE,
                    false
            )
            // TODO when this is used listing/http errors probably have to be separated as it is
            //  in the java version (response.hasErrors() etc)

            val comment = resp.body()?.response?.getListings()?.get(0)
            if (comment != null) {
                /*
                comment.depth = if (thing == Thing.POST) {
                    0
                } else {
                    -1
                }
                 */

                ApiResponse.Success(comment)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}