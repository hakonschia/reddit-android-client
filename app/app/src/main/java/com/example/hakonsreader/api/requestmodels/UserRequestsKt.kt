package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.ImgurService
import com.example.hakonsreader.api.service.UserServiceKt
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class UserRequestsKt(
        private val username: String?,
        private val accessToken: AccessToken,
        private val api: UserServiceKt,
        imgurApi: ImgurService?
) {

    private val imgurRequest: ImgurRequest = ImgurRequest(imgurApi)
    private val loadImgurAlbumsAsRedditGalleries: Boolean = imgurApi != null


    /**
     * Retrieves information about the user. If the username was set to *null* then information
     * is retrieved about the logged in user (if possible), otherwise information is retrieved
     * about the username set
     *
     * OAuth scopes required:
     * 1. For logged in users: *identity*
     * 2. For other users: *read*
     *
     * @return A [RedditUser] object representing the user if successful
     */
    suspend fun info() : ApiResponse<RedditUser> {
        return if (username == null) {
            infoForLoggedInUser()
        } else {
            infoForNonLoggedInUser()
        }
    }

    /**
     * Retrieves information about the logged in user. If [accessToken] is not valid for a logged in
     * user this will return an error
     *
     * @return A [RedditUser] object representing the user if successful
     */
    private suspend fun infoForLoggedInUser() : ApiResponse<RedditUser> {
        try {
            Util.verifyLoggedInToken(accessToken)
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

    /**
     * Retrieves information about another user. [username] must not be null in this call
     * as it will use the username set as the user to get information about
     *
     * @return A [RedditUser] object representing the user if successful
     */
    private suspend fun infoForNonLoggedInUser() : ApiResponse<RedditUser> {
        return try {
            val resp = api.getUserInfoOtherUsers(username!!)
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
     */
    suspend fun posts(postSort: SortingMethods = SortingMethods.HOT, timeSort: PostTimeSort = PostTimeSort.DAY, after: String = "", count: Int = 0) : ApiResponse<List<RedditPost>> {
        if (username == null) {
            return ApiResponse.Error(GenericError(-1), IllegalStateException("Cannot get posts without a username"))
        }

        return try {
            val resp = api.getListingsFromUser<RedditPost>(
                    username,
                    "submitted",
                    after,
                    count,
                    postSort.value,
                    timeSort.value,
            )

            val posts = resp.body()?.getListings()

            if (posts != null) {
                if (loadImgurAlbumsAsRedditGalleries) {
                    imgurRequest.loadAlbums(posts)
                }

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
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Can't get user information without access token for a logged in user", e))
        }

        if (username == null) {
            return ApiResponse.Error(GenericError(-1), IllegalStateException("No username set to block"))
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
}