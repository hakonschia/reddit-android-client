package com.example.hakonsreader.api.service;

import com.example.hakonsreader.api.responses.JsonResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ModService {

    // Comments and posts work a bit differently. If comments are stickied, they MUST also be distinguished as mod
    // Posts can be stickied WITHOUT being distinguished as mod, and they use a different api call to sticky
    // Distinguishing a post also cannot send a "sticky" parameter, or else it will fail with 400 Bad Request, so
    // we need two different functions for comments and post distinguishing


    /**
     * Distinguish a comment as a moderator
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param fullname The fullname of the comment
     * @param how "yes" to distinguish as mod, "no" to remove the mod distinguish
     * @param sticky True to also sticky the post or comment (comments will also be mod distinguished when stickied)
     * @param apiType The string "json"
     * @return A Call with a JsonResponse. The response will hold the updated comment
     */
    @POST("api/distinguish")
    @FormUrlEncoded
    Call<JsonResponse> distinguishAsModComment(
            @Field("id") String fullname,
            @Field("how") String how,
            @Field("sticky") boolean sticky,
            @Field("api_type") String apiType
    );

    /**
     * Distinguish a post as a moderator
     *
     * <p>OAuth scope required: {@code modposts}</p>
     *
     * @param fullname The fullname of the post
     * @param how "yes" to distinguish as mod, "no" to remove the mod distinguish
     * @param apiType The string "json"
     * @return A Call with a JsonResponse. The response will hold the updated post
     */
    @POST("api/distinguish")
    @FormUrlEncoded
    Call<JsonResponse> distinguishAsModPost(
            @Field("id") String fullname,
            @Field("how") String how,
            @Field("api_type") String apiType
    );


    /**
     * Sticky or unsticky a post
     *
     * @param fullname The fullname of the post. Note this must be the fullname, not just the ID of the post.
     *                 If only the ID is passed, this will not throw any errors, but the post will not be updated
     * @param sticky True to sticky, false to remove the sticky
     * @param apiType The string "json"
     * @return A Call with a JsonResponse. The JsonResponse will not hold any data on successful responses
     */
    @POST("api/set_subreddit_sticky")
    @FormUrlEncoded
    Call<JsonResponse> stickyPost(
            @Field("id") String fullname,
            @Field("state") boolean sticky,
            @Field("api_type") String apiType
    );
}
