package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.responses.MoreCommentsResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Service towards the Reddit API
 */
public interface RedditApiService {

    /**
     * Retrieves information about the logged in user. For information about any user see
     * {@link RedditApiService#getUserInfoOtherUsers(String, String)}
     *
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call with {@link User}
     */
    @GET("api/v1/me")
    Call<User> getUserInfo(@Header("Authorization") String accessToken);

    /**
     * Retrieves information about a user. This can retrieve information about users that aren't
     * the logged in user
     *
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call with {@link User}
     */
    @GET("u/{username}/about")
    Call<RedditListing> getUserInfoOtherUsers(
            @Path("username") String username,

            @Header("Authorization") String accessToken
    );


    /**
     * Retrieves posts from Reddit
     *
     *
     * @param subreddit The subreddit to get posts in. Must prefix with "r/" unless posts are from front page
     *                  (then just use an empty string)
     * @param sort How to sort the post (new, best etc.)
     * @param rawJson Set to 1 if the response should be raw JSON
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve posts without a logged in user
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET("{subreddit}/{sort}")
    Call<ListingResponse> getPosts(
            @Path("subreddit") String subreddit,
            @Path("sort") String sort,
            @Query("after") String after,
            @Query("count") int count,
            @Query("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );


    /* ---------------- Comments ---------------- */
    /**
     * Retrieves comments for a post
     *
     * @param postID The ID of the post to retrieve comments for
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A list of {@link ListingResponse}. The first item in this list is the post itself.
     * The actual comments is found in the second element of the list
     */
    @GET("comments/{postID}")
    Call<List<ListingResponse>> getComments(
            @Path("postID") String postID,
            @Query("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );

    /**
     * Retrieves more comments (from "4 more comments" comments)
     *
     * @param children A comma separated string of the IDs of the comments to load
     * @param linkId The fullname of the post the comments are in
     * @param apiType The string "json"
     * @param rawJson Set to 1 if the response should be raw JSON
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A call with {@link MoreCommentsResponse}
     */
    @POST("api/morechildren")
    @FormUrlEncoded
    Call<MoreCommentsResponse> getMoreComments(
            @Field("children") String children,
            @Field("link_id") String linkId,
            @Field("api_type") String apiType,
            @Field("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );

    /**
     * Submit a new comment to either a post, another comment (reply), or a private message.
     *
     * @param comment The raw markdown of the comment
     * @param parentId The fullname of the thing being replied to
     * @param apiType The string "json"
     * @param returnJson The boolean value "false". MUST be set to false
     * @param accessToken The type of token + the actual token. Form: "type token".
     * @return A call that holds the newly created comment
     */
    @POST("api/comment")
    @FormUrlEncoded
    Call<MoreCommentsResponse> postComment(
            @Field("text") String comment,
            @Field("thing_id") String parentId,
            @Field("api_type") String apiType,
            @Field("return_rtjson") boolean returnJson,

            @Header("Authorization") String accessToken
    );
    /* ---------------- End comments ---------------- */


    /**
     * Cast a vote on something
     *
     * @param id The ID of the something to vite on
     * @param dir The direction of the vote (1 = upvote, -1 = downvote, 0 = remove previous vote)
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call object ready to cast a vote
     */
    @POST("api/vote")
    @FormUrlEncoded
    Call<Void> vote(
            @Field("id") String id,
            @Field("dir") int dir,

            @Header("Authorization") String accessToken
    );


    /**
     * Retrieve a list of the users subscribed subreddits
     *
     * @param after
     * @param count
     * @param limit
     * @param accessToken
     * @return
     */
    @GET("subreddits/mine/subscriber?raw_json=1")
    Call<ListingResponse> getSubscribedSubreddits(
            @Query("after") String after,
            @Query("count") int count,
            @Query("limit") int limit,

            @Header("Authorization") String accessToken
    );

    /**
     * Retrieve a list of the the default subreddits (as set by reddit)
     *
     * @param after
     * @param count
     * @param limit
     * @param accessToken
     * @return
     */
    @GET("subreddits/default?raw_json=1")
    Call<ListingResponse> getDefaultSubreddits(
            @Query("after") String after,
            @Query("count") int count,
            @Query("limit") int limit,

            @Header("Authorization") String accessToken
    );


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


    /**
     *
     * <p>Requires "history" OAuth scope</p>
     *
     * @param username The username of the user to get listings for
     * @param what What kind of listings to get. Values possible:
     *             <ol>
     *                  <li>overview - A mix of comments and posts</li>
     *                  <li>submitted - Posts by the user</li>
     *                  <li>comments - Comments by the user</li>
     *                  <li>upvoted - Posts/comments upvoted by the user. This is only accessible for the user itself</li>
     *                  <li>downvoted - Posts/comments downvoted by the user. This is only accessible for the user itself</li>
     *                  <li>hidden - Posts/comments hidden by the user. This is only accessible for the user itself</li>
     *                  <li>saved - Posts/comments saved by the user. This is only accessible for the user itself</li>
     *             </ol>
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A ListingResponse call. The listings returned depend on the value of "what" (comments, post, etc.)
     */
    @GET("user/{username}/{what}?raw_json=1")
    Call<ListingResponse> getListingsFromUser(
            @Path("username") String username,
            @Path("what") String what,

            @Header("Authorization") String accessToken
    );
}
