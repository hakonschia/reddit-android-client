package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.requestmodels.thirdparty.ThirdPartyRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.UserService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

/**
 * Request model for communicating with Reddit users. This should only be used for communcation
 * about users not the logged in user. For logged in users use [UserRequestsLoggedInUser]
 */
class UserRequests(
        private val username: String,
        private val accessToken: AccessToken,
        private val api: UserService,
        imgurApi: ImgurService?,
        gfycatApi: GfycatService
) {

    private val imgurRequest = ThirdPartyRequest(imgurApi, gfycatApi)


    /**
     * Retrieves information about the user
     *
     * OAuth scope required: *read*
     *
     * @return A [RedditUser] object representing the user if successful
     */
    suspend fun info() : ApiResponse<RedditUser> {
        return try {
            val resp = api.getUserInfoOtherUsers(username)
            val user = resp.body()

            if (user != null) {
                ApiResponse.Success(user as RedditUser)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Retrieves posts from the user. This requires the username to be set, even for logged in users.
     *
     * If an access token for a user is set posts are customized for the user
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the posts (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the last post seen. Default is an empty string (ie. no last post)
     * @param count The amount of posts already retrieved. Default is *0* (ie. no posts already)
     * @param limit The amount of posts to retrieve
     */
    suspend fun posts(postSort: SortingMethods = SortingMethods.HOT, timeSort: PostTimeSort = PostTimeSort.DAY, after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditPost>> {
        return try {
            val resp = api.getListingsFromUser<RedditPost>(
                    username,
                    "submitted",
                    postSort.value,
                    timeSort.value,
                    after,
                    count,
                    limit
            )

            val posts = resp.body()?.getListings()

            if (posts != null) {
                imgurRequest.loadAll(posts)
                ApiResponse.Success(posts)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Blocks a user. If the access token is not valid for a logged in user, or there is no username set,
     * this will return an error
     *
     * OAuth scope required: *account*
     *
     * @return Successful requests return no data
     */
    suspend fun block() : ApiResponse<Nothing?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Can't block user without access token for a logged in user", e))
        }

        return try {
            val resp = api.blockUser(username)

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
     * Unblocks a user
     *
     * No specific OAuth scope is required
     *
     * @param loggedInUserId The ID of the user currently logged in (the user who is blocked another user)
     * @return Successful requests return no data
     */
    suspend fun unblock(loggedInUserId: String) : ApiResponse<Nothing?> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Can't unblock user without access token for a logged in user", e))
        }

        return try {
            val resp = api.unblockUser(
                    username,
                    createFullName(Thing.ACCOUNT, loggedInUserId)
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
}