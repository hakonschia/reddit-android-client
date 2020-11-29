package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ListingResponseKt
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.Subreddit
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
     * @param subreddit The subreddit to get posts in. Must prefix with "r/" unless posts are
     * from front page (then just use an empty string)
     * @param sort How to sort the post (new, best etc.). This should use [SortingMethods] and its
     * corresponding value
     * @param timeSort How to sort the posts. This should use [PostTimeSort] and its corresponding value
     * @param after The fullname of the last post retrieved
     * @param count The amount of items already fetched
     * @param limit The amount of posts to retrieve
     * @return A Response object which will hold the posts
     */
    @GET("{subreddit}/{sort}?raw_json=1")
    suspend fun getPosts(
            @Path("subreddit") subreddit: String,
            @Path("sort") sort: String,
            @Query("t") timeSort: String,
            @Query("after") after: String,
            @Query("count") count: Int,
            @Query("limit") limit: Int
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