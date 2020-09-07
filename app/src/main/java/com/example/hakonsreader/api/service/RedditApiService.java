package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.NetworkConstants;

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


    @GET(NetworkConstants.USER_INFO_PATH)
   // @Headers("User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)")
    Call<User> getUserInfo(@Header("Authorization") String token);

    /**
     * Retrieves posts from Reddit
     *
     * @param url The URL to retrieve posts from
     *            <p>The URL format for front page for not logged in user or a subreddit is: https://reddit.com/.json</p>
     *            <p>The URL for front page for logged in user is: https://oauth.reddit.com with authentication header</p>
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET
  //  @Headers("User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)")
    Call<RedditPostResponse> getPosts(@Url String url,
                                      @Query("after") String after,
                                      @Query("count") int count,
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
    @POST(NetworkConstants.VOTE_PATH)
    @FormUrlEncoded
    Call<Void> vote(@Field("id") String id,
                            @Field("dir") int dir,
                            @Header("Authorization") String accessToken
    );
}
