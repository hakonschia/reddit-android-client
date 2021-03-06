package com.example.hakonsreader.api.responses

import com.example.hakonsreader.api.jsonadapters.ListingListAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * Response for any request that returns a list of listings
 */
class ListingResponse<T> {

    @SerializedName("errors")
    private var errors: List<List<String>>? = null

    @SerializedName("data")
    private var data: Data? = null

    inner class Data {
        // For "more comments" kind of responses the array is called "things" instead of "children"
        @SerializedName(value = "children", alternate = ["things"])
        @JsonAdapter(ListingListAdapter::class)
        var listings: List<T>? = null

        @SerializedName("after")
        var after: String? = null
    }

    fun getAfter(): String? {
        return data?.after
    }

    fun getListings() : List<T>? {
        return data?.listings
    }

    fun hasErrors() : Boolean {
        return !errors.isNullOrEmpty()
    }

    fun getErrors() : List<List<String>>? {
        return errors
    }
}