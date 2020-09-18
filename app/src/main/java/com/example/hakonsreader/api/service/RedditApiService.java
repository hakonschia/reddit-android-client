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
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Service towards the Reddit API
 */
public interface RedditApiService {
    /* --------------------- API paths --------------------- */

    /**
     * The API path used to retrieve user information
     */
    String USER_INFO_PATH = "v1/me";

    /**
     * The API path used to vote on things (posts, comments)
     */
    String VOTE_PATH = "vote";

    /**
     * The API path used to get more comments
     */
    String MORE_COMMENTS_PATH = "morechildren";


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
     * @param url The URL to retrieve posts from
     *            <p>The URL format for front page for not logged in user or a subreddit is: https://reddit.com/.json</p>
     *            <p>The URL for front page for logged in user is: https://oauth.reddit.com with an authentication header</p>
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve posts without a logged in user
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET
    Call<RedditPostsResponse> getPosts(@Url String url,
                                       @Query("after") String after,
                                       @Query("count") int count,
                                       @Query("raw_json") int rawJson,

                                       @Header("Authorization") String accessToken
    );


    /**
     * Retrieves comments for a post
     *
     * @param url The URL to retrieve comments for
     *            <p>Format: reddit.com/{post ID}.json</p>
     *            <p>For upvote status for a user use oauth.reddit.com</p>
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A list of {@link RedditCommentsResponse}. Note that this is a list since the first element
     * returned is the post itself. The actual comments is found in the second element of the list
     */
    @GET
    Call<List<RedditCommentsResponse>> getComments(@Url String url,
                                                   @Query("show") String show,
                                                   @Query("raw_json") int rawJson,

                                                   @Header("Authorization") String accessToken
    );

    /**
     * Retrieves more comments (from "4 more comments" comments)
     *
     * @param apiType The string "json"
     * @param children A comma separated string of the IDs of the comments to load
     * @param linkId The fullname of the post the comments are in
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return A call with {@link MoreCommentsResponse}
     */
    @POST(MORE_COMMENTS_PATH)
    @FormUrlEncoded
    Call<MoreCommentsResponse> getMoreComments(
            @Field("api_type") String apiType,
            @Field("children") String children,
            @Field("link_id") String linkId,
            @Field("raw_json") int rawJson,

            @Header("Authorization") String accessToken
    );


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
    Call<Void> vote(@Field("id") String id,
                    @Field("dir") int dir,

                    @Header("Authorization") String accessToken
    );
}
