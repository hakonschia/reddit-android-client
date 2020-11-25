package com.example.hakonsreader.api.responses

sealed class ApiResponse<out T> {
    data class Success<out T>(val value: T): ApiResponse<T>()
    data class Error(val error: GenericError, val throwable: Throwable): ApiResponse<Nothing>()
}