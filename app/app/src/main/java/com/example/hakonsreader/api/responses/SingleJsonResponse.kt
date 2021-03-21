package com.example.hakonsreader.api.responses

import com.google.gson.annotations.SerializedName

class SingleJsonResponse<T> {
    @SerializedName("json")
    private var response: SingleListingResponse<T>? = null

    /**
     * @return The list of more response
     */
    fun getListing(): T? {
        return response?.getListing()
    }

    fun hasErrors(): Boolean {
        return response?.hasErrors() == true
    }

    fun errors(): List<List<String?>?>? {
        return response?.getErrors()
    }
}