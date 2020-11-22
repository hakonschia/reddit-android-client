package com.example.hakonsreader.api.utils

import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.google.gson.Gson
import retrofit2.Response

fun <T> apiError(resp: Response<T>) : ApiResponse.Error {
    val errorBody = Gson().fromJson(resp.errorBody()?.string(), GenericError::class.java)
    return ApiResponse.Error(errorBody, Throwable("Error executing request: ${resp.code()}"))
}