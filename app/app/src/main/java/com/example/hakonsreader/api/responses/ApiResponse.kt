package com.example.hakonsreader.api.responses

/**
 * Class for API responses
 */
sealed class ApiResponse<out T> {
    /**
     * For when the API response was successful
     *
     * @param value The value returned from the request
     */
    data class Success<out T>(val value: T): ApiResponse<T>()

    /**
     * For when the API request failed
     *
     * @param error The [GenericError] of the request
     * @param throwable The [Throwable] of the request
     */
    data class Error(val error: GenericError, val throwable: Throwable): ApiResponse<Nothing>()
}