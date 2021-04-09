package com.example.hakonsreader.api.model.images

import com.example.hakonsreader.api.interfaces.Image
import com.google.gson.annotations.SerializedName


/**
 * An image from a Reddit gallery
 */
data class RedditGalleryImage(
        @SerializedName("u")
        override val url: String,

        @SerializedName("y")
        override val height: Int,

        @SerializedName("x")
        override val width: Int,

        /**
         * If the gallery image is an MP4/gif, this will point to the MP4 URL
         *
         * @see gifUrl
         */
        @SerializedName("mp4")
        val mp4Url: String?,

        /**
         * If the gallery image is an MP4/gif, this will point to the GIF URL
         *
         * @see mp4Url
         */
        @SerializedName("gif")
        val gifUrl: String?
) : Image