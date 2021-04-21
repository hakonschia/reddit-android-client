package com.example.hakonsreader.api.jsonadapters

import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurAlbum
import com.example.hakonsreader.api.model.thirdparty.gfycat.GfycatGif
import com.example.hakonsreader.api.model.thirdparty.imgur.ImgurGif
import com.example.hakonsreader.api.utils.thirdPartyObjectFromJsonString
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Type adapter that adds the type of a third party object and returns the correct object back
 * when deserialized
 */
class ThirdPartyObjectAdapter : TypeAdapter<Any>() {
    override fun write(out: JsonWriter?, value: Any?) {
        value ?: return

        val jsonObj = Gson().toJsonTree(value).asJsonObject

        val typeName = value::class.java.simpleName
        val type = when {
            value is ImgurGif -> {
                ImgurGif::class.java.simpleName
            }

            value is ImgurAlbum -> {
                ImgurAlbum::class.java.simpleName
            }

            value is GfycatGif -> {
                GfycatGif::class.java.simpleName
            }

            typeName == LinkedTreeMap<String, Any>()::class.java.simpleName -> {
                value as LinkedTreeMap<String, Any>
                // The code above will not check for generics, so we either have this unchecked cast warning, or
                // cast it as LinkedTreeMap<*, *>
                when {
                    // Imgur uses "mp4"
                    value.containsKey("mp4") -> {
                        ImgurGif::class.java.simpleName
                    }

                    // Gfycat/redgifs uses "mp4Url"
                    value.containsKey("mp4Url") -> {
                        GfycatGif::class.java.simpleName
                    }

                    else -> null
                }
            }

            else -> null
        }

        if (type != null) {
            jsonObj.addProperty("type", type)
        }
        out?.value(jsonObj.toString())
    }

    override fun read(reader: JsonReader?): Any? {
        return reader?.nextString()?.let { thirdPartyObjectFromJsonString(it) }
    }
}