package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.service.VoteService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

/**
 * Request class to cast a vote. This is a convenience class that other request classes that allow
 * for voting can use, and is not exposed outside the API package
 */
class VoteableRequestModel {

    private final AccessToken accessToken;
    private final VoteService api;

    public VoteableRequestModel(AccessToken accessToken, VoteService api) {
        this.accessToken = accessToken;
        this.api = api;
    }

    /**
     * Cast a vote on a listing
     *
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * <p>OAuth scope required: {@code vote}</p>
     *
     * @param type The type of vote to cast
     * @param onResponse The callback for successful requests. The value returned will always be null
     *                   as this request does not return any data
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void vote(Thing thing, String thingId, VoteType type, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Voting requires a valid access token for a logged in user", e));
            return;
        }

        api.vote(
                Util.createFullName(thing, thingId),
                type.getValue()
        ).enqueue(new Callback<Void>() {
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
