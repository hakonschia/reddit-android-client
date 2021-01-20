package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Model for an Imgur album
 */
class ImgurAlbum {
    @SerializedName("data")
    private val data: Data? = null

    private class Data {
        @SerializedName("images")
        val images: List<Image>? = null
    }

    /**
     * @return The list of images in this album (this size can be 1)
     */
    val images: List<Image>?
        get() = data?.images
}