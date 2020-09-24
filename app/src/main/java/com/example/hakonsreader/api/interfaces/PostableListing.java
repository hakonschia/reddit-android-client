package com.example.hakonsreader.api.interfaces;

/**
 * Interface for postable listings, such as posts and comments
 */
public interface PostableListing extends RedditListing {

    /**
     * @return The subreddit the listing is in
     */
    String getSubreddit();

    /**
     * @return The author (poster) of the listing
     */
    String getAuthor();

    /**
     * @return The permalink to the listing
     */
    String getPermalink();

    /**
     * @return True if the listing is locked by a moderator
     */
    boolean isLocked();

    /**
     * @return True if the listing is stickied by a moderator
     */
    boolean isStickied();

    /**
     * @return True if the listing is distinguished as a moderator
     */
    boolean isMod();
}
