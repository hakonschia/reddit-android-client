package com.example.hakonsreader.api.responses

import com.google.gson.annotations.SerializedName

/**
 * Response class for responses that wrap a [ListingResponseKt] with a "json: {}" object
 *
 * The responses will look like:
 *
 * "json": {
 *      <ListingResponseKt>
 * }
 *
 */
class JsonResponseKt<T> {

    @SerializedName("json")
    private var response: ListingResponseKt<T>? = null

    /**
     * @return The list of more response
     */
    fun getListings(): List<T?>? {
        return response?.getListings()
    }

    fun hasErrors(): Boolean {
        return response?.hasErrors() == true
    }

    fun errors(): List<List<String?>?>? {
        return response?.getErrors()
    }
}