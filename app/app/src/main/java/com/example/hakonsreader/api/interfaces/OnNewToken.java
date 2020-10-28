package com.example.hakonsreader.api.interfaces;

import com.example.hakonsreader.api.model.AccessToken;

/**
 * Interface used to notify when a new token is received by the API
 */
public interface OnNewToken {

    /**
     * Called when a new token is received
     *
     * @param newToken The new token
     */
    void newToken(AccessToken newToken);
}
