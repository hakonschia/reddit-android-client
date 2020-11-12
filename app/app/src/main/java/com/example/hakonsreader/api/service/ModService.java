package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.responses.JsonResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ModService {

    /**
     *
     * @param fullname
     * @param how
     * @param sticky
     * @param apiType The string "json"
     * @param accessToken
     * @return
     */
    @POST("api/distinguish")
    @FormUrlEncoded
    Call<JsonResponse> distinguishAsMod(
            @Field("id") String fullname,
            @Field("how") String how,
            @Field("sticky") boolean sticky,
            @Field("api_type") String apiType,

            @Header("Authorization") String accessToken
    );

}
