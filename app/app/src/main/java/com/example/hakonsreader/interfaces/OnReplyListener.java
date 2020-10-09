package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.model.RedditListing;

public interface OnReplyListener {
    void replyTo(RedditListing listing);
}
