package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.model.Subreddit;

/**
 * Listener for when a subreddit is selected in the list of subreddits
 */
public interface OnSubredditSelected {

    /**
     * @param subreddit The subreddit selected
     */
    void subredditSelected(Subreddit subreddit);

}
