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
import java.util.HashMap;
import java.util.Map;

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
     * A Reddit "Thing"
     */
    public enum Thing {
        Post, Comment
    }

    /**
     * What type of vote to cast on something
     */
    public enum VoteType {
        Upvote(1),
        Downvote(-1),
        Unvote(0);

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


    /**
     * The service object used to communicate with the Reddit API
     */
    private RedditApiService apiService;

    /**
     * The service object used to communicate only with the part of the Reddit API
     * that deals with OAuth access tokens
     */
    private RedditOAuthService OAuthSerivce;

    /**
     * The access token to use for API calls
     */
    private AccessToken accessToken;

    /**
     * Mapping of the "things" of Reddit to their API string identifier
     */
    private final Map<Thing, String> thingMap = new HashMap<Thing, String>() {{
        put(Thing.Post, "t3_");
        put(Thing.Comment, "t1_");
    }};


    public RedditApi(AccessToken accessToken) {
        this.accessToken = accessToken;

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
        this.OAuthSerivce = oauthRetrofit.create(RedditOAuthService.class);
    }

    /**
     * Sets the OAuth access token to be used for API calls
     *
     * @param accessToken The token to use for API calls
     */
    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }


    /**
     * Gets a call object to retrieve an access token from Reddit
     *
     * @param code The authorization code retrieved from the initial login process
     * @return A Call object ready to be called to retrieve an access token
     */
    public Call<AccessToken> getAccessToken(String code) {
        return this.OAuthSerivce.getAccessToken(
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
        return this.OAuthSerivce.refreshToken(
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
        return this.OAuthSerivce.revokeToken(
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
        return this.apiService.getUserInfo(this.generateTokenString(this.accessToken));
    }


    /**
     * Retrieves posts from a given subreddit
     *
     * @param subreddit The subreddit to retrieve posts from. For front page use an empty string
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved
     * @return A Call object ready to retrieve subreddit posts
     */
    public Call<RedditPostResponse> getSubredditPosts(String subreddit, String after, int count) {
        if (subreddit.isEmpty()) { // Load front page posts
            return this.getFrontPagePosts(after, count);
        }

        String url = NetworkConstants.REDDIT_URL + "r/" + subreddit + ".json";

        // Access token is not required for subreddits as posts aren't personalized (TODO i think at least)
        return this.apiService.getPosts(url, after, count, "");
    }

    /**
     * Retrieves posts from the front page (reddit.com)
     *
     * @param after The ID of the last post retrieved (where to now get new posts from)
     * @param count The total amount of already retrieved posts
     *
     * @return A Call object ready to be called to retrieve posts from reddit's front page
     */
    private Call<RedditPostResponse> getFrontPagePosts(String after, int count) {
        if (accessToken == null) {
            // Retrieve default posts
            return this.apiService.getPosts(
                    NetworkConstants.REDDIT_URL + ".json",
                    after,
                    count,
                    ""
            );
        } else {
            // Send with OAuth access token to get custom front page posts
            return this.apiService.getPosts(
                    NetworkConstants.REDDIT_OUATH_URL + ".json",
                    after,
                    count,
                    this.generateTokenString(this.accessToken)
            );
        }
    }

    /**
     * Cast a vote on a thing (post or comment)
     *
     * @param thingId The ID of the thing
     * @param type The type of vote to cast
     * @param thing What kind of thing the vote is for (post or comment)
     * @return A Call object ready to cast a vote
     */
    public Call<Void> vote(String thingId, VoteType type, Thing thing) {
        return this.apiService.vote(
                this.thingMap.get(thing) + thingId,
                type.value,
                this.generateTokenString(this.accessToken)
        );
    }


    /* -------------------- Util methods -------------------- */
    /**
     * Generates the full string (tokenType + tokenValue) for an access token that can be used
     * in an authorization header
     *
     * @param accessToken The token to generate the string for
     * @return token + " " + value
     */
    private String generateTokenString(AccessToken accessToken) {
        return accessToken.getTokenType() + " " + accessToken.getAccessToken();
    }
}
