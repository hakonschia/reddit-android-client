package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.JsonResponseKt
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Service interface towards a comment on Reddit
 */
interface CommentService : VoteService, ReplyService, SaveService, ModService {


    /**
     * Edits a comment
     *
     * OAuth scope required: *edit*
     *
     * @param fullname The fullname of the comment to edit
     * @param text The updated text
     * @param apiType The string "json"
     * @return A [JsonResponseKt] holding a [RedditComment]
     */
    @POST("api/editusertext")
    @FormUrlEncoded
    suspend fun edit(
            @Field("thing_id") fullname: String,
            @Field("text") text: String,
            @Field("api_type") apiType: String
    ) : Response<JsonResponseKt<RedditComment>>

    /**
     * Delete a comment
     *
     * OAuth scope required: *edit*
     *
     * @param fullname The fullname of the comment to delete
     * @return The response returns no data
     */
    @POST("api/del")
    @FormUrlEncoded
    suspend fun delete(@Field("id") fullname: String) : Response<Any>
}