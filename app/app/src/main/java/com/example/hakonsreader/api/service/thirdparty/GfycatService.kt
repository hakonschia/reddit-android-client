package com.example.hakonsreader.api.service.thirdparty

import com.example.hakonsreader.api.model.thirdparty.GfycatGifOuter
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Service towards Gfycat/Redgifs
 */
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