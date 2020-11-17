package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.service.SaveService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SaveableRequestModel {

    private final AccessToken accessToken;
    private final SaveService api;

    public SaveableRequestModel(AccessToken accessToken, SaveService api) {
        this.accessToken = accessToken;
        this.api = api;
    }

    /**
     * Save a comment or post
     *
     * <p>Requires OAuth scope: {@code save}</p>
     *
     * @param thing The thing to save (must be {@link Thing#COMMENT} or {@link Thing#POST})
     * @param id The ID of the comment or post
     * @param onResponse Callback for successful responses. This will never hold any information, but
     *                   will be called when the request is successful
     * @param onFailure Callback for failed requests
     */
    public void save(Thing thing, String id, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Saving a comment or post requires a valid access token for a logged in user", e));
            return;
        }

        api.save(Util.createFullName(thing, id)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Unsave a comment or post
     *
     * <p>Requires OAuth scope: {@code save}</p>
     *
     * @param thing The thing to save (must be {@link Thing#COMMENT} or {@link Thing#POST})
     * @param id The ID of the comment or post
     * @param onResponse Callback for successful responses. This will never hold any information, but
     *                   will be called when the request is successful
     * @param onFailure Callback for failed requests
     */
    public void unsave(Thing thing, String id, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Unsaving a comment or post requires a valid access token for a logged in user", e));
            return;
        }

        api.unsave(Util.createFullName(thing, id)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }
}
