package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditComment;

/**
 * Interface used to communicate that more comments should be loaded
 */
public interface LoadMoreComments {

    /**
     * Called when more comments should be loaded
     *
     * @param comment The comment holding the data for what should be loaded.
     *                {@link RedditComment#getKind()} has to be {@link Thing#MORE}
     *                for this to work
     * @param parent The parent comment for the more comments
     */
    void loadMoreComments(RedditComment comment, RedditComment parent);
}