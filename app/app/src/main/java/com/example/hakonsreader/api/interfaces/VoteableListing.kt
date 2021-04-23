package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.model.RedditListing

/**
 * Interface for a [RedditListing] that can be voted on
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
     * The internal value used for [voteType]
     *
     * True = upvote
     * False = downvote
     * Null = no vote
     */
    var liked: Boolean?

    /**
     * The author/poster of the listing
     */
    var author: String

    /**
     * The vote type of the listing. Setting this value will automatically update [score]
     */
    var voteType: VoteType
        get() {
            return when (liked) {
                true -> VoteType.UPVOTE
                false -> VoteType.DOWNVOTE
                null -> VoteType.NO_VOTE
            }
        }
        set(value) {
            // Don't do anything if there is no update to the vote
            if (value == voteType) {
                return
            }

            // Going from upvote to downvote: -1 - 1 = -2
            // Going from downvote to upvote: 1 - (-1) = 2
            // Going from downvote to no vote: 0 - (-1) = 1

            // Going from upvote to downvote: -1 - 1 = -2
            // Going from downvote to upvote: 1 - (-1) = 2
            // Going from downvote to no vote: 0 - (-1) = 1
            val difference: Int = value.value - voteType.value

            score += difference

            // Update the internal data as that is used in getVoteType
            liked = when (value) {
                VoteType.UPVOTE -> true
                VoteType.DOWNVOTE -> false
                VoteType.NO_VOTE -> null
            }
        }

    /**
     * True if the listing is archived
     *
     * Archived listings cannot be voted on, and should therefore not attempt to make API requests
     * to vote, as the will always fail
     */
    var isArchived: Boolean

}