package com.example.hakonsreader.api.service

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Service for saving and unsaving posts/comments
 */
interface SaveServiceKt {


    /**
     * Save a post or comment
     *
     * OAuth scope required: *save*
     *
     * @param fullname The fullname of the post or comment
     * @return The Response returned will not include any data
     */
    @POST("api/save")
    @FormUrlEncoded
    suspend fun save(@Field("id") fullname: String) : Response<Void>


    /**
     * Unsave a post or comment
     *
     * OAuth scope required: *save*
     *
     * @param fullname The fullname of the post or comment
     * @return The Response returned will not include any data
     */
    @POST("api/unsave")
    @FormUrlEncoded
    suspend fun unsave(@Field("id") fullname: String) : Response<Void>
}