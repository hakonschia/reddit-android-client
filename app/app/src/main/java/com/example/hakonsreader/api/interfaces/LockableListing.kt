package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing

/**
 * Interface for a [RedditListing] that can be locked
 */
interface LockableListing {

    var id: String

    var isLocked: Boolean

}