package com.example.hakonsreader.api.service;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Service interface for listings that can be voted on
 */
public interface VoteService {
    /**
     * Cast a vote on something
     *
     * <p>OAuth scope required: {@code vote}</p>
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
}
