package com.example.hakonsreader.api.service

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Service interface for listings that can be voted on
 */
interface VoteServiceKt {

    /**
     * Cast a vote on a something
     *
     * OAuth scope required: *vote*
     *
     * @param fullname The fullname of the thing to vote on
     * @param dir The direction to vote. 1 = upvote, -1 = downvote, 0 remove previous vote
     * @return The Response body for this will be empty on successful requests
     */
    @POST("api/vote")
    @FormUrlEncoded
    suspend fun vote(
            @Field("id") fullname: String,
            @Field("dir") dir: Int
    ) : Response<Any>

}