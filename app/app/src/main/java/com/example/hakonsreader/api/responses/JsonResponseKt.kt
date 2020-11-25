package com.example.hakonsreader.api.responses

import com.example.hakonsreader.api.model.RedditListing
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
class JsonResponseKt<T : RedditListing> {

    @SerializedName("json")
    var response: ListingResponseKt<T>? = null

}