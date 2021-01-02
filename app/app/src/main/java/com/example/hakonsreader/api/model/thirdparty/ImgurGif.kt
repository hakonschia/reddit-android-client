package com.example.hakonsreader.api.model.thirdparty

import com.google.gson.annotations.SerializedName

class ImgurGif {

    /**
     * The link to the MP4 URL of the GIF
     */
    @SerializedName("mp4")
    var mp4Url = ""

    /**
     * The size of the MP4 version of the GIF (in bytes)
     */
    @SerializedName("mp4_size")
    var mp4Size = 0

    /**
     * True if the GIF has audio
     */
    @SerializedName("has_sound")
    var hasAudio = false
}