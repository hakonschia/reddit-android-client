package com.example.hakonsreader.api.enums;

/**
 * What type of vote to cast on something
 */
public enum VoteType {
    UPVOTE(1),
    DOWNVOTE(-1),
    NO_VOTE(0);

    private int value;

    private VoteType(int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }
}