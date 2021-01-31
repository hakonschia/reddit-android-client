package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for a [RedditListing] that can be saved/unsaved
 */
interface LockableRequest {

    /**
     * Locks the listing, meaning it cannot be replied to
     *
     * @return No response data is returned
     */
    suspend fun lock() : ApiResponse<Any?>

    /**
     * Unlocks the listing
     *
     * @return No response data is returned
     */
    suspend fun unlock() : ApiResponse<Any?>
}