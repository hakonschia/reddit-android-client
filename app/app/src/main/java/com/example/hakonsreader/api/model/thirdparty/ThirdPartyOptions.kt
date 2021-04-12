package com.example.hakonsreader.api.model.thirdparty


/**
 * Data class for providing options for which third party API calls to make when posts are loaded.
 *
 * All values have default values of `true`
 */
data class ThirdPartyOptions(
        /**
         * Set to true to load Gfycat/Redgifs gifs directly
         */
        var loadGfycatGifs: Boolean = true,

        /**
         * Set to true to load Imgur gifs directly
         */
        var loadImgurGifs: Boolean = true,

        /**
         * Set to true to load Imgur albums/galleries directly
         */
        var loadImgurAlbums: Boolean = true
)
