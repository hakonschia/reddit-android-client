package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Class representing an image in a Reddit post. This includes both gallery items, and preview images
 * (different quality images for image posts, or thumbnails). This might also be linking to a gif URL
 */
class Image {
    /**
     * @return The URL to the image
     */
    // Gallery images use u, x, y. Other images like preview images use full names
    // Imgur images use width, height and "link" instead of "url"
    @SerializedName(value = "url", alternate = ["u", "link"])
    val url: String? = null

    /**
     * @return The width of the image
     */
    @SerializedName(value = "width", alternate = ["x"])
    val width = 0

    /**
     * @return The height of the image
     */
    @SerializedName(value = "height", alternate = ["y"])
    val height = 0

    /**
     * @return Gets the image mimetype
     */
    @SerializedName("m")
    val mimeType: String? = null

    /**
     * If this is image is in a Reddit gallery and the image is a gif, this will contain the link to
     * the mp4 URL
     */
    @SerializedName("gif")
    val gifUrl: String? = null

    /**
     * If this is image is in a Reddit gallery and the image is a gif, this will contain the link to
     * the mp4 URL
     */
    @SerializedName("mp4")
    val mp4Url: String? = null

    val isGif: Boolean
        get() = gifUrl != null


    /**
     * The optional caption for a Reddit gallery image
     */
    @SerializedName("caption")
    var caption: String? = null

    /**
     * The optional outbound URL for a Reddit gallery image
     */
    @SerializedName("outbound_url")
    var outboundUrl: String? = null
}