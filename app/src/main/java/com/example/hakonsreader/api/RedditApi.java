package com.example.hakonsreader.api;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.hakonsreader.AccessTokenNotSetException;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.service.RedditApiService;
import com.example.hakonsreader.api.service.RedditOAuthService;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;
import com.example.hakonsreader.interfaces.OnFailure;
import com.example.hakonsreader.interfaces.OnResponse;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Wrapper for the Reddit API
 */
public class RedditApi {
    private static final String TAG = "RedditApi";

    /**
     * A Reddit "Thing"
     */
    public enum Thing {
        Comment("t1_"),
        Post("t3_");


        private String value;

        Thing(String value) {
            this.value = value;
        }

        /**
         * Retrieve the underlying string value of the thing
         * <p>This value can be used in addition to the things ID to create the fullname of the thing</p>
         *
         * @return The string identifier for the thing
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * What type of vote to cast on something
     */
    public enum VoteType {
        Upvote(1),
        Downvote(-1),
        NoVote(0);

        private int value;

        private VoteType(int value) {
            this.value = value;
        }
    }


    /**
     * Authenticator that automatically retrieves a new access token on 401 responses
     */
    public class Authenticator implements okhttp3.Authenticator {
        private static final String TAG = "Authenticator";
        
        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, Response response) throws IOException {
            Log.d(TAG, "authenticate: Retrieving new access token");
            Log.d(TAG, "authenticate: " + accessToken);

            if (accessToken == null) {
                accessToken = AccessToken.getStoredToken();
            }

            // If we have a previous access token with a refresh token
            if (accessToken != null && accessToken.getRefreshToken() != null) {
                AccessToken newToken = refreshToken()
                        .execute().body();

                // No new token received
                if (newToken == null) {
                    return response.request();
                }

                AccessToken.storeToken(newToken);
                accessToken = newToken;

                return response.request().newBuilder()
                        .header("Authorization", newToken.generateHeaderString())
                        .build();
            }

            return null;
        }
    }

    private static RedditApi instance;

    /**
     * The service object used to communicate with the Reddit API
     */
    private RedditApiService apiService;

    /**
     * The service object used to communicate only with the part of the Reddit API
     * that deals with OAuth access tokens
     */
    private RedditOAuthService OAuthService;

    /**
     * The access token to use for authorized API calls
     */
    private AccessToken accessToken;


    private RedditApi() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        // Add headers to every request
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .authenticator(new Authenticator()) // Automatically refresh access token on authentication errors (401)
                .addInterceptor(logger)
                .addInterceptor(chain -> {
                    Request request = chain.request()
                            .newBuilder()
                            .addHeader("User-Agent", NetworkConstants.USER_AGENT)
                            .build();

                    return chain.proceed(request);
                });

        // Create the RedditService object used to make API calls
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(NetworkConstants.REDDIT_OUATH_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build());

        Retrofit retrofit = builder.build();
        this.apiService = retrofit.create(RedditApiService.class);

        // Access tokens are retrieved by a different URL than API calls
        Retrofit.Builder oauthBuilder = new Retrofit.Builder()
                .baseUrl(NetworkConstants.REDDIT_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build());

        Retrofit oauthRetrofit = oauthBuilder.build();
        this.OAuthService = oauthRetrofit.create(RedditOAuthService.class);
    }

    /**
     * @return The RedditApi instance
     */
    public static RedditApi getInstance() {
        if (instance == null) {
            instance = new RedditApi();
        }

        return instance;
    }



    /* --------------- Access token calls --------------- */
    /**
     * Asynchronously retrieves an access token from Reddit
     *
     * @param code The authorization code retrieved from the initial login process
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    public void getAccessToken(String code, OnResponse<AccessToken> onResponse, OnFailure<AccessToken> onFailure) {
        this.OAuthService.getAccessToken(
                code,
                OAuthConstants.GRANT_TYPE_AUTHORIZATION,
                OAuthConstants.CALLBACK_URL
        ).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, retrofit2.Response<AccessToken> response) {
                onResponse.onResponse(call, response);
            }
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                onFailure.onFailure(call, t);
            }
        });
    }

    /**
     * Gets a call object to refresh the access token from Reddit
     *
     * @return A Call object ready to be called to refresh the access token
     */
    private Call<AccessToken> refreshToken() {
        return this.OAuthService.refreshToken(
                accessToken.getRefreshToken(),
                OAuthConstants.GRANT_TYPE_REFRESH
        );
    }

    /**
     * Revokes the refresh token. This will also invalidate the corresponding access token,
     * effectively logging the user out as the client can no longer make calls on behalf of the user
     *
     * @return A void Call. The response code says if the operation was successful or not.
     */
    public Call<Void> revokeRefreshToken() {
        return this.OAuthService.revokeToken(
                accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
        );
    }

    /**
     * Ensures that {@link RedditApi#accessToken} is set if one is stored in the application
     *
     * @throws AccessTokenNotSetException If there isn't any access token set
     */
    private void ensureTokenIsSet() throws AccessTokenNotSetException {
        this.accessToken = AccessToken.getStoredToken();

        if (this.accessToken == null) {
            throw new AccessTokenNotSetException("Access token was not found");
        }
    }
    /* --------------- End access token calls --------------- */



    /**
     * Asynchronously retrieves information about the user logged in
     * <p>Requires a valid access token for the request to be made</p>
     *
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    public void getUserInfo(OnResponse<User> onResponse, OnFailure<User> onFailure) {
        try {
            this.ensureTokenIsSet();

            this.apiService.getUserInfo(this.accessToken.generateHeaderString())
                    .enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                            onResponse.onResponse(call, response);
                        }
                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            onFailure.onFailure(call, t);
                        }
                    });
        } catch (AccessTokenNotSetException e) {
            Log.d(TAG, "getUserInfo: can't get user information without access token");
        }
    }


    /**
     * Asynchronously retrieves posts from a given subreddit
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param subreddit The subreddit to retrieve posts from. For front page use an empty string
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    public void getSubredditPosts(String subreddit, String after, int count, OnResponse<RedditPostResponse> onResponse, OnFailure<RedditPostResponse> onFailure) {
        String tokenString = "";
        String url = NetworkConstants.REDDIT_URL;

        // User is logged in, generate token string and set url to oauth.reddit.com to retrieve
        // customized post information (such as vote status)
        try {
            this.ensureTokenIsSet();

            tokenString = this.accessToken.generateHeaderString();
            url = NetworkConstants.REDDIT_OUATH_URL;
        } catch (AccessTokenNotSetException ignored) { }

        // Load posts for a subreddit
        if (!subreddit.isEmpty()) {
            url += "r/" + subreddit;
        }

        // .json isn't strictly needed for requests to oauth.reddit.com, but it is for reddit.com
        // so add it anyways
        url += ".json";

        this.apiService.getPosts(url, after, count, "all", tokenString).enqueue(new Callback<RedditPostResponse>() {
            @Override
            public void onResponse(Call<RedditPostResponse> call, retrofit2.Response<RedditPostResponse> response) {
                onResponse.onResponse(call, response);
            }
            @Override
            public void onFailure(Call<RedditPostResponse> call, Throwable t) {
                onFailure.onFailure(call, t);
            }
        });
    }

    /**
     * Send an asynchronous request to cast a vote on a thing (post or comment)
     * <p>Requires a valid access token</p>
     *
     * @param thingId The ID of the thing
     * @param type The type of vote to cast
     * @param thing What kind of thing the vote is for (post or comment)
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails*/
    public void vote(String thingId, VoteType type, Thing thing, OnResponse<Void> onResponse, OnFailure<Void> onFailure) {
        try {
            this.ensureTokenIsSet();

            this.apiService.vote(
                    // "t1_gre3" etc. to identify what is being voted on (post or comment)
                    thing.value + thingId,
                    type.value,
                    this.accessToken.generateHeaderString()
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                    onResponse.onResponse(call, response);
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    onFailure.onFailure(call, t);
                }
            });

        } catch (AccessTokenNotSetException e) {
            Log.d(TAG, "vote: Can't cast vote without access token");
        }
    }

}
