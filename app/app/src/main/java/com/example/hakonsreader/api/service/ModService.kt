package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.JsonResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Interface for making mod related API requests
 */
interface ModService {

    // Comments and posts work a bit differently. If comments are stickied, they MUST also be distinguished as mod
    // Posts can be stickied WITHOUT being distinguished as mod, and they use a different api call to sticky
    // Distinguishing a post also cannot send a "sticky" parameter, or else it will fail with 400 Bad Request, so
    // we need two different functions for comments and post distinguishing


    /**
     * Distinguish a comment as a moderator
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the comment
     * @param how "yes" to distinguish as mod, "no" to remove the mod distinguish
     * @param sticky True to also sticky the comment (comments will also be mod distinguished when stickied)
     * @param apiType The string "json"
     * @return A Response with a [JsonResponse]. The response will hold the updated comment
     */
    @POST("api/distinguish")
    @FormUrlEncoded
    suspend fun distinguishAsModComment(
            @Field("id") fullname: String,
            @Field("how") how: String,
            @Field("sticky") sticky: Boolean,
            @Field("api_type") apiType: String
    ) : Response<JsonResponse<RedditComment>>


    /**
     * Distinguish a post as a moderator
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @param how "yes" to distinguish as mod, "no" to remove the mod distinguish
     * @param apiType The string "json"
     * @return A Response with a [JsonResponse]. The response will hold the updated post
     */
    @POST("api/distinguish")
    @FormUrlEncoded
    suspend fun distinguishAsModPost(
            @Field("id") fullname: String,
            @Field("how") how: String,
            @Field("api_type") apiType: String
    ) : Response<JsonResponse<RedditPost>>

    /**
     * Sticky a post on the subreddit
     *
     * OAuth scope required: *modposts*
     *
     * @param fullname The fullname of the post
     * @param sticky True to sticky the post
     * @param apiType The string "json"
     * @return The response will not hold any data, but potential errors will be handled by the
     * [JsonResponse]
     */
    @POST("api/set_subreddit_sticky")
    @FormUrlEncoded
    suspend fun stickyPost(
            @Field("id") fullname: String,
            @Field("state") sticky: Boolean,
            @Field("api_type") apiType: String
    ) : Response<JsonResponse<Void>>


    /**
     * Ignore reports on a post or comment
     *
     * @param fullname The fullname of the post or comment
     * @return The response will not hold any data, but potential errors will be handled by the
     * [JsonResponse]
     */
    @POST("api/ignore_reports")
    @FormUrlEncoded
    suspend fun ignoreReports(
            @Field("id") fullname: String
    ) : Response<JsonResponse<Void>>

    /**
     * Undo ignore reports on a post or comment
     *
     * @param fullname The fullname of the post or comment
     * @return The response will not hold any data, but potential errors will be handled by the
     * [JsonResponse]
     */
    @POST("api/ignore_reports")
    @FormUrlEncoded
    suspend fun unignoreReports(
            @Field("id") fullname: String
    ) : Response<JsonResponse<Void>>
}