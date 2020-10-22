package com.example.hakonsreader.interfaces;

/**
 * Listener for when a subreddit is selected in the list of subreddits
 */
public interface OnSubredditSelected {

    /**
     * @param subredditName The name of the subreddit selected
     */
    void subredditSelected(String subredditName);

}
