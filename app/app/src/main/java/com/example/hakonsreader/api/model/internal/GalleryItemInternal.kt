package com.example.hakonsreader.api.model.internal

import com.google.gson.annotations.SerializedName

/**
 * Model for the "items" list in a "gallery_data" Reddit pots JSON
 */
data class GalleryItemInternal(
        /**
         * The media ID of the gallery item
         */
        @SerializedName("media_id")
        val mediaId: String,

        /**
         * The ID of the gallery item
         */
        @SerializedName("id")
        val id: Int,

        /**
         * The URL the gallery item has attached to it
         */
        @SerializedName("outbound_url")
        val outboundUrl: String?,

        /**
         * The caption of the gallery item
         */
        @SerializedName("caption")
        val caption: String?
)
