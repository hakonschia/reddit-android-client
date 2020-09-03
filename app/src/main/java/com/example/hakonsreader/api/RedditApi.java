package com.example.hakonsreader.api;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.service.RedditOAuthService;
import com.example.hakonsreader.api.service.RedditApiService;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Wrapper for the Reddit API
 */
public class RedditApi {
    private static final String TAG = "RedditApi";
    
    /**
     * Authenticator that automatically retrieves a new access token on 401 responses
     */
    public class Authenticator implements okhttp3.Authenticator {
        private static final String TAG = "Authenticator";
        
        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, Response response) throws IOException {
            Log.d(TAG, "authenticate: Retrieving new access token");
            
            // If we have a previous access token with a refresh token
            if (accessToken != null && accessToken.getRefreshToken() != null) {
                AccessToken newToken = refreshToken()
                        .execute().body();

                MainActivity.saveAccessToken(accessToken);

                return response.request().newBuilder()
                        .header("Authorization", newToken.getTokenType() + " " + newToken.getAccessToken())
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
    private RedditOAuthService redditOauthService;

    private static AccessToken accessToken;


    private RedditApi(AccessToken accessToken) {
        RedditApi.accessToken = accessToken;

        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

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
        this.redditOauthService = oauthRetrofit.create(RedditOAuthService.class);
    }

    /**
     * Retrieves the RedditApi instance and if given sets the OAuth access token
     *
     * @param accessToken The Reddit OAuth access token
     * @return The singleton instance
     */
    public static RedditApi getInstance(@Nullable AccessToken accessToken) {
        if (instance == null) {
            instance = new RedditApi(accessToken);
        }
        return instance;
    }

    /**
     * Sets the OAuth access token to be used for API calls
     *
     * @param accessToken The token to use for API calls
     */
    public static void setAccessToken(AccessToken accessToken) {
        RedditApi.accessToken = accessToken;
    }


    /**
     * Gets a call object to retrieve an access token from Reddit
     *
     * @param code The authorization code retrieved from the initial login process
     * @return A Call object ready to be called to retrieve an access token
     */
    public Call<AccessToken> getAccessToken(String code) {
        return this.redditOauthService.getAccessToken(
                code,
                OAuthConstants.GRANT_TYPE_AUTHORIZATION,
                OAuthConstants.CALLBACK_URL
        );
    }

    /**
     * Gets a call object to refresh the access token from Reddit
     *
     * @return A Call object ready to be called to refresh the access token
     */
    public Call<AccessToken> refreshToken() {
        return this.redditOauthService.refreshToken(
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
        return this.redditOauthService.revokeToken(
                accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
        );
    }



    /**
     * Retrieves information about the user logged in
     *
     * @return A Call object ready to be called to retrieve user information
     */
    public Call<User> getUserInfo() {
        return this.apiService.getUserInfo(String.format("%s %s", accessToken.getTokenType(), accessToken.getAccessToken()));
    }


    /**
     * Retrieves posts from a given subreddit
     *
     * @param subreddit The subreddit to retrieve posts from. For front page use an empty string
     * @return A Call object ready to retrieve subreddit posts
     */
    public Call<RedditPostResponse> getSubredditPosts(String subreddit) {
        if (subreddit.isEmpty()) { // Load front page posts
            Log.d(TAG, "getSubredditPosts: Getting front page posts");
            return this.getFrontPagePosts();
        }

        Log.d(TAG, "getSubredditPosts: Getting posts from " + subreddit);
        return this.apiService.getPosts(NetworkConstants.REDDIT_URL + "r/" + subreddit + ".json", "");
    }

    /**
     * Retrieves posts from the front page (reddit.com)
     *
     * @return A Call object ready to be called to retrieve posts from reddit's front page
     */
    private Call<RedditPostResponse> getFrontPagePosts() {
        if (accessToken == null) {
            // Retrieve default posts
            return this.apiService.getPosts(NetworkConstants.REDDIT_URL + ".json", "");
        } else {
            // Send with OAuth access token to get custom front page posts
            return this.apiService.getPosts(
                    NetworkConstants.REDDIT_OUATH_URL + ".json",
                    accessToken.getTokenType() + " " + accessToken.getAccessToken()
            );
        }
    }
}
