package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPost;
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

    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6", "User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)"})
    @FormUrlEncoded
    Call<AccessToken> getAccessToken(
            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri
    );

    @POST(NetworkConstants.ACCESS_TOKEN_PATH)
    @Headers({"Authorization: Basic VVozN3E5VVMwSDJFb1E6", "User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)"})
    @FormUrlEncoded
    Call<AccessToken> refreshToken(
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );




    @GET("v1/me")
    @Headers("User-Agent: android:com.example.hakonsreader.v0.0.0 (by /u/hakonschia)")
    Call<User> getUserInfo(@Header("Authorization") String token);

    @GET
    Call<List<RedditPost>> getPosts(@Url String url);
}
