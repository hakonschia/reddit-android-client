package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.model.internal.RedditMultiWrapper
import com.example.hakonsreader.api.responses.ListingResponse
import retrofit2.Response
import retrofit2.http.*

interface UserService {

    /**
     * Retrieves information about the logged in user. For information about any user see
     * [getUserInfoOtherUsers]
     *
     * OAuth scope required: *identity*
     *
     * @return A Response holding a [RedditUser]
     */
    @GET("api/v1/me?raw_json=1")
    suspend fun getUserInfo() : Response<RedditUser>

    /**
     * Retrieves information about a user. This can retrieve information about users that aren't
     * the logged in user
     *
     * OAuth scope required: *read*
     *
     * @return A Response holding a [RedditListing] that should be cast to a [RedditUser]
     */
    @GET("user/{username}/about?raw_json=1")
    suspend fun getUserInfoOtherUsers(@Path("username") username: String) : Response<RedditListing>


    /**
     * OAuth scope required: *history*
     *
     * @param username The username of the user to get listings for
     * @param what What kind of listings to get. Values possible:
     * 1. *overview* - A mix of comments and posts
     * 2. *submitted* - Posts by the user
     * 3. *comments* - Comments by the user
     * 4. *upvoted* - Posts/comments upvoted by the user. This is only accessible for the user itself</li>
     * 5. *downvoted* - Posts/comments downvoted by the user. This is only accessible for the user itself</li>
     * 6. *hidden* - Posts/comments hidden by the user. This is only accessible for the user itself</li>
     * 7. *saved* - Posts/comments saved by the user. This is only accessible for the user itself</li>
     * @param after The fullname of the last listing retrieved
     * @param count The amount of items already fetched
     * @param limit The amount of posts to retrieve
     * @param sort How to sort the post (new, best etc.). This should use [SortingMethods] and its
     * corresponding value
     * @param timeSort How to sort the posts. This should use [PostTimeSort] and its corresponding value
     * @param T The type of listing to retrieve. This should match the type expected (ie. for posts the type
     * should be [RedditPost]. The [ListingResponse] returned will hold a list of objects of this class
     * @return A ListingResponse call. The listings returned depend on the value of "what" (comments, post, etc.)
     */
    @GET("user/{username}/{what}?raw_json=1")
    suspend fun <T : RedditListing> getListingsFromUser(
            @Path("username") username: String,
            @Path("what") what: String,
            @Query("sort") sort: String,
            @Query("t") timeSort: String,
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
    ) : Response<ListingResponse<T>>


    /**
     * Block a user
     *
     * OAuth scope required: *account*
     *
     * @param username The username of the user to block
     * @return The Response will not hold any information
     */
    @POST("api/block_user")
    @FormUrlEncoded
    suspend fun blockUser(@Field("name") username: String) : Response<Void>


    /**
     * Unblock a user
     *
     * @param name The name of the user to block
     * @param fullname The fullname of the logged in user (the user blocking [name])
     */
    @POST("api/unfriend")
    @FormUrlEncoded
    suspend fun unblockUser(
            @Field("name") name: String,
            @Field("container") fullname: String,
            @Field("type") type: String = "enemy"
    ) : Response<Void>


    /**
     * Get multis from the logged in user
     */
    @GET("api/multi/mine?raw_json=1")
    suspend fun getMultisLoggedInUser(): Response<List<RedditMultiWrapper>>

    /**
     * Get multis from a user
     *
     * @param username The username to retrieve mutlis from
     */
    @GET("/api/multi/user/{username}?raw_json=1")
    suspend fun getMultisFromUser(@Path("username") username: String): Response<List<RedditMultiWrapper>>

    /**
     * Get posts from a multi
     */
    @GET("user/{username}/m/{multiName}/{sort}?raw_json=1")
    suspend fun getPostsFromMulti(
            @Path("username") username: String,
            @Path("multiName") multiName: String,
            @Path("sort") sort: String,
            @Query("t") timeSort: String,
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
    ): Response<ListingResponse<RedditPost>>
}