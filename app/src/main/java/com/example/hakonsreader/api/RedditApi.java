package com.example.hakonsreader.api;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.service.RedditService;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.constants.OAuthConstants;

import java.security.acl.AclEntry;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Wrapper for the Reddit API
 */
public class RedditApi {
    private static RedditApi instance;

    private RedditService accessTokenService;
    private RedditService redditService;

    private RedditApi() {
        // TODO maybe possible to add a function that checks if access token is invalid and refreshes
        // Add headers to every request
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpBuilder.addInterceptor(interceptor);
        okHttpBuilder.addInterceptor(chain -> {
            Request request = chain.request();

            Request.Builder newRequest = request.newBuilder().addHeader("User-Agent", NetworkConstants.USER_AGENT);

            return chain.proceed(newRequest.build());
        });

        // Create the RedditService object used to make API calls
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(NetworkConstants.REDDIT_OUATH_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build());

        Retrofit retrofit = builder.build();
        this.redditService = retrofit.create(RedditService.class);

        // Getting the access token requires a different URL compared to the API calls
        Retrofit.Builder oauthBuilder = new Retrofit.Builder()
                .baseUrl("https://www.reddit.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build());

        Retrofit oauthRetrofit = oauthBuilder.build();
        this.accessTokenService = oauthRetrofit.create(RedditService.class);
    }

    /**
     * @return The singleton instance
     */
    public static RedditApi getInstance() {
        if (instance == null) {
            instance = new RedditApi();
        }
        return instance;
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


    public Call<User> getUserInfo(AccessToken accessToken) {
        return this.redditService.getUserInfo(String.format("%s %s", accessToken.getTokenType(), accessToken.getAccessToken()));
    }
}
