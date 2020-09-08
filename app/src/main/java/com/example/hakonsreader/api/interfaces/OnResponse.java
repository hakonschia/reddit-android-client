package com.example.hakonsreader.api.interfaces;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Interface used to handle successful API responses
 * @param <T> The model type returned from the API
 */
public interface OnResponse<T> {

    /**
     * Called when a request has been successful
     * <p>Note that a response can have error codes such as 400 and 500. To check if it was
     * succesful use {@link Response#isSuccessful()}</p>
     *
     * @param call The call that was made for the request
     * @param response The response of the request. For the body returned use {@link Response#body()}
     */
    void onResponse(Call<T> call, Response<T> response);
}
