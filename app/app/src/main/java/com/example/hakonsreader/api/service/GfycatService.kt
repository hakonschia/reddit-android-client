package com.example.hakonsreader.api.service

import com.example.hakonsreader.api.model.GfycatGifOuter
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GfycatService {

    @GET("https://api.gfycat.com/v1/gfycats/{gfyid}")
    suspend fun gfycat(
        @Path("gfyid") id: String
    ) : Response<GfycatGifOuter>

    @GET("https://api.redgifs.com/v1/gfycats/{gfyid}")
    suspend fun redgifs(
            @Path("gfyid") id: String
    ) : Response<GfycatGifOuter>

}