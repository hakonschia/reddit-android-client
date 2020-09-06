package com.example.hakonsreader.interfaces;

import retrofit2.Call;

/**
 * Interface used for failures (like network issues) from API requests
 * @param <T> The model type of the API request
 */
public interface OnFailure<T> {

    /**
     * Called when a request failed due to an issue such as a network failure
     *
     * @param call The call that was made
     * @param t A throwable with error information
     */
    void onFailure(Call<T> call, Throwable t);
}