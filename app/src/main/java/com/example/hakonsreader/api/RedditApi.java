package com.example.hakonsreader.api;

import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.exceptions.AccessTokenNotSetException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnNewToken;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.User;
import com.example.hakonsreader.api.responses.MoreCommentsResponse;
import com.example.hakonsreader.api.responses.RedditCommentsResponse;
import com.example.hakonsreader.api.responses.RedditPostsResponse;
import com.example.hakonsreader.api.service.RedditApiService;
import com.example.hakonsreader.api.service.RedditOAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.internal.EverythingIsNonNull;

/**
 * Wrapper for the Reddit API
 */
public class RedditApi {
    private static final String TAG = "RedditApi";

    /**
     * Flag set to give back raw json (> instead of &gt; etc)
     */
    private static final int RAW_JSON = 1;


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



    private RedditApi(String userAgent) {
        this.userAgent = userAgent;

        this.logger = new HttpLoggingInterceptor();

        OkHttpClient client = new OkHttpClient.Builder()
                // Automatically refresh access token on authentication errors (401)
                .authenticator(new Authenticator())
                // Add User-Agent header to every request
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("User-Agent", this.userAgent)
                            .build();

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
     * @param userAgent The user agent for the application.
     * <p>See <a href="https://github.com/reddit-archive/reddit/wiki/API">Reddit documentation</a>
     * on creating your user agent</p>* @return The RedditApi instance
     */
    public static RedditApi getInstance(String userAgent) {
        if (instance == null) {
            instance = new RedditApi(userAgent);
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
     * @param onResponse The callback for successful requests. Holds the new access token
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getAccessToken(String code, OnResponse<AccessToken> onResponse, OnFailure onFailure) {
        if (this.callbackURL == null) {
            onFailure.onFailure(-1, new Throwable("Callback URL is not set. Use RedditApi.setCallbackUrl()"));
            return;
        }
        
        if (this.clientID == null) {
            onFailure.onFailure(-1, new Throwable("Client ID is not set. Use RedditApi.setClientID()"));
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
                if (response.isSuccessful()) {
                    AccessToken token = response.body();
                    if (token != null) {
                        onResponse.onResponse(token);
                    } else {
                        onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                    }
                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Asynchronously refreshes the access token from Reddit
     *
     * @param onResponse The callback for successful requests. Holds the new access token
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    private void refreshToken(OnResponse<AccessToken> onResponse, OnFailure onFailure) {
        this.OAuthService.refreshToken(
                this.basicAuthHeader,
                this.accessToken.getRefreshToken(),
                OAuthConstants.GRANT_TYPE_REFRESH
        ).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, retrofit2.Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    AccessToken body = response.body();
                    if (body != null) {
                        onResponse.onResponse(body);
                    }

                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                onFailure.onFailure(-1, t);
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
     * @param onResponse The callback for successful requests. Doesn't return anything, but is called when successful
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void revokeRefreshToken(OnResponse<Void> onResponse, OnFailure onFailure) {
        this.OAuthService.revokeToken(
                this.basicAuthHeader,
                this.accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(-1, t);
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
     * @param onResponse The callback for successful requests. Holds the {@link User} object representing the logged in user
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getUserInfo(OnResponse<User> onResponse, OnFailure onFailure) {
        try {
            this.ensureTokenIsSet();

            this.apiService.getUserInfo(this.accessToken.generateHeaderString())
                    .enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                            if (response.isSuccessful()) {
                                User body = response.body();
                                if (body != null) {
                                    onResponse.onResponse(body);
                                } else {
                                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                                }
                            } else {
                                onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                            }
                        }
                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            onFailure.onFailure(-1, t);
                        }
                    });
        } catch (AccessTokenNotSetException e) {
            onFailure.onFailure(-1, new AccessTokenNotSetException("Can't get user information without access token", e));
        }
    }


    /**
     * Asynchronously retrieves posts from a given subreddit
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param subreddit The subreddit to retrieve posts from. For front page use an empty string
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved
     *
     * @param onResponse The callback for successful requests. Holds the list of posts
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getSubredditPosts(String subreddit, String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        Pair<String, String> urlAndToken = this.getCorrectApiUrl(false);
        String url = urlAndToken.first;
        String tokenString = urlAndToken.second;

        // Load posts for a subreddit
        if (!subreddit.isEmpty()) {
            url += "r/" + subreddit;
        }

        // .json isn't strictly needed for requests to oauth.reddit.com, but it is for reddit.com
        // so add it anyways
        url += ".json";

        this.apiService.getPosts(url, after, count, RAW_JSON, tokenString).enqueue(new Callback<RedditPostsResponse>() {
            @Override
            public void onResponse(Call<RedditPostsResponse> call, retrofit2.Response<RedditPostsResponse> response) {
                if (response.isSuccessful()) {
                    RedditPostsResponse body = response.body();

                    if (body != null) {
                        List<RedditPost> posts = body.getPosts();

                        onResponse.onResponse(posts);
                    } else {
                        onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                    }
                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<RedditPostsResponse> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }


    /**
     * Asynchronously retrieves posts from a given subreddit
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param postID The post ID to retrieve comments from
     *
     * @param onResponse The callback for successful requests. Holds the list of posts
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getComments(String postID, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        Pair<String, String> urlAndToken = this.getCorrectApiUrl(false);
        String url = urlAndToken.first;
        String tokenString = urlAndToken.second;

        // .json isn't strictly needed for requests to oauth.reddit.com, but it is for reddit.com
        // so add it anyways
        url += "comments/" + postID + ".json";

        this.apiService.getComments(url, "all", RAW_JSON, tokenString).enqueue(new Callback<List<RedditCommentsResponse>>() {
            @Override
            public void onResponse(Call<List<RedditCommentsResponse>> call, retrofit2.Response<List<RedditCommentsResponse>> response) {
                if (response.isSuccessful()) {
                    List<RedditCommentsResponse> body = response.body();

                    if (body != null) {
                        // For comments the first listing object is the post itself and the second its comments
                        List<RedditComment> topLevelComments = body.get(1).getComments();

                        List<RedditComment> allComments = new ArrayList<>();
                        topLevelComments.forEach(comment -> {
                            // Add the comment itself and all its replies
                            allComments.add(comment);
                            allComments.addAll(comment.getReplies());
                        });

                        onResponse.onResponse(allComments);
                    } else {
                        onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                    }
                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<List<RedditCommentsResponse>> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Retrieves comments initially hidden (from "2 more comments" comments)
     *
     * @param postId The ID of the post the comments are in
     * @param children The list of IDs of comments to get
     * @param onResponse
     * @param onFailure
     */
    public void getMoreComments(String postId, List<String> children, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        Pair<String, String> urlAndToken = this.getCorrectApiUrl(true);
        String url = urlAndToken.first;
        String tokenString = urlAndToken.second;

        String postFullname = Thing.POST.getValue() + "_" + postId;

        StringBuilder childrenBuilder = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            childrenBuilder.append(children.get(i));

            if (i != children.size() - 1) {
                childrenBuilder.append(",");
            }
        }

        this.apiService.getMoreComments("json", childrenBuilder.toString(), postFullname, RAW_JSON, tokenString)
                .enqueue(new Callback<MoreCommentsResponse>() {
                    @Override
                    public void onResponse(Call<MoreCommentsResponse> call, retrofit2.Response<MoreCommentsResponse> response) {
                        if (response.isSuccessful()) {
                            MoreCommentsResponse body = response.body();
                            if (body != null) {
                                onResponse.onResponse(body.getComments());
                            } else {
                                onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                            }
                        } else {
                            onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<MoreCommentsResponse> call, Throwable t) {
                        onFailure.onFailure(-1, t);
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
     * @param onResponse The callback for successful requests. Doesn't return anything but is called if successful
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void vote(String thingId, VoteType type, Thing thing, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            this.ensureTokenIsSet();

            this.apiService.vote(
                    // "t1_gre3" etc. to identify what is being voted on (post or comment)
                    thing.getValue() + "_" + thingId,
                    type.getValue(),
                    this.accessToken.generateHeaderString()
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                    if (response.isSuccessful()) {
                        onResponse.onResponse(null);
                    } else {
                        onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    onFailure.onFailure(-1, t);
                }
            });
        } catch (AccessTokenNotSetException e) {
            onFailure.onFailure(-1, new AccessTokenNotSetException("Can't cast vote without access token", e));
        }
    }


    /**
     * Retrieves the correct API url to retrieve, based on if there is a logged in user
     *
     * @param api If true returns the API url instead of the standard reddit.com URL
     * @return A pair of strings where the first string is either www.reddit... or oauth.reddit...
     * and the second string is the access token header (if a user is logged in)
     */
    private Pair<String, String> getCorrectApiUrl(boolean api) {
        // User is logged in, generate token string and set url to oauth.reddit.com to retrieve
        // customized post information (such as vote status)
        try {
            this.ensureTokenIsSet();

            return new Pair<> ((api ? REDDIT_OUATH_API_URL : REDDIT_OUATH_URL), this.accessToken.generateHeaderString());
        } catch (AccessTokenNotSetException ignored) {
            return new Pair<> ((api ? REDDIT_API_URL : REDDIT_URL), "");
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
