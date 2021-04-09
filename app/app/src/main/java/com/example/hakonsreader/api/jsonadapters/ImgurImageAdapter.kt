package com.example.hakonsreader.api.jsonadapters

import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurGif
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurImage
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurImageBase
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ImgurImageAdapter : JsonDeserializer<ImgurImage> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ImgurImage {
        return if (json.asJsonObject.has("mp4")) {
            context.deserialize(json, ImgurGif::class.java)
        } else {
            context.deserialize(json, ImgurImageBase::class.java)
        }
    }
}