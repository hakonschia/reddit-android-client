package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.responses.GenericError;
import com.example.hakonsreader.api.responses.ListingResponse;
import com.example.hakonsreader.api.service.UserService;
import com.example.hakonsreader.api.utils.Util;

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

    /**
     * @param api The API service to use for requests
     * @param accessToken The access token to use for requests
     */
    public UserRequests(UserService api, AccessToken accessToken) {
        this.api = api;
        this.accessToken = accessToken;
        this.username = null;
    }

    /**
     * @param username The username to make requests towards
     * @param api The API service to use for requests
     * @param accessToken The access token to use for requests
     */
    public UserRequests(UserService api, AccessToken accessToken, String username) {
        this.username = username;
        this.accessToken = accessToken;
        this.api = api;
    }

    /**
     * Retrieve information about the user
     *
     * <p>Requires a valid access token for the request to be made</p>
     *
     * <p>OAuth scopes required:
     * <ol>
     *     <li>For information about logged in users: {@code identity}</li>
     *     <li>For information about other users: {@code read}</li>
     * </ol>
     * </p>
     *
     * @param onResponse The callback for successful requests. Holds the {@link User} object representing the user
     * @param onFailure The callback for failed requests
     */
    public void info(OnResponse<User> onResponse, OnFailure onFailure) {
        if (username == null) {
            try {
                Util.verifyLoggedInToken(accessToken);
            } catch (InvalidAccessTokenException e) {
                onFailure.onFailure(new GenericError(-1), new InvalidAccessTokenException("Can't get user information without access token for a logged in user", e));
                return;
            }

            api.getUserInfo(accessToken.generateHeaderString()).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    User body = null;
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
                public void onFailure(Call<User> call, Throwable t) {
                    onFailure.onFailure(new GenericError(-1), t);
                }
            });
        } else {
            api.getUserInfoOtherUsers(username, accessToken.generateHeaderString()).enqueue(new Callback<RedditListing>() {
                @Override
                public void onResponse(Call<RedditListing> call, Response<RedditListing> response) {
                    RedditListing body = null;
                    if (response.isSuccessful()) {
                        body = response.body();
                    }

                    if (body != null) {
                        onResponse.onResponse((User) body);
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
    }


    /**
     * Get posts by a user
     *
     * <p>The posts retrieved here are sorted by "new", if you want to retrieve posts with a
     * different sort use {@link UserRequests#posts()}</p>
     *
     * <p>OAuth scope required: {@code history}</p>
     *
     * @param onResponse The handler for successful responses. Holds the list of posts
     * @param onFailure The handler for failed responses
     */
    public void posts(OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        this.getPosts("new", PostTimeSort.ALL_TIME, onResponse, onFailure);
    }

    /**
     * Retrieve an object to make API calls for posts by the user
     *
     * @return An object that can retrieve new, top, and controversial posts for a user
     */
    public UserPostsRequets posts() {
        return new UserPostsRequets();
    }

    /**
     * Class to retrieve posts from a user. The functions declared here define how to sort the posts
     * (new, hot etc.)
     */
    public class UserPostsRequets {
        /**
         * Get the current "hot" posts for the user
         *
         * <p>OAuth scope required: {@code history}</p>
         */
        public void hot(OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("hot", null, onResponse, onFailure);
        }

        /**
         * Get the "top" posts for the user
         *
         * <p>OAuth scope required: history</p>
         *
         * @param sort How to sort the posts
         */
        public void top(PostTimeSort sort, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("top", sort, onResponse, onFailure);
        }

        /**
         * Get the "controversial" posts for the user
         *
         * <p>OAuth scope required: {@code history}</p>
         *
         * @param sort How to sort the posts
         */
        public void controversial(PostTimeSort sort, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("controversial", sort, onResponse, onFailure);
        }
    }


    /**
     * Convenience method that the other functions use internally
     *
     * @param sort The sort for the posts (new, hot, top, or controversial)
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial,
     *                 and can be set to null for new and hot
     * @param onResponse The handler for successful responses
     * @param onFailure The handler for failed responses
     */
    private void getPosts(String sort, PostTimeSort timeSort, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        api.getListingsFromUser(
                username,
                "submitted",
                sort,
                timeSort == null ? "" : timeSort.getValue(),
                accessToken.generateHeaderString()
        ).enqueue(new Callback<ListingResponse>() {
            @Override
            public void onResponse(Call<ListingResponse> call, Response<ListingResponse> response) {
                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = (List<RedditPost>) body.getListings();
                    onResponse.onResponse(posts);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<ListingResponse> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }
}