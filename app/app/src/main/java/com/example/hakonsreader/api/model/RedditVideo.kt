package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

class RedditVideo {
    @SerializedName("duration")
    val duration = 0

    /**
     * Gets the fallback URL for the video
     */
    @SerializedName("fallback_url")
    val fallbackUrl: String? = null

    /**
     * Gets the url to the DASH (Dynamic Adaptive Streaming over HTTP) video for the post
     */
    @SerializedName("dash_url")
    val dashUrl: String? = null

    /**
     * Gets the URL to the HLS (HTTP Live Streaming) video for the post
     */
    @SerializedName("hls_url")
    val hlsUrl: String? = null

    /**
     * Gets the height of the video
     */
    @SerializedName("height")
    val height = 0

    /**
     * Gets the width of the video
     */
    @SerializedName("width")
    val width = 0

    /**
     * @return True if the video is a gif
     */
    @SerializedName("is_gif")
    val isGif = false

    /**
     * The bitrate of the video in kilobits per second (kbps)
     */
    @SerializedName("bitrate_kbps")
    val bitrate = 0
}