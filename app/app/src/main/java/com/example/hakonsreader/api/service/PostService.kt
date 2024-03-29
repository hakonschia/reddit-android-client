package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.JsonResponse
import com.example.hakonsreader.api.responses.ListingResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Service interface to make post related API calls towards Reddit
 */
interface PostService : VoteService, ReplyService, SaveService, ModService {

    /**
     * Retrieves comments for a post
     *
     * @param postId The ID of the post to retrieve comments for
     * @param sort How to sort the comments (new, hot, top etc.)
     * @return A list of [ListingResponse]. The first item in this list is the post itself.
     * The actual comments is found in the second element of the list
     */
    @GET("comments/{postID}?raw_json=1")
    suspend fun getComments(
            @Path("postID") postId: String,
            @Query("sort") sort: String
    ) : Response<List<ListingResponse<RedditListing>>>


    /**
     * Retrieves more comments (from "4 more comments" comments)
     *
     * @param children A comma separated string of the IDs of the comments to load
     * @param linkId The fullname of the post the comments are in
     * @param apiType The string "json"
     * @param rawJson Set to 1 if the response should be raw JSON
     * @return A Response with a [JsonResponse] holding the new comments
     */
    @POST("api/morechildren")
    @FormUrlEncoded
    suspend fun getMoreComments(
            @Field("children") children: String,
            @Field("link_id") linkId: String,
            @Field("api_type") apiType: String = "json",
            @Field("raw_json") rawJson: Int = 1
    ) : Response<JsonResponse<RedditComment>>

    /**
     * Retrieves information about a post/a group of posts
     *
     * @param fullname The fullname of the post, or a comma separated string of the fullnames of the posts
     * @return A Response with a [ListingResponse] holding the list of the posts
     */
    @GET("api/info?raw_json=1")
    suspend fun getInfo(
            @Query("id") fullname: String
    ) : Response<ListingResponse<RedditPost>>

    /**
     * Delete a post
     *
     * OAuth scope required: *edit*
     *
     * @param fullname The fullname of the post to delete
     * @return The response returns no data
     */
    @POST("api/del")
    @FormUrlEncoded
    suspend fun delete(@Field("id") fullname: String) : Response<Any>


    /**
     * Mark a post as NSFW
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @return The response returns no data
     */
    @POST("api/marknsfw")
    @FormUrlEncoded
    suspend fun markNsfw(@Field("id") fullname: String) : Response<Void>

    /**
     * Unmark a post as NSFW
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @return The response returns no data
     */
    @POST("api/unmarknsfw")
    @FormUrlEncoded
    suspend fun unmarkNsfw(@Field("id") fullname: String) : Response<Void>

    /**
     * Mark a post as a spoiler
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @return The response returns no data
     */
    @POST("api/spoiler")
    @FormUrlEncoded
    suspend fun markSpoiler(@Field("id") fullname: String) : Response<Void>

    /**
     * Unmark a post as a spoiler
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @return The response returns no data
     */
    @POST("api/unspoiler")
    @FormUrlEncoded
    suspend fun unmarkSpoiler(@Field("id") fullname: String) : Response<Void>
}