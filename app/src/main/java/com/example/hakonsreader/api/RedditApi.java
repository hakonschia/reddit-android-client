package com.example.hakonsreader.api;

import androidx.core.util.Pair;

import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.exceptions.AccessTokenNotSetException;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnNewToken;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.interfaces.RedditListing;
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
import java.util.Base64;
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

    /**
     * The API type needed in certain requests (always hardcoded to "json")
     */
    private static final String API_TYPE = "json";


    /**
     * Default Reddit URL
     *
     * <p>Do not use this for calls that should be authenticated with OAuth, see {@link RedditApi#REDDIT_OUATH_URL}</p>
     */
    public static final String REDDIT_URL = "https://www.reddit.com/";

    /**
     * The OAuth subdomain URL for Reddit.
     *
     * <p>This is used to retrieve posts from reddit</p>
     */
    public static final String REDDIT_OUATH_URL = "https://oauth.reddit.com/";



    /**
     * The instance of the API
     */
    private static RedditApi instance;

    /**
     * The service object used to communicate with the Reddit API for non-logged in users
     */
    private RedditApiService api;

    /**
     * The service object used to communicate with the Reddit API that are authorized with
     * OAuth (for logged in users)
     */
    private RedditApiService apiOAuth;

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


        // Common builder for all service objects without a base URL set
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(client);

        // Create the API service for logged in users
        Retrofit loggedIn = retrofitBuilder.baseUrl(REDDIT_OUATH_URL).build();
        this.apiOAuth = loggedIn.create(RedditApiService.class);

        // Create the API service objects used to make API calls for non-logged in users
        Retrofit nonLoggedIn = retrofitBuilder.baseUrl(REDDIT_URL).build();
        this.api = nonLoggedIn.create(RedditApiService.class);
        this.OAuthService = nonLoggedIn.create(RedditOAuthService.class);
    }


    /**
     * Retrieves the singleton instance of the API.
     *
     * <p>Use relevant setters when getting the instance for the first time</p>
     * <p>Setters</p>
     *
     * @param userAgent The user agent for the application. This cannot be changed after the instance is created
     *                  <p>See <a href="https://github.com/reddit-archive/reddit/wiki/API">Reddit documentation</a>
     *                  on creating your user agent</p>
     *
     * @return The RedditApi instance
     */
    public static RedditApi getInstance(String userAgent) {
        if (instance == null) {
            instance = new RedditApi(userAgent);
        }

        return instance;
    }

    /**
     * If an access token is set a new one is automatically retrieved when a request is attempted with
     * an invalid token. This sets the listener for what to do when a new token is received by the API
     *
     * @param onNewToken The token listener. Holds an {@link AccessToken} object
     */
    public void setOnNewToken(OnNewToken onNewToken) {
        this.onNewToken = onNewToken;
    }

    /**
     * Sets the {@link HttpLoggingInterceptor.Level} to use
     *
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
     *
     * @param clientID The client ID
     */
    public void setClientID(String clientID) {
        this.clientID = clientID;

        // Create the header value now as it is unnecessary to re-create it for every call
        // The username:password is the client ID + client secret (for installed apps there is no secret)
        this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((clientID + ":").getBytes());
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
            onFailure.onFailure(-1, new Throwable("Callback URL is not set. Use RedditApi.setCallbackURL()"));
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
        if (this.accessToken == null || this.accessToken.getAccessToken() == null) {
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
        } catch (AccessTokenNotSetException e) {
            onFailure.onFailure(-1, new AccessTokenNotSetException("Can't get user information without access token", e));
            return;
        }


        this.apiOAuth.getUserInfo(this.accessToken.generateHeaderString()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                User body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse(body);
                } else {
                    onFailure.onFailure(response.code(), new Throwable("Error executing request: " + response.code()));
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }


    /**
     * Asynchronously retrieves posts from a given subreddit
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param subreddit The subreddit to retrieve posts from. For front page use an empty string
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved (0 if first time loading)
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditPost} objects
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getPosts(String subreddit, String after, int count, OnResponse<List<RedditPost>> onResponse, OnFailure onFailure) {
        Pair<RedditApiService, String> serviceAndToken = this.getCorrectService();

        // Not front page, add r/ prefix
        if (!subreddit.isEmpty()) {
            subreddit = "r/" + subreddit;
        }

        serviceAndToken.first.getPosts(
                subreddit,
                "hot",
                after,
                count,
                RAW_JSON,
                serviceAndToken.second
        ).enqueue(new Callback<RedditPostsResponse>() {
            @Override
            public void onResponse(Call<RedditPostsResponse> call, retrofit2.Response<RedditPostsResponse> response) {
                RedditPostsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = body.getPosts();
                    onResponse.onResponse(posts);
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


    /* ---------------- Comments ---------------- */
    /**
     * Asynchronously retrieves posts from a given subreddit
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param postID The ID of the post to retrieve comments for
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getComments(String postID, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        Pair<RedditApiService, String> serviceAndToken = this.getCorrectService();

        serviceAndToken.first.getComments(
                postID,
                RAW_JSON,
                serviceAndToken.second
        ).enqueue(new Callback<List<RedditCommentsResponse>>() {
            @Override
            public void onResponse(Call<List<RedditCommentsResponse>> call, retrofit2.Response<List<RedditCommentsResponse>> response) {
                List<RedditCommentsResponse> body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

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
            }
            @Override
            public void onFailure(Call<List<RedditCommentsResponse>> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Retrieves comments initially hidden (from "2 more comments" comments)
     * <p>If an access token is set posts are customized for the user</p>
     *
     * @param postID The ID of the post to retrieve comments for
     * @param children The list of IDs of comments to get (retrieved via {@link RedditComment#getChildren()})
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onFailure The callback for failed requests
     */
    public void getMoreComments(String postID, List<String> children, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        Pair<RedditApiService, String> serviceAndToken = this.getCorrectService();

        String postFullname = Thing.POST.getValue() + "_" + postID;

        // The query parameter for the children is a list of comma separated IDs
        StringBuilder childrenBuilder = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            childrenBuilder.append(children.get(i));

            if (i != children.size() - 1) {
                childrenBuilder.append(",");
            }
        }

        serviceAndToken.first.getMoreComments(
                childrenBuilder.toString(),
                postFullname,
                API_TYPE,
                RAW_JSON,
                serviceAndToken.second
        ).enqueue(new Callback<MoreCommentsResponse>() {
            @Override
            public void onResponse(Call<MoreCommentsResponse> call, retrofit2.Response<MoreCommentsResponse> response) {
                MoreCommentsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse(body.getComments());
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
     * Submit a new comment. This can either be a comment on a post (top-level comment), a reply,
     * or a private message.
     *
     * <p>Comments to posts and replies requires OAuth scope "submit", and private message requires "privatemessage"</p>
     *
     * @param comment The comment to submit
     * @param thing The thing being replied to
     * @param onResponse Callback for successful responses. Holds the newly created comment
     * @param onFailure Callback for failed requests
     */
    public void postComment(String comment, RedditListing thing, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        try {
            this.ensureTokenIsSet();
        } catch (AccessTokenNotSetException e) {
            onFailure.onFailure(-1, e);
            return;
        }

        String fullname = thing.getKind() + "_" + thing.getId();

        // The depth of the new comment
        int depth = 0;
        if (thing instanceof RedditComment) {
            // Set depth to the comment being replied to
            depth = ((RedditComment)thing).getDepth() + 1;
        }

        int finalDepth = depth;
        this.apiOAuth.postComment(
                comment,
                fullname, API_TYPE,
                false,
                this.accessToken.generateHeaderString()
        ).enqueue(new Callback<MoreCommentsResponse>() {
            @Override
            public void onResponse(Call<MoreCommentsResponse> call, retrofit2.Response<MoreCommentsResponse> response) {
                MoreCommentsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    RedditComment newComment = body.getComments().get(0);
                    newComment.setDepth(finalDepth);
                    onResponse.onResponse(newComment);
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
    /* ---------------- End comments ---------------- */


    /**
     * Cast a vote on a listing
     *
     * <p>Requires an access token to be set</p>
     *
     * @param thing The thing to cast a vote on
     * @param type The type of vote to cast
     * @param onResponse The callback for successful requests. The value returned will always be null
     *                   as this request does not return any data
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void vote(RedditListing thing, VoteType type, OnResponse<Void> onResponse, OnFailure onFailure) {
        try {
            this.ensureTokenIsSet();
        } catch (AccessTokenNotSetException e) {
            onFailure.onFailure(-1, new AccessTokenNotSetException("Can't cast vote without access token", e));
            return;
        }


        // "t1_gre3" etc. to identify what is being voted on (post or comment)
        String fullname = thing.getKind() + "_" + thing.getId();

        this.api.vote(
                fullname,
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
    }


    /**
     * Retrieves the correct API service object to use, based on if there is a logged in user
     *
     * @return A pair where the first object is a {@link RedditApiService} and the second
     * is the access token header string (an empty string if no user is logged in)
     */
    private Pair<RedditApiService, String> getCorrectService() {
        // User is logged in, generate token string and set url to oauth.reddit.com to retrieve
        // customized post information (such as vote status)
        try {
            this.ensureTokenIsSet();

            return new Pair<> (this.apiOAuth, this.accessToken.generateHeaderString());
        } catch (AccessTokenNotSetException ignored) {
            return new Pair<> (this.api, "");
        }
    }



    /**
     * Authenticator that automatically retrieves a new access token on 401 responses
     */
    public class Authenticator implements okhttp3.Authenticator {

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            // If we have a previous access token with a refresh token
            if (accessToken != null && accessToken.getRefreshToken() != null) {
                AccessToken newToken = refreshToken();

                // No new token received
                if (newToken == null) {
                    return response.request();
                }

                // The response does not send a new refresh token, so make sure the old one is saved
                newToken.setRefreshToken(accessToken.getRefreshToken());

                // Call token listener if registered
                if (onNewToken != null) {
                    onNewToken.newToken(newToken);
                }

                setToken(newToken);

                return response.request().newBuilder()
                        .header("Authorization", newToken.generateHeaderString())
                        .build();
            }

            return null;
        }
    }
}
