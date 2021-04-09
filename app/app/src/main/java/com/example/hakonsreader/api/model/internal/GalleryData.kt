package com.example.hakonsreader.api.model.internal

import com.google.gson.annotations.SerializedName

data class GalleryData (
        @SerializedName("items")
        val data: List<GalleryItemInternal>
)