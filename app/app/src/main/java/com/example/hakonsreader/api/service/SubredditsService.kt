package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ListingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SubredditsService {

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
    ) : Response<ListingResponse<Subreddit>>


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
    ) : Response<ListingResponse<Subreddit>>


    /**
     * Search for subreddits
     *
     * @param query The search query
     * @return A Response holding a list of subreddits
     */
    @GET("subreddits/search?raw_json=1")
    suspend fun search(
            @Query("q") query: String,
            @Query("include_over_18") includeNsfw: Boolean
    ) : Response<ListingResponse<Subreddit>>


    /**
     * Retrieves a list of the trending subreddits for the day
     * The value for trending subreddits is updated once per day
     *
     * @param url DO NOT CHANGE
     */
    // For some reason, using the normal authenticated url with oauth.reddit.com gives
    // 400 errors, so this is the workaround
    @GET("")
    suspend fun getTrendingSubreddits(
            @Url url: String = "https://reddit.com/api/trending_subreddits.json"
    ) : Response<TrendingSubreddits>
}