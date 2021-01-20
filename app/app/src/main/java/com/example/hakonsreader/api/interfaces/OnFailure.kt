package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.responses.GenericError

/**
 * Interface used for failures (like network issues) from API requests
 */
fun interface OnFailure {
    /**
     * Called when a request failed due to an issue such as a network failure
     *
     * @param error An object with error information (status code, reason, and message)
     * @param t A throwable with error information
     */
    fun onFailure(error: GenericError?, t: Throwable?)
}