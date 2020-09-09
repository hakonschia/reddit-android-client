package com.example.hakonsreader.api.interfaces;

/**
 * Interface used for failures (like network issues) from API requests
 */
public interface OnFailure {

    /**
     * Called when a request failed due to an issue such as a network failure
     *
     * @param statusCode The status code of the response
     * @param t A throwable with error information
     */
    void onFailure(int statusCode, Throwable t);
}