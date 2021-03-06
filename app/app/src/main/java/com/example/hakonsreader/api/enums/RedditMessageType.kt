package com.example.hakonsreader.api.enums

/**
 * Private message/inbox message types
 */
enum class RedditMessageType {
    /**
     * When a message is a reply to a comment
     */
    COMMENT_REPLY,

    /**
     * When a message is a reply to a post (top-level comment)
     */
    POST_REPLY,

    UNKNOWN
}