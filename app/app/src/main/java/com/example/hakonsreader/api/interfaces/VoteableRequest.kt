package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.enums.VoteType
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for request classes that offers requests for voting
 *
 * This interface is intended to be used with methods from [com.example.hakonsreader.api.RedditApi]
 * to use the same code for voting on different types of listings (comments or posts).
 */
/**
 * Interface for request models dealing with listings that implement [VoteableListing]
 */
interface VoteableRequest {

    /**
     * Vote on the listing
     *
     * OAuth scope required: `vote`
     *
     * @param voteType The vote type to update the listing with
     * @return No response data is returned
     */
    suspend fun vote(voteType: VoteType) : ApiResponse<Any?>

}