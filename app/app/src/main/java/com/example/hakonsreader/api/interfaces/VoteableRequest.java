package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.enums.VoteType;

/**
 * Interface for request classes that offers requests for voting
 *
 * <p>This interface is intended to be used with methods from {@link com.example.hakonsreader.api.RedditApi}
 * to use the same code for voting on different types of listings (comments or posts). </p>
 */
public interface VoteableRequest {
    void vote(VoteType voteType, OnResponse<Void> onResponse, OnFailure onFailure);
}
