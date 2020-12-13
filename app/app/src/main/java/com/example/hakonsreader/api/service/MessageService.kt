package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.responses.ListingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageService {

    /**
     * Gets messages
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
}