package com.example.hakonsreader.api;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.service.RedditService;
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
                AccessToken newToken = refreshToken(accessToken)
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
    private RedditService apiService;

    /**
     * The service object used to communicate only with the part of the Reddit API
     * that deals with OAuth access tokens
     */
    private RedditService accessTokenService;

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
        this.apiService = retrofit.create(RedditService.class);

        // Access tokens are retrieved by a different URL than API calls
        Retrofit.Builder oauthBuilder = new Retrofit.Builder()
                .baseUrl(NetworkConstants.REDDIT_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build());

        Retrofit oauthRetrofit = oauthBuilder.build();
        this.accessTokenService = oauthRetrofit.create(RedditService.class);
    }

    /**
     * If given, sets the OAuth access token
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
        return this.accessTokenService.getAccessToken(code, "authorization_code", OAuthConstants.CALLBACK_URL);
    }

    /**
     * Gets a call object to refresh the access token from Reddit
     *
     * @param accessToken The access token object containing the refresh token
     * @return A Call object ready to be called to refresh the access token
     */
    public Call<AccessToken> refreshToken(AccessToken accessToken) {
        return this.accessTokenService.refreshToken(accessToken.getRefreshToken(), "refresh_token");
    }

    /**
     * Retrieves information about the user logged in
     *
     * @param accessToken The AccessToken object holding the OAuth access token
     * @return A Call object ready to be called to retrieve user information
     */
    public Call<User> getUserInfo(AccessToken accessToken) {
        return this.apiService.getUserInfo(String.format("%s %s", accessToken.getTokenType(), accessToken.getAccessToken()));
    }

    /**
     * Retrieves posts from the front page (reddit.com)
     *
     * @param accessToken If present gets posts for the logged in user
     * @return A Call object ready to be called to retrieve posts from reddit's front page
     */
    public Call<RedditPostResponse> getFrontPagePosts(@Nullable AccessToken accessToken) {
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

    /**
     * Retrieves posts from a given subreddit
     *
     * @param subreddit The subreddit to retrieve posts from (without /r/)
     * @return A Call object ready to retrieve subreddit posts
     */
    public Call<RedditPostResponse> getSubredditPosts(String subreddit) {
        return this.apiService.getPosts(NetworkConstants.REDDIT_URL + "r/" + subreddit + ".json", "");
    }
}
