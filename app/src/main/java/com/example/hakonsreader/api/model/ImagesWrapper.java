package com.example.hakonsreader.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Wrapper class for "preview.images" in JSON posts responses
 */
class ImagesWrapper {
    @SerializedName("source")
    PreviewImage source;

    @SerializedName("resolutions")
    List<PreviewImage> resolutions;


    /**
     * For posts such as gifs there is a object called "variants" with links to mp4 and gif URLs
     */
    @SerializedName("variants")
    PreviewImageVariants variants;

    public static class PreviewImageVariants {
        @SerializedName("gif")
        ImagesWrapper gif;

        @SerializedName("mp4")
        ImagesWrapper mp4;
    }
}
