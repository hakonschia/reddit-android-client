package com.example.hakonsreader.api.utils

import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.google.gson.Gson
import retrofit2.Response

/**
 * Convenience method for returning an [ApiResponse.Error]. This takes in a [Response] and
 * converts the error body to a [GenericError] which is sent back in an [ApiResponse.Error], alongside
 * a generic throwable
 *
 * @param resp The response that failed
 */
fun <T> apiError(resp: Response<T>) : ApiResponse.Error {
    val errorBody = Gson().fromJson(resp.errorBody()?.string(), GenericError::class.java)
    return ApiResponse.Error(errorBody, Throwable("Error executing request: ${resp.code()}"))
}