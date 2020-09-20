package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.enums.VoteType;

/**
 * Interface for all common attributes for a Reddit listing
 */
public interface RedditListing {

    String getKind();

    String getSubreddit();

    String getId();

    String getAuthor();

    int getScore();

    String getPermalink();

    long getCreatedAt();

    Boolean getLiked();

    boolean isLocked();

    boolean isScoreHidden();

    boolean isStickied();

    boolean isMod();

    VoteType getVoteType();

    void setVoteType(VoteType voteType);
}
