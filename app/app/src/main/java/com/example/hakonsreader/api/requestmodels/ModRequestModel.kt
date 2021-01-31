package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.ModService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

/**
 * Request model for performing various mod related actions
 */
class ModRequestModel(
        private val accessToken: AccessToken,
        private val api: ModService
) {


    /**
     * Distinguish a comment as mod, and optionally sticky it
     *
     * @param id The ID of the comment to distinguish
     * @param distinguish True to distinguish as mod, false to remove previous distinguish
     * @param sticky True to also sticky the comment. This is only possible on top-level comments
     */
    suspend fun distinguishAsModComment(id: String, distinguish: Boolean, sticky: Boolean) : ApiResponse<RedditComment> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Distinguishing/stickying a comment requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.distinguishAsModComment(
                    createFullName(Thing.COMMENT, id),
                    if (distinguish) "yes" else "no",
                    sticky,
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
     * Distinguish a post as mod
     *
     * @param id The ID of the post to distinguish
     * @param distinguish True to distinguish as mod, false to remove previous distinguish
     */
    suspend fun distinguishAsModPost(id: String, distinguish: Boolean) : ApiResponse<RedditPost> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Distinguishing a post requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.distinguishAsModPost(
                    createFullName(Thing.POST, id),
                    if (distinguish) "yes" else "no"
            )

            val post = resp.body()?.getListings()?.get(0)
            if (post != null) {
                ApiResponse.Success(post)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Sticky or unsticky a post
     *
     * @param id The id of the post
     * @param sticky True to sticky, false to unsticky. If the post is already stickied and this is true,
     *               a 409 Conflict error will occur
     * @return No data is returned
     */
    suspend fun stickyPost(id: String, sticky: Boolean) : ApiResponse<Any?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Stickying a post requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.stickyPost(
                    createFullName(Thing.POST, id),
                    sticky
            )

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
     * Ignore reports on a post or comment
     *
     * @see unignoreReports
     */
    suspend fun ignoreReports(thing: Thing, id: String) : ApiResponse<Any?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Ignoring reports requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.ignoreReports(
                    createFullName(thing, id)
            )

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
     * Unignore reports on a post or comment
     *
     * @return No response data is returned
     * @see ignoreReports
     */
    suspend fun unignoreReports(thing: Thing, id: String) : ApiResponse<Any?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Unignoring reports requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.unignoreReports(
                    createFullName(thing, id)
            )

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
     * Lock a post or comment
     *
     * @param id The id of the post/comment
     * @param isPost True if a post is being locked, false if a comment is being locked
     * @return No response data is returned
     * @see unlock
     */
    suspend fun lock(id: String, isPost: Boolean) : ApiResponse<Any?> {
        return lockInternal(if (isPost) Thing.POST else Thing.COMMENT, id, true)
    }

    /**
     * Unlock a post or comment
     *
     * @param id The id of the post/comment
     * @param isPost True if a post is being unlocked, false if a comment is being unlocked
     * @return No response data is returned
     * @see lock
     */
    suspend fun unlock(id: String, isPost: Boolean) : ApiResponse<Any?> {
        return lockInternal(if (isPost) Thing.POST else Thing.COMMENT, id, false)
    }

    private suspend fun lockInternal(thing: Thing, id: String, lock: Boolean) : ApiResponse<Any?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Cannot lock/unlock without a valid access token for a logged in user", e))
        }

        val fullname = createFullName(thing, id)

        return try {
            val resp = if (lock) {
                api.lock(fullname)
            } else {
                api.unlock(fullname)
            }

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