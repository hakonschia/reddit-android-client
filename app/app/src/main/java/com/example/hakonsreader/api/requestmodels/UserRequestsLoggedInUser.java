package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.service.ImgurService;
import com.example.hakonsreader.api.service.UserService;
import com.example.hakonsreader.api.utils.Util;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRequestsLoggedInUser {

    private final AccessToken accessToken;
    private final UserService api;

    /**
     * @param accessToken The access token to use for requests
     * @param api The API service to use for requests
     */
    public UserRequestsLoggedInUser(AccessToken accessToken, UserService api) {
        this.accessToken = accessToken;
        this.api = api;
    }

    /**
     * Retrieves information about logged in users
     *
     * <p>OAuth scope required: {@code identity}</p>
     *
     * @param onResponse The callback for successful requests. Holds the {@link RedditUser} object representing the user
     * @param onFailure The callback for failed requests
     */
    public void info(OnResponse<RedditUser> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Can't get user information without access token for a logged in user", e));
            return;
        }

        api.getUserInfo().enqueue(new Callback<RedditUser>() {
            @Override
            public void onResponse(Call<RedditUser> call, Response<RedditUser> response) {
                RedditUser body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse(body);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<RedditUser> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }
}
