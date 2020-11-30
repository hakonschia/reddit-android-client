package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.model.RedditListing

/**
 * Interface for a class extending [RedditListing] that is voteable
 */
interface VoteableListing {

    /**
     * The ID of the listing
     */
    var id: String

    /**
     * The score of the listing
     */
    var score: Int

    /**
     * True if the score of the listing should be hidden
     */
    var isScoreHidden: Boolean

    /**
     * The vote type of the listing
     */
    var voteType: VoteType

}