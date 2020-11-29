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
    var response: ListingResponseKt<T>? = null

}