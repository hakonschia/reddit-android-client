package com.example.hakonsreader.api.responses

import com.example.hakonsreader.api.jsonadapters.ListingListAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * Response for any request that returns a list of listings
 */
class ListingResponseKt<T> {

    @SerializedName("errors")
    private var errors: List<List<String>>? = null

    @SerializedName("data")
    private var data: Data? = null

    inner class Data {
        // For "more comments" kind of responses the array is called "things" instead of "children"
        @SerializedName(value = "children", alternate = ["things"])
        @JsonAdapter(ListingListAdapter::class)
        var listings: List<T>? = null
    }

    public fun getListings() : List<T>? {
        return data?.listings
    }

    public fun hasErrors() : Boolean {
        return !errors.isNullOrEmpty()
    }

    public fun getErrors() : List<List<String>>? {
        return errors
    }
}