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
}
