package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.responses.ListingResponse
import retrofit2.Response
import retrofit2.http.*

interface MessageService {

    /**
     * Gets messages
     *
     * OAuth scope required: `privatemessages`
     *
     * @param where One of: *inbox*, *unread*, *sent*
     */
    @GET("message/{where}?raw_json=1")
    suspend fun getMessages(
            @Path("where") where: String,
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
    ) : Response<ListingResponse<RedditMessage>>


    /**
     * Mark inbox messages as read
     *
     * @param fullnames A comma separated list of the fullnames to mark as read
     */
    @POST("api/read_message")
    @FormUrlEncoded
    suspend fun markRead(
            @Field("id") fullnames: String
    ) : Response<Void>
}