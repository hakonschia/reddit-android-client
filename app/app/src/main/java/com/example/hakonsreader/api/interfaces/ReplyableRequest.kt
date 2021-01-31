package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for a [RedditListing] that can be replied to
 */
interface ReplyableRequest {

    /**
     * Reply to a listing
     *
     * @param text The markdown text of the reply
     * @return An ApiResponse with the comment created
     */
    suspend fun reply(text: String) : ApiResponse<RedditComment>
}