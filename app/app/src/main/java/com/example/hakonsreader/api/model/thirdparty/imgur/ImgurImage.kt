package com.example.hakonsreader.api.model.thirdparty.imgur

import com.example.hakonsreader.api.interfaces.GalleryImage
import com.example.hakonsreader.api.interfaces.Image
import com.example.hakonsreader.api.jsonadapters.ImgurImageAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * Base abstract class for Imgur images
 */
@JsonAdapter(ImgurImageAdapter::class)
abstract class ImgurImage : Image, GalleryImage {
    @SerializedName("link")
    override var url: String = ""

    @SerializedName("height")
    override var height: Int = 0

    @SerializedName("width")
    override var width: Int = 0

    @SerializedName("type")
    var mimeType: String = ""
}