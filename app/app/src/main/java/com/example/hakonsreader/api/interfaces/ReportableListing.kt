package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing

/**
 * Interface for a [RedditListing] that can be reported
 */
interface ReportableListing {
    /**
     * The ID of the listing
     */
    var id: String

    /**
     * The user reports on the listing.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    var userReports: Array<Array<Any>>?

    /**
     * The dismissed user reports on the listing.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    var userReportsDismissed: Array<Array<Any>>?

    /**
     * The amount of reports the listing has
     */
    var numReports: Int

    /**
     * True if reports are set to be ignored on the listing
     */
    var ignoreReports: Boolean
}