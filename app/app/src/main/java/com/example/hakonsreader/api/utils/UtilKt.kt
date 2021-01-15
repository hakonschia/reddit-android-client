package com.example.hakonsreader.api.utils

import com.example.hakonsreader.api.enums.ResponseErrors
import com.example.hakonsreader.api.exceptions.ArchivedException
import com.example.hakonsreader.api.exceptions.RateLimitException
import com.example.hakonsreader.api.exceptions.ThreadLockedException
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

/**
 * Handles Reddit errors in the form of:
 * "json": {
 *     "errors": []
 * }
 */
fun apiListingErrors(errors: List<List<String>>)  : ApiResponse.Error {
    // There can be more errors, not sure the best way to handle it other than returning the info for the first
    val errorType: String = errors[0][0]
    val errorMessage: String = errors[0][1]

    // TODO when accessing r/lounge (requires premium): {"reason": "gold_only", "message": "Forbidden", "error": 403}
    // There isn't really a response code for these errors, as the HTTP code is still 200
    return when {
        // TODO should find out if this is a comment or thread and return different exception/message
        ResponseErrors.THREAD_LOCKED.value == errorType -> ApiResponse.Error(GenericError(-1), ThreadLockedException("The thread has been locked"))
        ResponseErrors.RATE_LIMIT.value == errorType -> ApiResponse.Error(GenericError(-1), RateLimitException("The action has been done too many times too fast"))
        ResponseErrors.ARCHIVED.value == errorType -> ApiResponse.Error(GenericError(-1), ArchivedException("The listing has been archived"))
        else -> ApiResponse.Error(GenericError(-1), Exception(String.format("Unknown error: %s; %s", errorType, errorMessage)))
    }
}