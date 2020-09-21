package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.responses.MoreCommentsResponse;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.example.hakonsreader.api.responses.RedditPostsResponse;

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
    /* --------------------- API paths --------------------- */

    /**
     * The API path used to retrieve user information
     */
    String USER_INFO_PATH = "api/v1/me";

    /**
     * The API path used to vote on things (posts, comments)
     */
    String VOTE_PATH = "api/vote";

    /**
     * The API path used to get more comments
     */
    String MORE_COMMENTS_PATH = "api/morechildren";

    /**
     * API path used to submit a comment, either to a post, a reply to another comment, or a private message
     */
    String COMMENT = "api/comment";


    /**
     * Retrieves information about the logged in user
     *
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call with {@link User}
     */
    @GET(USER_INFO_PATH)
    Call<User> getUserInfo(@Header("Authorization") String accessToken);

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
    @GET("{subreddit}/{sort}.json")
    Call<RedditPostsResponse> getPosts(
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
     * @return A list of {@link RedditCommentsResponse}. Note that this is a list since the first element
     * returned is the post itself. The actual comments is found in the second element of the list
     */
    @GET("comments/{postID}.json")
    Call<List<RedditCommentsResponse>> getComments(
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
    @POST(MORE_COMMENTS_PATH)
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
    @POST(COMMENT)
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
    @POST(VOTE_PATH)
    @FormUrlEncoded
    Call<Void> vote(
            @Field("id") String id,
            @Field("dir") int dir,

            @Header("Authorization") String accessToken
    );
}
