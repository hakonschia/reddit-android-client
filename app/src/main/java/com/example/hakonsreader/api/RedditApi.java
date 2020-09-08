package com.example.hakonsreader.api;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.hakonsreader.api.interfaces.OnNewToken;
import com.example.hakonsreader.api.exceptions.AccessTokenNotSetException;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.service.RedditApiService;
import com.example.hakonsreader.api.service.RedditOAuthService;
import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;

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
         * @return The string identifier for the thing (eg. "t1_")
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

    public static final String REDDIT_URL = "https://reddit.com/";

    /**
     *
     * The standard Reddit API URL.
     * <p>Do not use this for calls that should be authenticated with OAuth, see {@link RedditApi#REDDIT_OUATH_API_URL}</p>
     */
    public static final String REDDIT_API_URL = "https://www.reddit.com/api/";

    /**
     * The OAuth subdomain URL for Reddit.
     * <p>This is used to retrieve posts from reddit</p>
     */
    public static final String REDDIT_OUATH_URL = "https://oauth.reddit.com/";

    /**
     * The Reddit API URL used when authenticated with OAuth.
     * <p>This is used when making calls on behalf of a user with their access token</p>
     */
    public static final String REDDIT_OUATH_API_URL = REDDIT_OUATH_URL + "api/";



    /**
     * The instance of the API
     */
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

    /**
     * The listener for when the authenticator retrieves a new token automatically
     */
    private OnNewToken onNewToken;

    /**
     * The logger for debug information
     */
    private HttpLoggingInterceptor logger;


    /* ----------------- Client specific variables ----------------- */
    /**
     * The callback URL used for OAuth
     */
    private String callbackURL;

    /**
     * The client ID for the application
     */
    private String clientID;
    /**
     * The basic authentication header with client ID. Includes "Basic " prefix
     */
    private String basicAuthHeader;

    /**
     * The user agent of the client
     */
    private String userAgent;



    private RedditApi() {
        this.logger = new HttpLoggingInterceptor();

        OkHttpClient client = new OkHttpClient.Builder()
                // Automatically refresh access token on authentication errors (401)
                .authenticator(new Authenticator())
                // Add User-Agent header to every request
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original;

                    // Set the user agent if available
                    if (this.userAgent != null) {
                        request = original.newBuilder()
                                .header("User-Agent", (this.userAgent == null ? "" : this.userAgent))
                                .build();
                    }

                    return chain.proceed(request);
                })
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(this.logger)
                .build();

        // Create the API service object used to make API calls
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(REDDIT_OUATH_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        this.apiService = retrofit.create(RedditApiService.class);

        // Create the service object for OAuth related calls
        Retrofit oauthRetrofit = new Retrofit.Builder()
                .baseUrl(REDDIT_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
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

    /**
     * Sets the listener for what to do when a new token is received by the API
     *
     * @param onNewToken The token listener
     */
    public void setOnNewToken(OnNewToken onNewToken) {
        this.onNewToken = onNewToken;
    }

    /**
     * @param loggingLevel The level at what to log
     */
    public void setLoggingLevel(HttpLoggingInterceptor.Level loggingLevel) {
        this.logger.setLevel(loggingLevel);
    }


    /**
     * Sets the access token to use for authorized API calls
     *
     * @param token The token to use
     */
    public void setToken(AccessToken token) {
        this.accessToken = token;
    }

    /**
     * Sets the value to use in the User-Agent header sent to Reddit
     * <p>See <a href="https://github.com/reddit-archive/reddit/wiki/API">Reddit documentation</a>
     * on creating your user agent</p>
     *
     * @param userAgent The user agent string for your application
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Sets the callback URL used for OAuth. This is used when retrieving access tokens
     * <p>This must match the callback URL set in <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>
     *
     * @param callbackURL The URL to use for OAuth access tokens
     */
    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    /**
     * Sets the client ID of the application
     * <p>To find your client ID see <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>

     * @param clientID The client ID
     */
    public void setClientID(String clientID) {
        this.clientID = clientID;

        // Create the header value now as it is unnecessary to re-create it for every call
        // The username:password is the client ID + client secret (for installed apps there is no secret)
        this.basicAuthHeader = "Basic " + Base64.encodeToString((clientID + ":").getBytes(), Base64.NO_WRAP);
    }

    /* --------------- Access token calls --------------- */
    /**
     * Asynchronously retrieves an access token from Reddit
     * <p>Note: The callback URL must be set with {@link RedditApi#setCallbackURL(String)}</p>
     * <p>Note: Client ID must be set with {@link RedditApi#setClientID(String)}</p>
     *
     * @param code The authorization code retrieved from the initial login process
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    public void getAccessToken(String code, OnResponse<AccessToken> onResponse, OnFailure<AccessToken> onFailure) {
        if (this.callbackURL == null) {
            onFailure.onFailure(null, new Throwable("Callback URL is not set. Use RedditApi.setCallbackUrl()"));
            return;
        }
        
        if (this.clientID == null) {
            onFailure.onFailure(null, new Throwable("Client ID is not set. Use RedditApi.setClientID()"));
            return;
        }

        this.OAuthService.getAccessToken(
                this.basicAuthHeader,
                code,
                OAuthConstants.GRANT_TYPE_AUTHORIZATION,
                this.callbackURL
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
     * Asynchronously refreshes the access token from Reddit
     *
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    private void refreshToken(OnResponse<AccessToken> onResponse, OnFailure<AccessToken> onFailure) {
        this.OAuthService.refreshToken(
                this.basicAuthHeader,
                this.accessToken.getRefreshToken(),
                OAuthConstants.GRANT_TYPE_REFRESH
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
     * Synchronously refreshes the access token
     *
     * @return The new access token, or null if it couldn't be refreshed
     */
    private AccessToken refreshToken() {
        AccessToken newToken = null;
        try {
            newToken = this.OAuthService.refreshToken(
                    this.basicAuthHeader,
                    this.accessToken.getRefreshToken(),
                    OAuthConstants.GRANT_TYPE_REFRESH
            ).execute().body();

            if (newToken != null) {
                newToken.setRefreshToken(this.accessToken.getRefreshToken());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newToken;
    }

    /**
     * Revokes the refresh token. This will also invalidate the corresponding access token,
     * effectively logging the user out as the client can no longer make calls on behalf of the user
     *
     * @param onResponse The callback for successful requests. Note: The request can still
     *                         fail in this callback (such as 400 error codes), use {@link Response#isSuccessful()}
     * @param onFailure The callback for errors caused by issues such as network connection fails
     */
    public void revokeRefreshToken(OnResponse<Void> onResponse, OnFailure<Void> onFailure) {
        this.OAuthService.revokeToken(
                this.basicAuthHeader,
                this.accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
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

        this.accessToken = null;
    }

    /**
     * Ensures that {@link RedditApi#accessToken} is set if one is stored in the application
     *
     * @throws AccessTokenNotSetException If there isn't any access token set
     */
    private void ensureTokenIsSet() throws AccessTokenNotSetException {
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
        String url = REDDIT_URL;

        // User is logged in, generate token string and set url to oauth.reddit.com to retrieve
        // customized post information (such as vote status)
        try {
            this.ensureTokenIsSet();

            tokenString = this.accessToken.generateHeaderString();
            url = REDDIT_OUATH_URL;
        } catch (AccessTokenNotSetException ignored) { }

        // Load posts for a subreddit
        if (!subreddit.isEmpty()) {
            url += "r/" + subreddit;
        }

        // .json isn't strictly needed for requests to oauth.reddit.com, but it is for reddit.com
        // so add it anyways
        url += ".json";

        this.apiService.getPosts(url, after, count, tokenString).enqueue(new Callback<RedditPostResponse>() {
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



    /**
     * Authenticator that automatically retrieves a new access token on 401 responses
     */
    public class Authenticator implements okhttp3.Authenticator {

        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, Response response) throws IOException {
            // If we have a previous access token with a refresh token
            if (accessToken != null && accessToken.getRefreshToken() != null) {
                AccessToken newToken = refreshToken();

                // No new token received
                if (newToken == null) {
                    return response.request();
                }

                // The response does not send a new refresh token, so make sure the old one is saved
                newToken.setRefreshToken(accessToken.getRefreshToken());

                if (onNewToken != null) {
                    onNewToken.newToken(newToken);
                }

                return response.request().newBuilder()
                        .header("Authorization", newToken.generateHeaderString())
                        .build();
            }

            return null;
        }
    }
}
