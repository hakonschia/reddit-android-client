package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.enums.VoteType;

/**
 * Interface for all common attributes for a Reddit listing
 */
public interface RedditListing {

    String getSubreddit();

    String getId();

    String getTitle();

    String getAuthor();

    int getScore();

    String getPermalink();

    long getCreatedAt();

    boolean isLocked();

    Boolean getLiked();


    VoteType getVoteType();

    void setVoteType(VoteType voteType);
}
