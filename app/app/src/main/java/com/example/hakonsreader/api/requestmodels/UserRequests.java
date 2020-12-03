package com.example.hakonsreader.api.requestmodels;

import androidx.annotation.NonNull;

import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.service.ImgurService;
import com.example.hakonsreader.api.service.UserService;
import com.example.hakonsreader.api.utils.Util;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class that provides an interface towards the Reddit API related to users, such as retrieving
 * information about a user and their posts
 */
public class UserRequests {
    private final AccessToken accessToken;
    private final UserService api;
    private final String username;
    private final ImgurRequest imgurRequest;
    private final boolean loadImgurAlbumsAsRedditGalleries;

    /**
     * @param username The username to make requests towards
     * @param api The API service to use for requests
     * @param accessToken The access token to use for requests
     * @param imgurService The service to optionally use for loading Imgur albums directly. Set to
     *                     {@code null} to not load albums.
     */
    public UserRequests(@NonNull UserService api, @NonNull AccessToken accessToken, @NonNull String username, ImgurService imgurService) {
        this.accessToken = accessToken;
        this.api = api;
        this.username = username;
        this.imgurRequest = new ImgurRequest(imgurService);
        this.loadImgurAlbumsAsRedditGalleries = imgurService != null;
    }

    /**
     * Retrieve information about the user
     *
     * <p>Requires a valid access token for the request to be made</p>
     *
     * <p>OAuth scope required: {@code read}</p>
     *
     * @param onResponse The callback for successful requests. Holds the {@link RedditUser} object representing the user
     * @param onFailure The callback for failed requests
     */
    public void info(OnResponse<RedditUser> onResponse, OnFailure onFailure) {
        api.getUserInfoOtherUsers(username).enqueue(new Callback<RedditListing>() {
            @Override
            public void onResponse(Call<RedditListing> call, Response<RedditListing> response) {
                RedditListing body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse((RedditUser) body);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<RedditListing> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }

    /**
     * Block a user
     *
     * @param onResponse The callback for successful requests. This will never hold any information
     *                   but is called when the request is successful
     * @param onFailure The callback for failed requests
     */
    public void block(OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            Util.verifyLoggedInToken(accessToken);
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Blocking a user requires an access token for a logged in user", e));
            return;
        }

        // Technically this API call returns some information, but it's barely any information, and it's
        // not particularly interesting (it's only when the user has created their account, their profile image, and their name/fullname)
        // If the user was blocked already, nothing is returned at all (but 200 OK is returned)

        api.blockUser(username).enqueue(new Callback<Void>() {
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