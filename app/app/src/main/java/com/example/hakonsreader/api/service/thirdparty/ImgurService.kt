package com.example.hakonsreader.api.service.thirdparty

import com.example.hakonsreader.api.model.ImgurAlbum
import com.example.hakonsreader.api.model.thirdparty.ImgurGifOuter
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Service for various requests towards the Imgur API
 */
interface ImgurService {

    /**
     * Loads information about an Imgur album
     *
     * @param albumHash The hash of the album to retrieve
     * @return
     */
    @GET("3/album/{albumHash}")
    suspend fun getAlbum(
            @Path("albumHash") albumHash: String
    ): Response<ImgurAlbum>

    /**
     * Loads information about an Imgur image (this can be a Gif)
     *
     * @param imageHash The hash of the image to retrieve information for
     * @return
     */
    @GET("3/image/{imageHash}")
    suspend fun getImage(
            @Path("imageHash") imageHash: String
    ): Response<ImgurGifOuter>
}