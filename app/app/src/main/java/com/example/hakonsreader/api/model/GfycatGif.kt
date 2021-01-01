package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Class representing a GIF from Gfycat/Redgifs
 */
class GfycatGif {

    /**
     * The link to the MP4 URL of the GIF
     */
    @SerializedName("mp4Url")
    var mp4Url = ""

    /**
     * The size of the MP4 version of the GIF (in bytes)
     */
    @SerializedName("mp4Size")
    var mp4Size = 0

    /**
     * The thumbnail for the GIF
     */
    @SerializedName("posterUrl")
    var thumbnail = ""

    /**
     * True if the GIF has audio
     */
    @SerializedName("hasAudio")
    var hasAudio = false
}