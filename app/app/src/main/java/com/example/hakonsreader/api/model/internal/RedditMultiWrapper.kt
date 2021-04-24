package com.example.hakonsreader.api.model.internal

import com.example.hakonsreader.api.model.RedditMulti
import com.google.gson.annotations.SerializedName

data class RedditMultiWrapper(
        @SerializedName("data")
        val mutli: RedditMulti
)