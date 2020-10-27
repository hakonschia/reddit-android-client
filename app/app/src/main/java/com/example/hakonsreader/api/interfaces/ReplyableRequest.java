package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.model.RedditComment;

/**
 * Interface for request classes that offers requests for adding replies (reply to post, comment etc)
 *
 * <p>This interface is intended to be used with methods from {@link com.example.hakonsreader.api.RedditApi}
 * to use the same code for replying to different types of listings (comments or posts). </p>
 */
public interface ReplyableRequest {
    void reply(String text, OnResponse<RedditComment> onResponse, OnFailure onFailure);
}
