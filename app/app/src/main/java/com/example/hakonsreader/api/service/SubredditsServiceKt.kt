package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ListingResponse
import com.example.hakonsreader.api.responses.ListingResponseKt
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SubredditsServiceKt {

    /**
     * Retrieve a list of the users subscribed subreddits
     *
     * @param after
     * @param count
     * @param limit
     * @return
     */
    @GET("subreddits/mine/subscriber?raw_json=1")
    suspend fun getSubscribedSubreddits(
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
    ) : Response<ListingResponseKt<Subreddit>>


    /**
     * Retrieve a list of the the default subreddits (as set by reddit)
     *
     * @param after
     * @param count
     * @param limit
     * @return
     */
    @GET("subreddits/default?raw_json=1")
    suspend fun getDefaultSubreddits(
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
    ) : Response<ListingResponseKt<Subreddit>>
}