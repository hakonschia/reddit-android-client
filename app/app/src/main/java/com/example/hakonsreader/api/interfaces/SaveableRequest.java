package com.example.hakonsreader.api.interfaces;

/**
 * Interface for requests that offer functionality to save/unsave a post or comment
 */
public interface SaveableRequest {

    /**
     * Save a comment or post
     */
    void save(OnResponse<Void> onResponse, OnFailure onFailure);

    /**
     * Save a comment or post
     */
    void unsave(OnResponse<Void> onResponse, OnFailure onFailure);

}
