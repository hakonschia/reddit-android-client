package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.constants.NetworkConstants;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface RedditService {

    /**
     * Retrieves the OAuth access token
     * @param code
     * @param grantType
     * @param redirectUri
     * @return
     */
    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6"})
    @FormUrlEncoded
    Call<AccessToken> getAccessToken(
            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri
    );

    /**
     * Refreshes the OAuth access token
     *
     * @param refreshToken The refresh token received in the initial access token retrieval
     * @param grantType The grant type (refresh_token)
     * @return
     */
    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6"})
    @FormUrlEncoded
    Call<AccessToken> refreshToken(
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );


    @GET("v1/me")
   // @Headers("User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)")
    Call<User> getUserInfo(@Header("Authorization") String token);

    /**
     * Retrieves posts from Reddit
     * TODO paging (infinite reading)
     *
     * @param url The URL to retrieve posts from
     *            <p>The URL format for front page for not logged in user or a subreddit is: https://reddit.com/.json</p>
     *            <p>The URL for front page for logged in user is: https://oauth.reddit.com with authentication header</p>
     * @param accessToken The type of token + the actual token. Form: "type token"
     * @return A Call object ready to retrieve posts from a subreddit
     */
    @GET
  //  @Headers("User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)")
    Call<RedditPostResponse> getPosts(@Url String url, @Header("Authorization") String accessToken);
}