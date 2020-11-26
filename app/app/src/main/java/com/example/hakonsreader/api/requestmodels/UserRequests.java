package com.example.hakonsreader.api.requestmodels;

import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditListingKt;
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
     * @param api The API service to use for requests
     * @param accessToken The access token to use for requests
     * @param imgurService The service to optionally use for loading Imgur albums directly. Set to
     *                     {@code null} to not load albums.
     */
    public UserRequests(UserService api, AccessToken accessToken, ImgurService imgurService) {
        this.api = api;
        this.accessToken = accessToken;
        this.imgurRequest = new ImgurRequest(imgurService);
        this.loadImgurAlbumsAsRedditGalleries = imgurService != null;
        this.username = null;
    }

    /**
     * @param username The username to make requests towards
     * @param api The API service to use for requests
     * @param accessToken The access token to use for requests
     * @param imgurService The service to optionally use for loading Imgur albums directly. Set to
     *                     {@code null} to not load albums.
     */
    public UserRequests(UserService api, AccessToken accessToken, String username, ImgurService imgurService) {
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
     * <p>OAuth scopes required:
     * <ol>
     *     <li>For information about logged in users: {@code identity}</li>
     *     <li>For information about other users: {@code read}</li>
     * </ol>
     * </p>
     *
     * @param onResponse The callback for successful requests. Holds the {@link RedditUser} object representing the user
     * @param onFailure The callback for failed requests
     */
    public void info(OnResponse<RedditUser> onResponse, OnFailure onFailure) {
        if (username == null) {
            this.getInfoForLoggedInUser(onResponse, onFailure);
        } else {
           this.getInfoByUsername(username, onResponse, onFailure);
        }
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

    /**
     * Retrieves information about logged in users
     *
     * @param onResponse The callback for successful requests. Holds the {@link RedditUser} object representing the user
     * @param onFailure The callback for failed requests
     */
    private void getInfoForLoggedInUser(OnResponse<RedditUser> onResponse, OnFailure onFailure) {
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

    /**
     * Retrieves information about a user by username
     *
     * @param username The username to retrieve information for
     * @param onResponse The callback for successful requests. Holds the {@link RedditUser} object representing the user
     * @param onFailure The callback for failed requests
     */
    private void getInfoByUsername(String username, OnResponse<RedditUser> onResponse, OnFailure onFailure) {
        api.getUserInfoOtherUsers(username).enqueue(new Callback<RedditListingKt>() {
            @Override
            public void onResponse(Call<RedditListingKt> call, Response<RedditListingKt> response) {
                RedditListingKt body = null;
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
            public void onFailure(Call<RedditListingKt> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
            }
        });
    }


    /**
     * NOTE: the response for this request is sent on a background thread
     *
     * <p>Get posts by a user</p>
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
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Get the current "hot" posts for the user</p>
         *
         * <p>OAuth scope required: {@code history}</p>
         */
        public void hot(OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("hot", null, onResponse, onFailure);
        }

        /**
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Get the "top" posts for the user</p>
         *
         * <p>OAuth scope required: {@code history}</p>
         *
         * @param sort How to sort the posts
         */
        public void top(PostTimeSort sort, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
            getPosts("top", sort, onResponse, onFailure);
        }

        /**
         * NOTE: the response for this request is sent on a background thread
         *
         * <p>Get the "controversial" posts for the user</p>
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
     * NOTE: the response for this request is sent on a background thread
     *
     * <p>Convenience method that the other functions use internally</p>
     *
     * @param sort The sort for the posts (new, hot, top, or controversial)
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial,
     *                 and can be set to null for new and hot
     * @param onResponse The handler for successful responses
     * @param onFailure The handler for failed responses
     */
    private void getPosts(String sort, PostTimeSort timeSort, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        // Loading Imgur albums requires API calls inside the callback. If we use "enqueue" and operate
        // on the current thread the RedditPost objects will be updated after the response is given with
        // onResponse, which means the UI potentially wont be correct, so we have to run this entire thing on
        // a background thread
        new Thread(() -> {
            try {
                Response<ListingResponse> response = api.getListingsFromUser(
                        username,
                        "submitted",
                        sort,
                        timeSort == null ? "" : timeSort.getValue()
                ).execute();

                ListingResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = (List<RedditPost>) body.getListings();

                    if (loadImgurAlbumsAsRedditGalleries) {
                        imgurRequest.loadAlbums(posts);
                    }

                    onResponse.onResponse(posts);
                } else {
                    Util.handleHttpErrors(response, onFailure);
                }
            } catch (IOException e) {
                onFailure.onFailure(new GenericError(-1), e);
            }
        }).start();
    }
}