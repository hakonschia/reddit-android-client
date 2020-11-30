package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.interfaces.ReplyableListing;

/**
 * Interface used to indicate that a listing can be replied to (posts and comments)
 */
public interface OnReplyListener {
    void replyTo(ReplyableListing listing);
}
