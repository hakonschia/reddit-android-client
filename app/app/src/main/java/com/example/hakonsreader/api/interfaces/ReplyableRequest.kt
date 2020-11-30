package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for request classes that offers requests for adding replies (reply to post, comment etc)
 *
 * his interface is intended to be used with methods from [com.example.hakonsreader.api.RedditApi]
 * to use the same code for replying to different types of listings (comments or posts).
 */
interface ReplyableRequest {
    suspend fun reply(text: String) : ApiResponse<RedditComment>
}