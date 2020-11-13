package com.example.hakonsreader.api.enums;

/**
 * What type of vote a post or comment has
 */
public enum VoteType {
    UPVOTE(1),
    DOWNVOTE(-1),
    NO_VOTE(0);

    private int value;

    VoteType(int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }
}