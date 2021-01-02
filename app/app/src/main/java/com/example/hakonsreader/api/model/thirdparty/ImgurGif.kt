package com.example.hakonsreader.api.model.thirdparty

import com.example.hakonsreader.api.interfaces.ThirdPartyGif
import com.google.gson.annotations.SerializedName

class ImgurGif : ThirdPartyGif {

    /**
     * The link to the MP4 URL of the GIF
     */
    @SerializedName("mp4")
    override var mp4Url = ""

    /**
     * The size of the MP4 version of the GIF (in bytes)
     */
    @SerializedName("mp4_size")
    override var mp4Size = 0

    /**
     * True if the GIF has audio
     */
    @SerializedName("has_sound")
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

    /**
     * Imgur gifs do not give a thumbnail, so this will be empty
     */
    override var thumbnail = ""
}