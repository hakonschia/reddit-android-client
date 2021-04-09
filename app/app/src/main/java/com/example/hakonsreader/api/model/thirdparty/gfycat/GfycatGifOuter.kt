package com.example.hakonsreader.api.model.thirdparty.gfycat

import com.google.gson.annotations.SerializedName

class GfycatGifOuter {

    // This should be done with an adapter but it just returns null
    @SerializedName("gfyItem")
    var gif: GfycatGif? = null

}