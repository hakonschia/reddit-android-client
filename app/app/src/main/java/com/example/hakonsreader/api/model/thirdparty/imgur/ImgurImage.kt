package com.example.hakonsreader.api.model.thirdparty.imgur

import com.example.hakonsreader.api.interfaces.GalleryImage
import com.example.hakonsreader.api.interfaces.Image
import com.google.gson.annotations.SerializedName

/**
 * Image from Imgur
 */
data class ImgurImage(
        @SerializedName("link")
        override val url: String,

        @SerializedName("height")
        override val height: Int,

        @SerializedName("width")
        override val width: Int,

        @SerializedName("type")
        val mimeType: String
) : Image, GalleryImage