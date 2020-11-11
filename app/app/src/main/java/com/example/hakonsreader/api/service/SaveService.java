package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.GenericError;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Service for saving and unsaving posts/comments
 */
public interface SaveService {

    /**
     * Save a post or comment
     *
     * @param fullname The fullname of the post or comment
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return The body for this will always be empty for successful requests
     */
    @POST("api/save")
    @FormUrlEncoded
    Call<Void> save(
            @Field("id") String fullname,

            @Header("Authorization") String accessToken
    );

    /**
     * Unsave a post or comment
     *
     * @param fullname The fullname of the post or comment
     * @param accessToken The type of token + the actual token. Form: "type token". This can be omitted (an empty string)
     *                    to retrieve comments without a logged in user
     * @return The body for this will always be empty for successful requests
     */
    @POST("api/unsave")
    @FormUrlEncoded
    Call<Void> unsave(
            @Field("id") String fullname,

            @Header("Authorization") String accessToken
    );

}
