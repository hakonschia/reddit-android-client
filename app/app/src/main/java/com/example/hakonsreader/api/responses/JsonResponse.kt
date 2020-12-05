package com.example.hakonsreader.api.responses

import com.google.gson.annotations.SerializedName

/**
 * Response class for responses that wrap a [ListingResponse] with a "json: {}" object
 *
 * The responses will look like:
 *
 * "json": {
 *     "errors": [],
 *     "data:" {
 *
 *     }
 * }
 *
 */
class JsonResponse<T> {

    @SerializedName("json")
    private var response: ListingResponse<T>? = null

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