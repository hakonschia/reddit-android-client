package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model for an Imgur album
 */
public class ImgurAlbum {

    @SerializedName("data")
    private Data data;


    private static class Data {
        @SerializedName("images")
        private List<Image> images;
    }

    /**
     * @return The list of images in this album (this size can be 1)
     */
    public List<Image> getImages() {
        return data.images;
    }
}
