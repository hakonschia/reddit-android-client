package com.example.hakonsreader.api.model.flairs

import com.google.gson.annotations.SerializedName

/**
 * A richtext flair is a flair combined of multiple types of items, such as images and text.
 * This class represents one of the items in a richtext flair
 */
class RichtextFlair {
    @SerializedName("e")
    val type: String? = null

    @SerializedName("t")
    val text: String? = null

    @SerializedName("u")
    val url: String? = null
}