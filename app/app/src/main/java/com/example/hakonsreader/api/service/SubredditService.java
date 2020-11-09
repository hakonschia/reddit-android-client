package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.responses.ListingResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Service interface to make subreddit related API calls towards Reddit
 */
public interface SubredditService {

    /**
     * Retrieve information about a subreddit
     *
     * @param subreddit The name of the subreddit to get info from
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A call with a {@link RedditListing} that can be converted to a {@link Subreddit}
     */
    @GET("r/{subreddit}/about?raw_json=1")
    Call<RedditListing> getSubredditInfo(
            @Path("subreddit") String subreddit,

            @Header("Authorization") String accessToken
    );


    /**
     * Retrieves posts from Reddit
     *
     * @param subreddit The subreddit to get posts in. Must prefix with "r/" unless posts are from front page
     *                  (then just use an empty string)
     * @param sort How to sort the post (new, best etc.)
     * @param rawJson Set to 1 if the response should be raw JSON
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve posts without a logged in user
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET("{subreddit}/{sort}?raw_json=1/")
    Call<ListingResponse> getPosts(
            @Path("subreddit") String subreddit,
            @Path("sort") String sort,
            @Query("t") String timeSort,
            @Query("after") String after,
            @Query("count") int count,
            @Query("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );

    /**
     * Subscribe or unsubscribe to a subreddit
     *
     * @param action "sub" to subscribe "unsub" to unsubscribe
     * @param subredditName The name of the subreddit to sub/unsub to
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Void call, does not return any data
     */
    @POST("api/subscribe")
    @FormUrlEncoded
    Call<Void> subscribeToSubreddit(
            @Field("action") String action,
            @Field("sr_name") String subredditName,

            @Header("Authorization") String accessToken
    );

    /**
     * Favorite or un-favorite a subreddit
     *
     * @param subredditName The name of the subreddit
     * @param favorite True to favorite, false to un-favorite
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Void call, does not return any data
     */
    @POST("api/favorite")
    @FormUrlEncoded
    Call<Void> favoriteSubreddit(
            @Field("sr_name") String subredditName,
            @Field("make_favorite") boolean favorite,

            @Header("Authorization") String accessToken
    );
}
