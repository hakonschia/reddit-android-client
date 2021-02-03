package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.model.AccessToken

/**
 * Interface used to notify when a new token is received by the API
 */
fun interface OnNewToken {
    /**
     * Called when a new token is received
     *
     * @param newToken The new token
     */
    fun newToken(newToken: AccessToken)
}