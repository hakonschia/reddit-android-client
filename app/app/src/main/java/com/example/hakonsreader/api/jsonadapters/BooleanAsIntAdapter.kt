package com.example.hakonsreader.api.jsonadapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Json adapter that treats a Boolean value as -1
 */
class BooleanAsIntAdapter : TypeAdapter<Int>() {
    override fun read(reader: JsonReader) : Int {

        return try {
            reader.nextBoolean()
            -1
        } catch (e: IllegalStateException) {
            // nextBoolean will throw an exception if it's not a boolean, so return it as an int instead
            reader.nextInt()
        }
    }

    override fun write(out: JsonWriter?, value: Int?) {
        out?.value(value)
    }
}