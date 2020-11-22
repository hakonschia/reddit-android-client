package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ListingResponse
import com.example.hakonsreader.api.responses.ListingResponseKt
import retrofit2.Response
import retrofit2.http.*

/**
 * Service interface to make API calls towards a subreddit
 */
interface SubredditServiceKt {

    /**
     * Retrieve information about a subreddit
     *
     * @param subreddit The name of the subreddit to get info from
     * @return A Response holding a [Subreddit] object
     */
    @GET("r/{subreddit}/about?raw_json=1")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String) : Response<Subreddit>

    /**
     * Retrieves posts from a subreddit
     *
     * @param subreddit The subreddit to get posts in. Must prefix with "r/" unless posts are from front page
     *                  (then just use an empty string)
     * @param sort How to sort the post (new, best etc.)
     * @param rawJson Set to 1 if the response should be raw JSON
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET("{subreddit}/{sort}?raw_json=1/")
    suspend fun getPosts(
            @Path("subreddit") subreddit: String,
            @Path("sort") sort: String,
            @Query("t") timeSort: String,
            @Query("after") after: String,
            @Query("count") count: Int
    ) : Response<ListingResponseKt<RedditPost>>

    /**
     * Subscribe or unsubscribe to a subreddit
     *
     * @param action "sub" to subscribe, "unsub" to unsubscribe
     * @param subredditName The name of the subreddit to favorite/un-favorite
     */
    @POST("api/subscribe")
    @FormUrlEncoded
    suspend fun subscribeToSubreddit(
            @Field("action") action: String,
            @Field("sr_name") subredditName: String
    ) : Response<Nothing>

    /**
     * Favorite or un-favorite a subreddit
     *
     * @param favorite True to favorite, false to un-favorite
     * @param subredditName The name of the subreddit to favorite/un-favorite
     */
    @POST("api/favorite")
    @FormUrlEncoded
    suspend fun favoriteSubreddit(
            @Field("make_favorite") favorite: Boolean,
            @Field("sr_name") subredditName: String,
    ) : Response<Nothing>
}