package com.example.hakonsreader.api.model.thirdparty

import com.example.hakonsreader.api.interfaces.ThirdPartyGif
import com.google.gson.annotations.SerializedName

/**
 * Class representing a GIF from Gfycat/Redgifs
 */
class GfycatGif : ThirdPartyGif {

    /**
     * The link to the MP4 URL of the GIF
     */
    @SerializedName("mp4Url")
    override var mp4Url = ""

    /**
     * The size of the MP4 version of the GIF (in bytes)
     */
    @SerializedName("mp4Size")
    override var mp4Size = 0

    /**
     * The thumbnail for the GIF
     */
    @SerializedName("posterUrl")
    override var thumbnail = ""

    /**
     * True if the GIF has audio
     */
    @SerializedName("hasAudio")
    override var hasAudio = false

    /**
     * The width of the GIF
     */
    @SerializedName("width")
    override var width = 0

    /**
     * The width of the GIF
     */
    @SerializedName("height")
    override var height = 0
}