package com.example.hakonsreader.api.model

import com.google.gson.annotations.SerializedName

/**
 * Wrapper class for "preview.images" in JSON posts responses
 */
class ImagesWrapper {
    @SerializedName("source")
    var source: Image? = null

    @SerializedName("resolutions")
    var resolutions: List<Image>? = null

    /**
     * For posts such as gifs there is a object called "variants" with links to mp4 and gif URLs
     */
    @SerializedName("variants")
    var variants: PreviewImageVariants? = null

    class PreviewImageVariants {
        @SerializedName("gif")
        var gif: ImagesWrapper? = null

        @SerializedName("mp4")
        var mp4: ImagesWrapper? = null

        @SerializedName("obfuscated")
        var obfuscated: ImagesWrapper? = null
    }
}