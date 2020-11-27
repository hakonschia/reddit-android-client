package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ListingResponseKt
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.Submission
import com.example.hakonsreader.api.responses.JsonResponseKt
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
    // TODO reply notifications
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

            @Field("api_type") apiType: String = "json",
            @Field("raw_json") rawJson: Int = 1
    ) : Response<JsonResponseKt<Submission>>
}