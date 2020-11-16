package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.model.RedditListing;

/**
 * Interface used to indicate that a listing can be replied to (posts and comments)
 */
public interface OnReplyListener {
    void replyTo(RedditListing listing);
}
