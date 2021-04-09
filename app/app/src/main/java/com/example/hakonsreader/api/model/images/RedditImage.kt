package com.example.hakonsreader.api.model.images

import com.example.hakonsreader.api.interfaces.Image
import com.google.gson.annotations.SerializedName

/**
 * Image from Reddit, such as from posts and awards
 */
data class RedditImage(
        @SerializedName("url")
        override val url: String,

        @SerializedName("height")
        override val height: Int,

        @SerializedName("width")
        override val width: Int,
) : Image