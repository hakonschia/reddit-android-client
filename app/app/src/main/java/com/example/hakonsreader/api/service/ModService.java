package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.JsonResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ModService {

    /**
     * Distinguish a post or comment as a moderator
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param fullname The fullname of the post or comment
     * @param how "yes" to distinguish as mod, "no" to remove the mod distinguish
     * @param sticky True to also sticky the post or comment (comments will also be mod distinguished when stickied)
     * @param apiType The string "json"
     * @param accessToken The type of token + the actual token. Form: "type token". This must be for
     *                    a user that is a moderator in the respective subreddit
     * @return A Call with a JsonResponse. The response will hold the updated post or comment
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
