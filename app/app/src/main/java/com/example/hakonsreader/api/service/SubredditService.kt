package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.responses.ListingResponse
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.responses.JsonResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Service interface to make API calls towards a subreddit
 */
interface SubredditService {

    /**
     * Retrieve information about a subreddit
     *
     * @param subreddit The name of the subreddit to get info from
     * @return A Response holding a [RedditListing] which can be cast into a [Subreddit] object
     */
    @GET("r/{subreddit}/about?raw_json=1")
    suspend fun getSubredditInfo(@Path("subreddit") subreddit: String) : Response<RedditListing>

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
    ) : Response<ListingResponse<RedditPost>>

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
    ) : Response<Void>

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
    ) : Response<Void>


    /**
     * Submit a post to a subreddit
     *
     * OAuth scope required: *submit*
     *
     * @param subredditName The name of the subreddit to submit the post to
     * @param kind One of: *link*, *self*, *crosspost*, *image*, *video*, *videogif*
     * @param title The title of the post (max 300 characters)
     *
     * @param text For *kind* = *self*: The Markdown text of the post. Default to an
     * empty string (can be omit if not a selfpost)
     * @param link For *kind* = *link*: The URL of the post used when *kind* is *link*.
     * Default to an empty string (omit if not a link post)
     * @param crosspostFullname For *kind* = *crosspost*: The fullname of the post to crosspost.
     * Default to an empty string (omit if not a crosspost)
     *
     * @param nsfw True if the post should should be marked as NSFW. Default to *false*
     * @param spoiler True if the post should be marked as a spoiler. Default to *false*
     * @param sendNotifications True if the poster of the post wants to receive notifications on the post.
     * Default to *true*
     * @param resubmit For *kind* = *link*: Resubmit the post is the link is already posted to the subreddit.
     * Default to *true*
     *
     * @param apiType The string *json*
     * @param rawJson The value *1*
     */
    @POST("api/submit")
    @FormUrlEncoded
    suspend fun submit(
            @Field("sr") subredditName: String,
            @Field("kind") kind: String,
            @Field("title") title: String,

            @Field("text") text: String = "",
            @Field("url") link: String = "",
            @Field("crosspost_fullname") crosspostFullname: String = "",

            @Field("nsfw") nsfw: Boolean = false,
            @Field("spoiler") spoiler: Boolean = false,
            @Field("sendreplies") sendNotifications: Boolean = true,
            @Field("resubmit") resubmit: Boolean = true,

            @Field("flair_id") flairId: String = "",

            @Field("api_type") apiType: String = "json",
            @Field("raw_json") rawJson: Int = 1
    ) : Response<JsonResponse<Submission>>


    /**
     * Gets submission/link flairs for a subreddit
     *
     * @param subredditName The name of the subreddit to get flairs for
     * @return A list of [RedditFlair]
     */
    @GET("r/{subreddit}/api/link_flair_v2?raw_json=1")
    suspend fun getLinkFlairs(@Path("subreddit") subredditName: String) : Response<List<RedditFlair>>

    /**
     * Gets user flairs for a subreddit
     *
     * @param subredditName The name of the subreddit to get flairs for
     * @return A list of [RedditFlair]
     */
    @GET("r/{subreddit}/api/user_flair_v2?raw_json=1")
    suspend fun getUserFlairs(@Path("subreddit") subredditName: String) : Response<List<RedditFlair>>

    /**
     * Select a new flair for a user on a subreddit
     *
     * @param subredditName The name of the subreddit to select a flair on
     * @param username The username to select flair for
     * @param flairId The ID of the flair to select
     */
    @POST("r/{subreddit}/api/selectflair?raw_json=1")
    @FormUrlEncoded
    suspend fun selectFlair(
            @Path("subreddit") subredditName: String,
            @Field("name") username: String,
            @Field("flair_template_id") flairId: String,

            @Field("api_type") apiType: String = "json"
    ) : Response<JsonResponse<Any?>>

    /**
     * Enables or disables user flairs on a subreddit
     *
     * @param subredditName The name of the subreddit to enable or disable flairs on
     * @param enable True to enable flairs, false to disable
     */
    @POST("r/{subreddit}/api/setflairenabled?raw_json=1")
    @FormUrlEncoded
    suspend fun enableUserFlair(
            @Path("subreddit") subredditName: String,
            @Field("flair_enabled") enable: Boolean,

            @Field("api_type") apiType: String = "json"
    ) : Response<JsonResponse<Any?>>

    /**
     * Gets the rules of a subreddit
     *
     * @param subredditName The name of the subreddit to get rules for
     */
    @GET("r/{subreddit}/about/rules?raw_json=1")
    suspend fun getRules(@Path("subreddit") subredditName: String) : Response<SubredditRuleInternal>
}