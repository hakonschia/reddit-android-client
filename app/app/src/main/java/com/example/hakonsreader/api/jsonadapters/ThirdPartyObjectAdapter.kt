package com.example.hakonsreader.api.jsonadapters

import com.example.hakonsreader.api.model.thirdparty.GfycatGif
import com.example.hakonsreader.api.model.thirdparty.ImgurGif
import com.google.gson.Gson
import com.google.gson.JsonParser
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

        val treeName = LinkedTreeMap<String, Any>()::class.java.typeName
        val actualName = value::class.java.typeName
        if (treeName != actualName) {
            return
        }

        // The code above will not check for generics, so we either have this unchecked cast warning, or
        // cast it as LinkedTreeMap<*, *>
        value as LinkedTreeMap<String, Any>

        val t = when {
            // Imgur uses "mp4"
            value.containsKey("mp4") -> {
                ImgurGif::class.java.typeName
            }

            // Gfycat/redgifs uses "mp4Url"
            value.containsKey("mp4Url") -> {
                GfycatGif::class.java.typeName
            }

            else -> return
        }

        value.put("type", t)
        val asJsonString = Gson().toJsonTree(value).asJsonObject.toString()

        out?.value(asJsonString)
    }

    override fun read(reader: JsonReader?): Any? {
        val rawString = reader?.nextString()

        val type = when (JsonParser.parseString(rawString).asJsonObject.get("type").asString) {
            ImgurGif::class.java.typeName -> {
                ImgurGif::class.java
            }

            GfycatGif::class.java.typeName -> {
                GfycatGif::class.java
            }

            else -> null
        }

        return Gson().fromJson(rawString, type)
    }
}