package com.example.hakonsreader.api.responses

import com.google.gson.annotations.SerializedName

/**
 * Identical to [ListingResponse], but will only include one listing, not multiple
 */
class SingleListingResponse<T> {


    @SerializedName("errors")
    private var errors: List<List<String>>? = null

    @SerializedName("data")
    private var data: T? = null

    fun getListing() : T? {
        return data
    }

    fun hasErrors() : Boolean {
        return !errors.isNullOrEmpty()
    }

    fun getErrors() : List<List<String>>? {
        return errors
    }

}