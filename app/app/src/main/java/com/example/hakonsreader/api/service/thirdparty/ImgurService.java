package com.example.hakonsreader.api.service.thirdparty;

import com.example.hakonsreader.api.model.ImgurAlbum;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Service for various requests towards the Imgur API
 */
public interface ImgurService {


    /**
     * Loads information about an Imgur album
     *
     * @param albumHash The hash of the album to retrieve
     * @return
     */
    @GET("3/album/{albumHash}")
    Call<ImgurAlbum> loadImgurAlbum(
            @Path("albumHash") String albumHash
    );

}
