package com.example.hakonsreader.api.interfaces;

/**
 * Interface used to handle successful API responses
 * @param <T> The model type returned from the API
 */
public interface OnResponse<T> {

    /**
     * Called when a request has been successful
     *
     * @param response The response of the request. For the body returned use
     */
    void onResponse(T response);
}
