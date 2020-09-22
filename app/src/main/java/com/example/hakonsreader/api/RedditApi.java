package com.example.hakonsreader.api;

import android.util.Log;

import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.enums.VoteType;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
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

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.internal.EverythingIsNonNull;


/**
 * Wrapper for the Reddit API for installed applications
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
     * The service object used to communicate with the Reddit API
     */
    private RedditApiService api;

    /**
     * The service object used to communicate only with the part of the Reddit API
     * that deals with OAuth access tokens
     */
    private RedditOAuthService oauthService;


    /**
     * The access token to use for authorized API calls
     *
     * <p>Note: never set this to {@code null}, use {@code new AccessToken()} instead. To update the
     * value use {@link RedditApi#setTokenInternal(AccessToken)} instead of updating it directly</p>
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

    /**
     * The device ID to send to Reddit for non-logged in user access tokens
     */
    private String deviceID;


    private RedditApi(String userAgent, String clientID) {
        this.userAgent = userAgent;

        this.clientID = clientID;
        // Create the header value now as it is unnecessary to re-create it for every call
        // The username:password is the client ID + client secret (for installed apps there is no secret)
        this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((clientID + ":").getBytes());

        this.accessToken = new AccessToken();
        this.logger = new HttpLoggingInterceptor();

        OkHttpClient apiClient = new OkHttpClient.Builder()
                // Automatically refresh access token on authentication errors (401)
                .authenticator(new Authenticator())
                // Add User-Agent header to every request
                .addInterceptor(new UserAgentInterceptor())
                // Ensure that an access token is always set before sending a request
                .addInterceptor(new NoTokenInterceptor())
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(this.logger)
                .build();

        // Create the API service used to make calls towards oauth.reddit.com
        Retrofit oauth = new Retrofit.Builder()
                .baseUrl(REDDIT_OUATH_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(apiClient)
                .build();
        this.api = oauth.create(RedditApiService.class);

        // The OAuth client does not need interceptors/authenticators for tokens as it doesn't
        // use the access tokens for authorization
        OkHttpClient oauthClient = new OkHttpClient.Builder()
                // Add User-Agent header to every request
                .addInterceptor(new UserAgentInterceptor())
                .addInterceptor(this.logger)
                .build();

        // Create the API service used to make API calls towards www.reddit.com
        Retrofit normal = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(REDDIT_URL)
                .client(oauthClient)
                .build();
        this.oauthService = normal.create(RedditOAuthService.class);
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
     * @param clientID The client ID of the application
     *                 <p>To find your client ID see <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>
     *
     * @return The RedditApi instance
     */
    public static RedditApi getInstance(String userAgent, String clientID) {
        if (instance == null) {
            instance = new RedditApi(userAgent, clientID);
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
     * <p>This only has to be set during the initial initialization. When new tokens are retrieved
     * the internal value is set automatically. To retrieve the new token use {@link RedditApi#setOnNewToken(OnNewToken)}</p>
     *
     * @param token The token to use
     */
    public void setToken(AccessToken token) {
        if (token != null) {
            this.accessToken = token;
        } else {
            this.accessToken = new AccessToken();
        }
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
     * Set the device ID to use to receive access tokens for non-logged in users
     *
     * <p>If this is null or empty "DO_NOT_TRACK_THIS_DEVICE" will be used. See
     * <a href="https://github.com/reddit-archive/reddit/wiki/OAuth2#application-only-oauth">
     *     Reddit OAuth documentation</a> for more information</p>
     *
     * @param deviceID The device ID to use
     */
    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }


    /**
     * Convenience method to set the internal token. This calls the registered token listener
     *
     * @param token The token to set, does nothing if null
     */
    private void setTokenInternal(AccessToken token) {
        if (token != null) {
            accessToken = token;

            // Call token listener if registered
            if (onNewToken != null) {
                onNewToken.newToken(token);
            }
        }
    }

    /* --------------- Access token calls --------------- */
    /**
     * Asynchronously retrieves an access token from Reddit.
     *
     * <p>Note: The new access token is given with the registered token listener. Use
     * {@link RedditApi#setOnNewToken(OnNewToken)} to retrieve the new token</p>
     * <p>Note: The callback URL must be set with {@link RedditApi#setCallbackURL(String)}</p>
     *
     * @param code The authorization code retrieved from the initial login process
     * @param onResponse The callback for successful requests. Does not hold anything, but is called with
     *                   {@code null} when successful.
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getAccessToken(String code, OnResponse<Void> onResponse, OnFailure onFailure) {
        if (this.callbackURL == null) {
            onFailure.onFailure(-1, new Throwable("Callback URL is not set. Use RedditApi.setCallbackURL()"));
            return;
        }

        this.oauthService.getAccessToken(
                this.basicAuthHeader,
                code,
                OAuthConstants.GRANT_TYPE_AUTHORIZATION,
                this.callbackURL
        ).enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                AccessToken token = null;
                if (response.isSuccessful()) {
                    token = response.body();
                }

                if (token != null) {
                    setTokenInternal(token);
                    onResponse.onResponse(null);
                }  else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }

    /**
     * Revokes the refresh token. This will also invalidate the corresponding access token,
     * effectively logging the user out as the client can no longer make calls on behalf of the user
     *
     * <p>This does nothing with access tokens for non-logged in users</p>
     *
     * @param onResponse The callback for successful requests. Doesn't return anything, but is called when successful
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void revokeRefreshToken(OnResponse<Void> onResponse, OnFailure onFailure) {
        if (this.accessToken.getRefreshToken() == null) {
            onFailure.onFailure(-1, new Throwable("No token to revoke"));
            return;
        }

        this.oauthService.revokeToken(
                this.basicAuthHeader,
                this.accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });

        this.accessToken = new AccessToken();
    }

    /**
     * Ensures that {@link RedditApi#accessToken} is set and is valid for a user that is logged in.
     * Access tokens for non-logged in users do not count.
     *
     * @throws InvalidAccessTokenException If the access token has no refresh token (ie. not an actually logged in user)
     */
    private void verifyLoggedInToken() throws InvalidAccessTokenException {
        if (this.accessToken.getRefreshToken() == null) {
            throw new InvalidAccessTokenException("Valid access token was not found");
        }
    }
    /* --------------- End access token calls --------------- */



    /**
     * Asynchronously retrieves information about the user logged in
     *
     * <p>Requires OAuth scope "identity"</p>
     * <p>Requires a valid access token for the request to be made</p>
     *
     * @param onResponse The callback for successful requests. Holds the {@link User} object representing the logged in user
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getUserInfo(OnResponse<User> onResponse, OnFailure onFailure) {
        try {
            this.verifyLoggedInToken();
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(-1, new InvalidAccessTokenException("Can't get user information without access token for a logged in user", e));
            return;
        }

        this.api.getUserInfo(this.accessToken.generateHeaderString()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse(body);
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
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
        // Not front page, add r/ prefix
        if (!subreddit.isEmpty()) {
            subreddit = "r/" + subreddit;
        }

        this.api.getPosts(
                subreddit,
                "hot",
                after,
                count,
                RAW_JSON,
                this.accessToken.generateHeaderString()
        ).enqueue(new Callback<RedditPostsResponse>() {
            @Override
            public void onResponse(Call<RedditPostsResponse> call, Response<RedditPostsResponse> response) {
                RedditPostsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    List<RedditPost> posts = body.getPosts();
                    onResponse.onResponse(posts);
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
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
     *
     * <p>If a user access token is set comments are customized for the user (ie. vote status is set).
     * Requires OAuth scope "read"</p>
     *
     * @param postID The ID of the post to retrieve comments for
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onFailure The callback for failed requests
     */
    @EverythingIsNonNull
    public void getComments(String postID, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        this.api.getComments(
                postID,
                RAW_JSON,
                this.accessToken.generateHeaderString()
        ).enqueue(new Callback<List<RedditCommentsResponse>>() {
            @Override
            public void onResponse(Call<List<RedditCommentsResponse>> call, Response<List<RedditCommentsResponse>> response) {
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
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
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
     * <p>If an access token is set comments are customized for the user (ie. vote status is set)</p>
     *
     * @param postID The ID of the post to retrieve comments for
     * @param children The list of IDs of comments to get (retrieved via {@link RedditComment#getChildren()})
     * @param onResponse The callback for successful requests. Holds a {@link List} of {@link RedditComment} objects
     * @param onFailure The callback for failed requests
     */
    public void getMoreComments(String postID, List<String> children, OnResponse<List<RedditComment>> onResponse, OnFailure onFailure) {
        String postFullname = Thing.POST.getValue() + "_" + postID;

        // The query parameter for the children is a list of comma separated IDs
        StringBuilder childrenBuilder = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            childrenBuilder.append(children.get(i));

            if (i != children.size() - 1) {
                childrenBuilder.append(",");
            }
        }

        this.api.getMoreComments(
                childrenBuilder.toString(),
                postFullname,
                API_TYPE,
                RAW_JSON,
                this.accessToken.generateHeaderString()
        ).enqueue(new Callback<MoreCommentsResponse>() {
            @Override
            public void onResponse(Call<MoreCommentsResponse> call, Response<MoreCommentsResponse> response) {
                MoreCommentsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    onResponse.onResponse(body.getComments());
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
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
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
     *
     * @param comment The comment to submit
     * @param thing The thing being replied to
     * @param onResponse Callback for successful responses. Holds the newly created comment
     * @param onFailure Callback for failed requests
     */
    public void postComment(String comment, RedditListing thing, OnResponse<RedditComment> onResponse, OnFailure onFailure) {
        try {
            this.verifyLoggedInToken();
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(-1, new InvalidAccessTokenException("Posting comments requires a valid access token for a logged in user"));
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
        this.api.postComment(
                comment,
                fullname, API_TYPE,
                false,
                this.accessToken.generateHeaderString()
        ).enqueue(new Callback<MoreCommentsResponse>() {
            @Override
            public void onResponse(Call<MoreCommentsResponse> call, Response<MoreCommentsResponse> response) {
                MoreCommentsResponse body = null;
                if (response.isSuccessful()) {
                    body = response.body();
                }

                if (body != null) {
                    RedditComment newComment = body.getComments().get(0);
                    newComment.setDepth(finalDepth);
                    onResponse.onResponse(newComment);
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
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
     * <p>Requires OAuth scope "vote"</p>
     * <p>Requires a user access token to be set. {@code onFailure} will be called if no access token is set</p>
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
            this.verifyLoggedInToken();
        } catch (InvalidAccessTokenException e) {
            onFailure.onFailure(-1, new InvalidAccessTokenException("Voting requires a valid access token for a logged in user", e));
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
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    onFailure.onFailure(response.code(), newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });
    }



    /* ----------------- Misc ----------------- */
    /**
     * Retrieve a new access token valid for non-logged in users
     *
     * @return A new access token only valid for non-logged in users
     */
    private AccessToken newNonLoggedInToken() {
        AccessToken newToken = null;
        try {
            String device = (this.deviceID == null || this.deviceID.isEmpty() ? "DO_NOT_TRACK_THIS_DEVICE" : this.deviceID);

            newToken = this.oauthService.getAccessTokenNoUser(
                    this.basicAuthHeader,
                    OAuthConstants.GRANT_TYPE_INSTALLED_CLIENT,
                    device
            ).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newToken;
    }

    /**
     * Create a new throwable with a generic request error message
     *
     * @param code The code of the request
     * @return A throwable with a generic error message and the code
     */
    private Throwable newThrowable(int code) {
        return new Throwable("Error executing request: " + code);
    }
    /* ----------------- End misc ----------------- */



    /* ----------------- Classes ----------------- */
    /**
     * Authenticator that automatically retrieves a new access token
     */
    private class Authenticator implements okhttp3.Authenticator {

        @Override
        public Request authenticate(Route route, okhttp3.Response response) {
            AccessToken newToken;

            // If we have a previous access token with a refresh token
            if (accessToken.getRefreshToken() != null) {
                newToken = refreshToken();

                if (newToken != null) {
                    // The response does not send a new refresh token, so make sure the old one is saved
                    newToken.setRefreshToken(accessToken.getRefreshToken());
                }
            } else {
                // No refresh token means we have a token for non-logged in users
                newToken = newNonLoggedInToken();
            }

            // No new token received
            if (newToken != null) {
                setTokenInternal(newToken);

                return response.request().newBuilder()
                        .header("Authorization", accessToken.generateHeaderString())
                        .build();
            } else {
                return response.request();
            }
        }

        /**
         * Synchronously refreshes the access token
         *
         * @return The new access token, or null if it couldn't be refreshed
         */
        private AccessToken refreshToken() {
            AccessToken newToken = null;
            try {
                newToken = oauthService.refreshToken(
                        basicAuthHeader,
                        accessToken.getRefreshToken(),
                        OAuthConstants.GRANT_TYPE_REFRESH
                ).execute().body();

                if (newToken != null) {
                    newToken.setRefreshToken(accessToken.getRefreshToken());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return newToken;
        }
    }


    /**
     * Interceptor that adds the User-Agent header to the request
     */
    private class UserAgentInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();

            return chain.proceed(request);
        }
    }

    /**
     * Interceptor that ensures that an access token is set. If no token is found
     * a new token for non-logged in users is retrieved
     */
    private class NoTokenInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder request = original.newBuilder();

            if (accessToken.getAccessToken() == null) {
                AccessToken token = newNonLoggedInToken();

                if (token != null && onNewToken != null) {
                    setTokenInternal(token);
                    request.header("Authorization", accessToken.generateHeaderString());
                }
            }

            return chain.proceed(request.build());
        }
    }
}
