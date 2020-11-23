package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ListingResponseKt
import retrofit2.Response
import retrofit2.http.*

interface UserServiceKt {

    /**
     * Retrieves information about the logged in user. For information about any user see
     * [userInfoOtherUsers]
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
     * @return A Response holding a [RedditUser]
     */
    @GET("u/{username}/about?raw_json=1")
    suspend fun getUserInfoOtherUsers(@Path("username") username: String) : Response<RedditUser>


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

     * @param sort How to sort the listings (top, new, hot, or controversial)
     * @param timeSort For listings that can be sorted by time, how to sort the listings (use {@link com.example.hakonsreader.api.enums.PostTimeSort}
     *                 and getValue())
     * @param T The type of listing to retrieve. This should match the type expected (ie. for posts the type
     * should be [RedditPost]. The [ListingResponseKt] returned will hold a list of objects of this class
     * @return A ListingResponse call. The listings returned depend on the value of "what" (comments, post, etc.)
     */
    @GET("user/{username}/{what}?raw_json=1")
    suspend fun <T : RedditListing> getListingsFromUser(
            @Path("username") username: String,
            @Path("what") what: String,
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("sort") sort: String,
            @Query("t") timeSort: String
    ) : Response<ListingResponseKt<T>>


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
    suspend fun blockUser(@Field("name") username: String) : Response<Nothing>
}