package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.requestmodels.thirdparty.ThirdPartyRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.UserService
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken



/**
 * Interface for communicating with Reddit users. This should only be used for communication
 * about users not the logged in user. For logged in users use [UserRequestsLoggedInUser]
 */
interface UserRequests {
    /**
     * Retrieves information about the user
     *
     * OAuth scope required: *read*
     *
     * @return A [RedditUser] object representing the user if successful
     */
    suspend fun info() : ApiResponse<RedditUser>

    /**
     * Retrieves posts from the user. This requires the username to be set, even for logged in users.
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
    suspend fun posts(
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    ) : ApiResponse<List<RedditPost>>

    /**
     * Retrieves comments from the user. This requires the username to be set, even for logged in users.
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the comments (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the comments should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the last post seen. Default is an empty string (ie. no last comment)
     * @param count The amount of comments already retrieved. Default is *0* (ie. no comments already)
     * @param limit The amount of comments to retrieve
     */
    suspend fun comments(
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    )
    : ApiResponse<List<RedditComment>>

    /**
     * Retrieves posts and comments the user has saved. This is only available for the logged in user
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the posts/comments (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the posts/comments should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the last post/comment seen. Default is an empty string (ie. no last post/comment)
     * @param count The amount of posts/comments already retrieved. Default is *0* (ie. no posts/comments already)
     * @param limit The amount of posts/comments to retrieve
     */
    suspend fun saved(
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    ): ApiResponse<List<RedditListing>>

    /**
     * Retrieves comments and posts the user has upvoted. This is only available for the logged in user
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the posts/comments (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the posts/comments should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the post/comment post seen. Default is an empty string (ie. no post/comment comment)
     * @param count The amount of posts/comments already retrieved. Default is *0* (ie. no posts/comments already)
     * @param limit The amount of posts/comments to retrieve
     */
    suspend fun upvoted(
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    ): ApiResponse<List<RedditListing>>

    /**
     * Retrieves posts/comments the user has downvoted. This is only available for the logged in user
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the posts/comments (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the comments should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the post/comment post seen. Default is an empty string (ie. no post/comment comment)
     * @param count The amount of posts/comments already retrieved. Default is *0* (ie. no posts/comments already)
     * @param limit The amount of posts/comments to retrieve
     */
    suspend fun downvoted(
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    ): ApiResponse<List<RedditListing>>

    /**
     * Blocks a user. If the access token is not valid for a logged in user, or there is no username set,
     * this will return an error
     *
     * OAuth scope required: *account*
     *
     * @return Successful requests return no data
     */
    suspend fun block() : ApiResponse<Nothing?>

    /**
     * Unblocks a user
     *
     * No specific OAuth scope is required
     *
     * @param loggedInUserId The ID of the user currently logged in (the user who is blocked another user)
     * @return Successful requests return no data
     */
    suspend fun unblock(loggedInUserId: String) : ApiResponse<Nothing?>
}

/**
 * Request model for communicating with Reddit users. This should only be used for communication
 * about users not the logged in user. For logged in users use [UserRequestsLoggedInUser]
 */
class UserRequestsImpl(
        private val username: String,
        private val accessToken: AccessToken,
        private val api: UserService,
        imgurApi: ImgurService?,
        gfycatApi: GfycatService,
) : UserRequests {

    /**
     * Listing types that can be retrieved for users
     */
    private enum class ListingType(val value: String) {
        POSTS("submitted"),
        COMMENTS("comments"),
        UPVOTED("upvoted"),
        DOWNVOTED("DOWNVOTED"),
        SAVED("saved");
    }

    private val thirdPartyRequest = ThirdPartyRequest(imgurApi, gfycatApi)


    override suspend fun info() : ApiResponse<RedditUser> {
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
     * Gets listings for a user
     *
     * OAuth scope required: *read*
     *
     * @param what What listing type to get
     * @param postSort sort for the posts (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the last post seen. Default is an empty string (ie. no last post)
     * @param count The amount of posts already retrieved. Default is *0* (ie. no posts already)
     * @param limit The amount of posts to retrieve
     */
    private suspend fun <T : RedditListing> listings(
            what: ListingType,
            postSort: SortingMethods = SortingMethods.HOT,
            timeSort: PostTimeSort = PostTimeSort.DAY,
            after: String = "",
            count: Int = 0,
            limit: Int = 25,
    ) : ApiResponse<List<T>> {
        return try {
            val resp = api.getListingsFromUser<T>(
                    username,
                    what.value,
                    postSort.value,
                    timeSort.value,
                    after,
                    count,
                    limit
            )

            val listings = resp.body()?.getListings()

            if (listings != null) {
                // Load third party for the posts in the listings
                val posts = listings.filterIsInstance<RedditPost>()
                thirdPartyRequest.loadAll(posts)
                ApiResponse.Success(listings)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun posts(
            postSort: SortingMethods,
            timeSort: PostTimeSort,
            after: String,
            count: Int,
            limit: Int,
    ) : ApiResponse<List<RedditPost>> {
        return listings(ListingType.POSTS, postSort, timeSort, after, count, limit)
    }

    override suspend fun comments(
            postSort: SortingMethods,
            timeSort: PostTimeSort,
            after: String,
            count: Int,
            limit: Int,
    )
    : ApiResponse<List<RedditComment>> {
        return listings(ListingType.COMMENTS, postSort, timeSort, after, count, limit)
    }

    override suspend fun saved(
            postSort: SortingMethods,
            timeSort: PostTimeSort,
            after: String,
            count: Int,
            limit: Int,
    ): ApiResponse<List<RedditListing>> {
        return listings(ListingType.SAVED, postSort, timeSort, after, count, limit)
    }

    override suspend fun upvoted(
            postSort: SortingMethods,
            timeSort: PostTimeSort,
            after: String,
            count: Int,
            limit: Int,
    ): ApiResponse<List<RedditListing>> {
        return listings(ListingType.UPVOTED, postSort, timeSort, after, count, limit)
    }

    override suspend fun downvoted(
            postSort: SortingMethods,
            timeSort: PostTimeSort,
            after: String,
            count: Int,
            limit: Int,
    ): ApiResponse<List<RedditListing>> {
        return listings(ListingType.DOWNVOTED, postSort, timeSort, after, count, limit)
    }


    override suspend fun block() : ApiResponse<Nothing?> {
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

    override suspend fun unblock(loggedInUserId: String) : ApiResponse<Nothing?> {
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