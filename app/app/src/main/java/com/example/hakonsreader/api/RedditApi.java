package com.example.hakonsreader.api;

import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.interceptors.UserAgentInterceptor;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnNewToken;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.requestmodels.CommentRequest;
import com.example.hakonsreader.api.requestmodels.PostRequest;
import com.example.hakonsreader.api.requestmodels.SubredditRequest;
import com.example.hakonsreader.api.requestmodels.SubredditsRequest;
import com.example.hakonsreader.api.requestmodels.UserRequests;
import com.example.hakonsreader.api.service.CommentService;
import com.example.hakonsreader.api.service.PostService;
import com.example.hakonsreader.api.service.OAuthService;
import com.example.hakonsreader.api.service.SubredditService;
import com.example.hakonsreader.api.service.SubredditsService;
import com.example.hakonsreader.api.service.UserService;
import com.example.hakonsreader.api.utils.Util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
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
    public static final int RAW_JSON = 1;

    /**
     * The API type needed in certain requests (always hardcoded to "json")
     */
    public static final String API_TYPE = "json";


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
     * The list of standard subs
     */
    public static final List<String> STANDARD_SUBS = Arrays.asList("", "Popular", "All");


    /**
     * The service object used to communicate with the Reddit API about user related calls
     */
    private UserService userApi;

    /**
     * The service object used to communicate with the Reddit API about subreddit related calls
     */
    private SubredditService subredditApi;

    /**
     * The service object used to communicate with the Reddit API about multiple subreddits related calls
     */
    private SubredditsService subredditsApi;

    /**
     * The service object used to communicate with the Reddit API about post related calls
     */
    private PostService postApi;

    /**
     * The service object used to communicate with the Reddit API about comment related calls
     */
    private CommentService commentApi;

    /**
     * The service object used to communicate only with the part of the Reddit API
     * that deals with OAuth access tokens
     */
    private OAuthService oauthService;


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
    private String callbackUrl;

    /**
     * The client ID for the application
     */
    private final String clientId;

    /**
     * The basic authentication header with client ID. Includes "Basic " prefix
     */
    private final String basicAuthHeader;

    /**
     * The user agent of the client
     */
    private final String userAgent;

    /**
     * The device ID to send to Reddit for non-logged in user access tokens
     */
    private String deviceId;


    private RedditApi(String userAgent, String clientId) {
        this.userAgent = userAgent;
        this.clientId = clientId;

        // Create the header value now as it is unnecessary to re-create it for every call
        // The username:password is the client ID + client secret (for installed apps there is no secret)
        basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":").getBytes());
    }

    /**
     * Creates the service objects used for API calls
     */
    private void createServices() {
        OkHttpClient apiClient = new OkHttpClient.Builder()
                // Automatically refresh access token on authentication errors (401)
                .authenticator(new Authenticator())
                // Add User-Agent header to every request
                .addInterceptor(new UserAgentInterceptor(userAgent))
                // Ensure that an access token is always set before sending a request
                .addInterceptor(new NoTokenInterceptor())
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(logger)
                .build();

        // Create the API service used to make calls towards oauth.reddit.com
        Retrofit apiRetrofit = new Retrofit.Builder()
                .baseUrl(REDDIT_OUATH_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(apiClient)
                .build();

        userApi = apiRetrofit.create(UserService.class);
        subredditApi = apiRetrofit.create(SubredditService.class);
        subredditsApi = apiRetrofit.create(SubredditsService.class);
        postApi = apiRetrofit.create(PostService.class);
        commentApi = apiRetrofit.create(CommentService.class);

        // The OAuth client does not need interceptors/authenticators for tokens as it doesn't
        // use the access tokens for authorization
        OkHttpClient oauthClient = new OkHttpClient.Builder()
                // Add User-Agent header to every request
                .addInterceptor(new UserAgentInterceptor(userAgent))
                .addInterceptor(logger)
                .build();

        // Create the API service used to make API calls towards www.reddit.com
        Retrofit oauthRetrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(REDDIT_URL)
                .client(oauthClient)
                .build();
        oauthService = oauthRetrofit.create(OAuthService.class);
    }

    /**
     * Builder for API objects
     */
    public static class Builder {
        private boolean built = false;

        private final String userAgent;
        private final String clientId;

        private AccessToken accessToken;
        private OnNewToken onNewToken;
        private HttpLoggingInterceptor logger;
        private String callbackUrl;
        private String deviceId;


        /**
         * Creates a new API builder. The required parameters for the API are User-Agent and client ID, which
         * are the parameters to this constructor.
         *
         * @param userAgent The user agent for the application. This cannot be changed after the instance is created
         *                  <p>See <a href="https://github.com/reddit-archive/reddit/wiki/API">Reddit documentation</a>
         *                  on creating your user agent</p>
         * @param clientId The client ID of the application
         *                 <p>To find your client ID see <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>
         *
         * @throws IllegalStateException If userAgent or clientId is empty
         */
        public Builder(@NotNull String userAgent, @NotNull String clientId) {
            if (userAgent.isEmpty()) {
                throw new IllegalStateException("User Agent must not be empty. See https://github.com/reddit-archive/reddit/wiki/API for more information");
            } else if (clientId.isEmpty()) {
                throw new IllegalStateException("Client ID must not be empty. See https://www.reddit.com/prefs/apps for more information");
            }

            this.userAgent = userAgent;
            this.clientId = clientId;

            // Variables that cannot be null
            this.accessToken = new AccessToken();
            this.logger = new HttpLoggingInterceptor();
        }

        /**
         * Sets the access token to use for authorized API calls
         *
         * <p>This only has to be set during the initial initialization. When new tokens are retrieved
         * the internal value is set automatically. To retrieve the new token use {@link RedditApi.Builder#onNewToken(OnNewToken)}</p>
         *
         * <p>If the passed access token is null it is not added</p>
         *
         * @param accessToken The token to use
         */
        public Builder accessToken(AccessToken accessToken) {
            if (accessToken != null) {
                this.accessToken = accessToken;
            }
            return this;
        }

        /**
         * Sets the callback for when new access tokens have been received. If an access token is set a
         * new one is automatically retrieved when a request is attempted with an invalid token.
         * This sets the listener for what to do when a new token is received by the API
         *
         * @param onNewToken The token listener. Holds an {@link AccessToken} object
         */
        public Builder onNewToken(OnNewToken onNewToken) {
            this.onNewToken = onNewToken;
            return this;
        }

        /**
         * Sets the {@link HttpLoggingInterceptor.Level} to use for logging of the API calls.
         *
         * @param level The level to log
         */
        public Builder loggerLevel(HttpLoggingInterceptor.Level level) {
            this.logger.setLevel(level);
            return this;
        }

        /**
         * Sets the callback URL used for OAuth. This is used when retrieving access tokens
         * <p>This must match the callback URL set in <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>
         *
         * @param callbackUrl The URL to use for OAuth access tokens
         */
        public Builder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        /**
         * Sets the device ID to use to receive access tokens for non-logged in users
         *
         * <p>If this is null or empty "DO_NOT_TRACK_THIS_DEVICE" will be used. See
         * <a href="https://github.com/reddit-archive/reddit/wiki/OAuth2#application-only-oauth">
         *     Reddit OAuth documentation</a> for more information</p>
         *
         * @param deviceId The device ID to use
         */
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }


        /**
         * Builds the API
         *
         * @throws IllegalStateException if the builder has already been built
         * @return An API object with the corresponding values set
         */
        public RedditApi build() {
            if (!built) {
                built = true;
            } else {
                throw new IllegalStateException("Builder is already used. Use 'new RedditApi.Builder()' to create a new one");
            }

            RedditApi api = new RedditApi(userAgent, clientId);
            api.accessToken = accessToken;
            api.onNewToken = onNewToken;
            api.logger = logger;
            api.callbackUrl = callbackUrl;
            api.deviceId = deviceId;

            api.createServices();

            return api;
        }
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
     * {@link RedditApi.Builder#onNewToken(OnNewToken)} when creating the API object to retrieve the new token</p>
     * <p>Note: The callback URL must be set with {@link RedditApi.Builder#callbackUrl(String)}</p>
     *
     * @param code The authorization code retrieved from the initial login process
     * @param onResponse The callback for successful requests. Does not hold anything, but is called with
     *                   {@code null} when successful.
     * @param onFailure The callback for failed requests
     *
     * @throws IllegalStateException If the callback URL was not set when the API object was built
     */
    @EverythingIsNonNull
    public void getAccessToken(String code, OnResponse<Void> onResponse, OnFailure onFailure) {
        if (callbackUrl == null) {
            throw new IllegalStateException("Callback URL is not set. Use RedditApi.Builder.callbackUrl()");
        }

        oauthService.getAccessToken(
                basicAuthHeader,
                code,
                OAuthConstants.GRANT_TYPE_AUTHORIZATION,
                callbackUrl
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
                    onFailure.onFailure(response.code(), Util.newThrowable(response.code()));
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
        if (accessToken.getRefreshToken() == null) {
            onFailure.onFailure(-1, new Throwable("No token to revoke"));
            return;
        }

        oauthService.revokeToken(
                basicAuthHeader,
                accessToken.getRefreshToken(),
                OAuthConstants.TOKEN_TYPE_REFRESH
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    onResponse.onResponse(null);
                } else {
                    onFailure.onFailure(response.code(), Util.newThrowable(response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(-1, t);
            }
        });

        setTokenInternal(new AccessToken());
    }
    /* --------------- End access token calls --------------- */


    /**
     * Retrieve a {@link PostRequest} object that can be used to make API calls towards posts
     *
     * @param postId The ID of the post to make calls towards
     * @return An object that can perform various post related API requests
     */
    public PostRequest post(String postId) {
        return new PostRequest(accessToken, postApi, postId);
    }

    /**
     * Retrieve a {@link CommentRequest} object that can be used to make API calls towards comments
     *
     * @param commentId The ID of the comment
     * @return An object that can perform various comment related API requests
     */
    public CommentRequest comment(String commentId) {
        return new CommentRequest(commentId, accessToken, commentApi);
    }

    /**
     * Retrieve a {@link SubredditRequest} object that can be used to make API calls towards subreddits
     *
     * @param subredditName The name of the subreddit to make calls towards
     * @return An object that can perform various subreddit related API requests
     */
    public SubredditRequest subreddit(String subredditName) {
        return new SubredditRequest(accessToken, subredditApi, subredditName);
    }

    /**
     * Retrieve a {@link SubredditsRequest} object that can be used to make API calls towards subreddits.
     * This differs from {@link RedditApi#subreddit(String)} as this is for multiple subreddits (like
     * getting subreddits a user is subscribed to), not one specific subreddit
     *
     * @return An object that can perform various subreddit related API requests
     */
    public SubredditsRequest subreddits() {
        return new SubredditsRequest(accessToken, subredditsApi);
    }

    /**
     * Retrieve a {@link UserRequests} object for logged in users only. For non-logged in users
     * use {@link RedditApi#user(String)}.
     *
     * @return An object that can perform various user related API requests
     */
    public UserRequests user() {
        return new UserRequests(userApi, accessToken);
    }

    /**
     * Retrieve a {@link UserRequests} object that can get handle requests for non-logged in users.
     * For logged in users use {@link RedditApi#user()}
     *
     * @param username the username to to make calls towards. Passing {@code null} to this has
     *                 the same effect as using {@link RedditApi#user()}.
     *
     * @return An object that can perform various user related API requests for non-logged in users
     */
    public UserRequests user(String username) {
        return new UserRequests(userApi, accessToken, username);
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
            String device = (deviceId == null || deviceId.isEmpty() ? "DO_NOT_TRACK_THIS_DEVICE" : deviceId);

            newToken = oauthService.getAccessTokenNoUser(
                    basicAuthHeader,
                    OAuthConstants.GRANT_TYPE_INSTALLED_CLIENT,
                    device
            ).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newToken;
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
