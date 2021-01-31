package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for a [RedditListing] that can be saved/unsaved
 */
interface SaveableRequest {

    /**
     * Save the listing
     *
     * @return No response data is returned
     */
    suspend fun save() : ApiResponse<Any?>

    /**
     * Unsave the listing
     *
     * @return No response data is returned
     */
    suspend fun unsave() : ApiResponse<Any?>
}