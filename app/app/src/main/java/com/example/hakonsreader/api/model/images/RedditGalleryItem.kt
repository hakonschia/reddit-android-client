package com.example.hakonsreader.api.model.images

import com.example.hakonsreader.api.interfaces.GalleryImage
import com.google.gson.annotations.SerializedName

/**
 * An individual gallery item in Reddit galleries
 */
data class RedditGalleryItem(

    /**
     * The status of image. This will be [STATUS_FAILED] or [STATUS_VALID]. If this is [STATUS_FAILED]
     * then the all values will be `null` and the item has nothing to display
     */
    @SerializedName("status")
    val status: String,

    /**
     * The mime type for the gallery item
     *
     * Note: If this is `null`, then the gallery item is empty and has nothing to display
     */
    @SerializedName("m")
    val mimeType: String?,

    /**
     * The source gallery image
     *
     * @see resolutions
     */
    @SerializedName("s")
    val source: RedditGalleryImage,

    /**
     * The resolutions for the gallery item. Note that if the gallery item is a GIF/MP4, the items
     * in this list will still be populated with [RedditGalleryImage.url], not
     * [RedditGalleryImage.gifUrl] or [RedditGalleryImage.mp4Url]
     */
    @SerializedName("p")
    val resolutions: List<RedditGalleryImage>,

    /**
     * The optional caption for the gallery image
     */
    @SerializedName("caption")
    var caption: String?,

    /**
     * The optional outbound URL for the gallery image
     */
    @SerializedName("outbound_url")
    var outboundUrl: String? = null,
) : GalleryImage {
    companion object {
        const val STATUS_VALID = "valid"
        const val STATUS_FAILED = "failed"
    }

    override val height: Int
        get() = source.height

    override val width: Int
        get() = source.width
}