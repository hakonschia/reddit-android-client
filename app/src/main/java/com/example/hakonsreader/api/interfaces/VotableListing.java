package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.enums.VoteType;

/**
 * Interface for listings that can be voted on
 */
public interface VotableListing extends RedditListing {

    /**
     * @return The score of the listing
     */
    int getScore();

    /**
     * Retrieves the logged in users vote on the post
     *
     * @return {@link VoteType#UPVOTE} if upvoted. {@link VoteType#DOWNVOTE} if downvoted.
     * {@link VoteType#NO_VOTE} if no vote is cast
     */
    VoteType getVoteType();

    /**
     * @param voteType The new vote to set logged in user on the listing
     */
    void setVoteType(VoteType voteType);

    /**
     * @return True if the score is hidden
     */
    boolean isScoreHidden();
}
