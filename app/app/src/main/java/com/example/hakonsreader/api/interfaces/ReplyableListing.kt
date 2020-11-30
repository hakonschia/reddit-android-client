package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditListing

/**
 * Interface for a class extending [RedditListing] that is replyable
 */
interface ReplyableListing {

    /**
     * The ID of the listing
     */
    var id: String

    /**
     * What kind of listing is being replied to
     */
    var kind: String

    /**
     * The author of the listing being replied to
     */
    var author: String
}