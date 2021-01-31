package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for a [RedditListing] that has reports
 */
interface ReportableRequest {

    /**
     * Ignore the reports on the listing
     *
     * @return No response data is returned
     */
    suspend fun ignoreReports() : ApiResponse<Any?>

    /**
     * Unignore the reports on the listing
     *
     * @return No response data is returned
     */
    suspend fun unignoreReports() : ApiResponse<Any?>

}