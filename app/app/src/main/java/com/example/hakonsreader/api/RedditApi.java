package com.example.hakonsreader.api;

import androidx.annotation.NonNull;

import com.example.hakonsreader.api.constants.OAuthConstants;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.interceptors.UserAgentInterceptor;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnNewToken;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.requestmodels.CommentRequest;
import com.example.hakonsreader.api.requestmodels.PostRequest;
import com.example.hakonsreader.api.requestmodels.SubredditRequest;
import com.example.hakonsreader.api.requestmodels.SubredditsRequest;
import com.example.hakonsreader.api.requestmodels.UserRequests;
import com.example.hakonsreader.api.requestmodels.UserRequestsLoggedInUser;
import com.example.hakonsreader.api.requestmodels.UserRequestsLoggedInUserKt;
import com.example.hakonsreader.api.service.CommentService;
import com.example.hakonsreader.api.service.ImgurService;
import com.example.hakonsreader.api.service.OAuthService;
import com.example.hakonsreader.api.service.PostService;
import com.example.hakonsreader.api.service.SubredditService;
import com.example.hakonsreader.api.service.SubredditsService;
import com.example.hakonsreader.api.service.UserServiceKt;
import com.example.hakonsreader.api.service.UserService;
import com.example.hakonsreader.api.utils.Util;
import com.example.hakonsreader.api.responses.GenericError;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
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
 * Android wrapper for the Reddit API for installed applications.
 * For an example application see <a href="https://github.com/hakonschia/reddit-android-client">reddit-android-client</a>
 * <br>
 *
 *
 * <p>The API objects are built with a builder. Some parameters used in the API might not be applicable
 * and can therefore be omitted. </p>
 *
 * <p>Example usage of the builder:
 * <pre>{@code
 * // User-Agent and client ID must always be set and cannot be empty
 * String userAgent = "User-Agent for your application";
 * String clientId = "client ID for your application";
 *
 * RedditApi api = new RedditApi.Builder(userAgent, clientId)
 *         // Set the initial access token to use (when the application has previously saved one)
 *         .accessToken(savedAccessToken)
 *         // Register the callback for when new access tokens have been retrieved
 *         .onNewToken(saveTokenCallback)
 *         .build();
 * }
 * </pre>
 * </p>
 * <br>
 *
 *
 * <p>The class exposes request objects through functions that return these models (such as
 * {@link RedditApi#subreddit(String)} returning a {@link SubredditRequest} object). These request objects expose API
 * endpoints that are grouped together based on their functionality.</p>
 *
 * <p>The endpoints exposed handle responses back with the use of callback methods. There will be generally be
 * two callbacks for each endpoint; one for successful responses ({@link OnResponse}) and one for failed responses
 * ({@link OnFailure}). If the API endpoint returns multiple values that are of different types (such as
 * {@link PostRequest#comments(OnResponse, OnResponse, OnFailure)}) there will be multiple {@link OnResponse} callbacks.
 * The {@link OnFailure} includes a {@link GenericError} and a {@link Throwable} with error information.
 * The error code in {@link GenericError} will be the HTTP error code, or if another type of error occurred this will be -1
 * and the throwable will hold the information needed to debug the issue.</p>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Build object with RedditApi.Builder
 * RedditApi api = ...
 *
 * // Retrieve information about the "GlobalOffensive" subreddit
 * api.subreddit("GlobalOffensive").info(subreddit -> {
 *     String subredditName = subreddit.getName();
 *     int subscribers = subreddit.getSubscribers();
 * }, (error, throwable) -> {
 *      int errorCode = error.getCode();
 *      throwable.printStackTrace();
 * });
 *
 * api.subreddit("Norge").subscribe(true, response -> {
 *     // Some endpoints won't have a return value (they take a callback of OnResponse<Void>)
 *     // For these endpoints these callbacks are still called to indicate that the response was
 *     // successful, but will not have any data so "response" is never used
 * }, (error, throwable) -> {
 *     // This is still called as normal
 *     int errorCode = error.getCode();
 *     throwable.printStackTrace();
 * });
 * }
 * </pre>
 * </p>
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
     * The OAuth sub domain URL for Reddit.
     *
     * <p>This is used to retrieve posts from reddit</p>
     */
    public static final String REDDIT_OUATH_URL = "https://oauth.reddit.com/";

    /**
     * The URL to the Imgur API
     */
    public static final String IMGUR_API_URL = "https://api.imgur.com/";

    /**
     * The list of standard subs: front page (represented as an empty string), popular, all.
     *
     * <p>Note: The elements in this list are case sensitive and in this list are all lower case.
     * When using this list to check against a standard sub you should ensure the string is lower cased, or use
     * {@link String#equalsIgnoreCase(String)}</p>
     *
     * <p>Note: This is an unmodifiable list. Attempting to change it will throw an exception</p>
     */
    public static final List<String> STANDARD_SUBS = Collections.unmodifiableList(Arrays.asList("", "popular", "all"));


    /**
     * The service object used to communicate with the Reddit API about user related calls
     */
    private UserService userApi;

    /**
     * The service object used to communicate with the Reddit API about user related calls
     * for Kotlin
     */
    private UserServiceKt userApiKt;

    /**
     * The service object used to communicate with the Reddit API about subreddit related calls,
     * such as getting posts for the subreddit and subscribing
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
     * The service object used to communicate with the Imgur API
     */
    private ImgurService imgurService;


    /**
     * The access token to use for authorized API calls
     *
     * <p>Note: never set this to {@code null}, use {@code new AccessToken()} instead. To update the
     * value use {@link RedditApi#setTokenInternal(AccessToken)} instead of updating it directly</p>
     */
    @NotNull
    private AccessToken accessToken = new AccessToken();

    /**
     * The saved access token stored when the API is in a private browsing context. This will reference
     * what {@link RedditApi#accessToken} was at the point when private browsing was set, and should
     * be used to set the token again when private browsing is disabled
     *
     * <p>This should be set to {@code null} when private browsing is disabled</p>
     */
    @Nullable
    private AccessToken savedToken = null;

    /**
     * The listener for when the authenticator retrieves a new token automatically
     */
    private OnNewToken onNewToken;

    /**
     * The logger for debug information
     */
    private HttpLoggingInterceptor logger;

    /**
     * The callback for when the access token isn't valid
     */
    private OnFailure onInvalidToken;

    /**
     * The OAuth client ID for Imgur (for optionally loading Imgur albums as galleries)
     */
    private String imgurClientId;


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
                .addInterceptor(new TokenInterceptor())
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(logger)
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        // Create the API service used to make calls towards oauth.reddit.com
        Retrofit apiRetrofit = new Retrofit.Builder()
                .baseUrl(REDDIT_OUATH_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(apiClient)
                .build();

        userApi = apiRetrofit.create(UserService.class);
        userApiKt = apiRetrofit.create(UserServiceKt.class);
        subredditApi = apiRetrofit.create(SubredditService.class);
        subredditsApi = apiRetrofit.create(SubredditsService.class);
        postApi = apiRetrofit.create(PostService.class);
        commentApi = apiRetrofit.create(CommentService.class);

        // For Imgur we don't need any authentication, and adding it would cause issues
        // as adding the access token for Reddit would break things for Imgur, so only add the logger

        if (imgurClientId != null && !imgurClientId.isEmpty()) {
            OkHttpClient imgurClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Client-ID " + imgurClientId)
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logger)
                    .build();

            Retrofit imgurRetrofit = new Retrofit.Builder()
                    .baseUrl(IMGUR_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(imgurClient)
                    .build();
            imgurService = imgurRetrofit.create(ImgurService.class);
        }

        // The OAuth client does not need interceptors/authenticators for tokens as it doesn't
        // use the access tokens for authorization
        OkHttpClient oauthClient = new OkHttpClient.Builder()
                // Add User-Agent header to every request
                .addInterceptor(new UserAgentInterceptor(userAgent))
                .addInterceptor(logger)
                .addNetworkInterceptor(new StethoInterceptor())
                .build();

        // Create the API service used to make API calls towards www.reddit.com
        Retrofit oauthRetrofit = new Retrofit.Builder()
                .baseUrl(REDDIT_URL)
                .addConverterFactory(GsonConverterFactory.create())
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

        // The logger can be final as the logger object will never be changed, but its logging level will be
        private final HttpLoggingInterceptor logger;
        private AccessToken accessToken;
        private OnNewToken onNewToken;
        private OnFailure onInvalidToken;
        private String callbackUrl;
        private String deviceId;
        private String imgurClientId;


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
         * <p>If the passed access token is {@code null} it is not added</p>
         *
         * @param accessToken The token to use
         * @return This builder
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
         * @return This builder
         */
        public Builder onNewToken(OnNewToken onNewToken) {
            this.onNewToken = onNewToken;
            return this;
        }

        /**
         * Sets a callback for when the API attempts to refresh an access token that is no longer valid.
         *
         * <p>This will be called if the access token set with {@link Builder#accessToken(AccessToken)}
         * wasn't valid (ie. it can't be refreshed anymore), or if the user has revoked the applications
         * access from <a href="https://reddit.com/prefs/apps">reddit.com/prefs/apps</a></p>
         *
         * <p>When the API notices an invalid token an attempt to get a token for a non-logged in user
         * is automatically attempted. If this also fails, an empty access token is set. This change
         * is communicated through {@link Builder#onNewToken(OnNewToken)} as with other new tokens.</p>
         *
         * @param onInvalidToken The callback for when tokens are now invalid
         * @return This builder
         */
        public Builder onInvalidToken(OnFailure onInvalidToken) {
            this.onInvalidToken = onInvalidToken;
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
         * @return This builder
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
         * @return This builder
         */
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        /**
         * If set this will make Imgur albums load as Reddit galleries.
         *
         * <p>Typically, an Imgur album will be represented as {@link PostType#LINK}. With this set to
         * true the API will automatically call the Imgur API when posts are received and get the individual
         * images and store them so they are accessible through {@link RedditPost#getGalleryImages()}  }
         * in the same way as a normal Reddit gallery would. The post type will be {@link PostType#GALLERY}.
         * While Imgur albums are typically for multiple images, these albums sometimes only contain one image.
         * The API will still treat one image albums as a gallery</p>
         *
         * <p>Note that since this will call the Imgur API loading times for posts will increase when there are Imgur albums.
         * No failure callback will be called if these API calls fail, and the post will be as a standard
         * {@link PostType#LINK}.</p>
         *
         * <p>Using this option requires an Imgur OAuth client. Only public endpoints are used, so
         * an OAuth client for anonymous use is sufficient.</p>
         *
         * @param imgurClientId The Client ID for your Imgur OAuth application. See
         *                      <a href="https://api.imgur.com/oauth2/addclient">Imgur.com</a>
         * @return This builder
         */
        public Builder loadImgurAlbumsAsRedditGalleries(String imgurClientId) {
            this.imgurClientId = imgurClientId;
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
            api.onInvalidToken = onInvalidToken;
            api.imgurClientId = imgurClientId;

            api.createServices();

            return api;
        }
    }

    /**
     * Convenience method to set the internal token. This calls the registered token listener
     *
     * @param token The token to set. If this is set to null, a new empty access token is created
     */
    private void setTokenInternal(@Nullable AccessToken token) {
        if (token == null) {
            token = new AccessToken();
        }

        accessToken = token;

        // Call token listener if registered
        // If we are currently in a private browsing context, don't notify the listener as the
        // tokens retrieved at this point shouldn't be forwarded to the API users
        if (onNewToken != null && !isPrivatelyBrowsing()) {
            onNewToken.newToken(token);
        }
    }

    /**
     * Enable or disable private browsing. Enabling private browsing will temporarily set an anonymous
     * access token to be used for API calls
     *
     * <p>When non-logged in access tokens are retrieved, the listener set with
     * {@link Builder#onNewToken(OnNewToken)} will NOT be notified</p>
     *
     * <p>This operation is idempotent</p>
     *
     * @param enable True to enable private browsing, false to disable it
     * @see RedditApi#isPrivatelyBrowsing()
     */
    public void enablePrivateBrowsing(boolean enable) {
        if (enable) {
            // If we're in a private browsing context already, calling this again would override the stored token
            if (savedToken != null) {
                return;
            }
            savedToken = accessToken;
            setTokenInternal(new AccessToken());
        } else {
            // Ie. if we weren't in a private browsing context, this shouldn't do anything
            if (savedToken == null) {
                return;
            }
            setTokenInternal(savedToken);
            savedToken = null;
        }
    }

    /**
     * Checks if the API is currently in a private browsing context, meaning that there is a user
     * logged in, but the API calls should currently not be made on behalf of that user
     *
     * @return True if private browsing is enabled
     * @see RedditApi#enablePrivateBrowsing(boolean)
     */
    public boolean isPrivatelyBrowsing() {
        return savedToken != null;
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
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
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
            onFailure.onFailure(new GenericError(-1), new Throwable("No token to revoke"));
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
                    Util.handleHttpErrors(response, onFailure);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                onFailure.onFailure(new GenericError(-1), t);
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
        return new CommentRequest(accessToken, commentApi, commentId);
    }

    /**
     * Retrieve a {@link SubredditRequest} object that can be used to make API calls towards subreddits
     *
     * @param subredditName The name of the subreddit to make calls towards
     * @return An object that can perform various subreddit related API requests
     */
    public SubredditRequest subreddit(String subredditName) {
        return new SubredditRequest(subredditName, accessToken, subredditApi, imgurService);
    }

    /**
     * Retrieve a {@link SubredditsRequest} object that can be used to make API calls towards subreddits.
     * This differs from {@link RedditApi#subreddit(String)} as this is for multiple subreddits (like
     * getting subreddits a user is subscribed to), not one specific subreddit
     *
     * @return An object that can perform various subreddits related API requests
     */
    public SubredditsRequest subreditts() {
        return new SubredditsRequest(accessToken, subredditsApi);
    }

    /**
     * Retrieve a {@link UserRequests} object for logged in users only. For non-logged in users
     *
     * @return An object that can perform various user related API requests
     */
    public UserRequestsLoggedInUser user() {
        return new UserRequestsLoggedInUser(accessToken, userApi);
    }

    /**
     * Retrieve a kotlin based {@link UserRequests} object that can get handle requests for non-logged in users.
     * For logged in users use {@link #user()}
     *
     * @param username the username to to make calls towards.
     *
     * @return An object that can perform various user related API requests for non-logged in users
     */
    public UserRequests user(@NonNull String username) {
        return new UserRequests(username, accessToken, userApiKt, imgurService);
    }

    /**
     * Retrieve a Kotlin based request object that offers API calls for logged in users
     *
     * <p>For logged in users use {@link RedditApi#user(String)}</p>
     *
     * @return An object that can perform various user related API requests for logged in users
     * @see #user(String)
     */
    public UserRequestsLoggedInUserKt userKt() {
        return new UserRequestsLoggedInUserKt(accessToken, userApiKt);
    }


    /* ----------------- Misc ----------------- */
    /**
     * Retrieve a new access token valid for non-logged in users
     *
     * @return A new access token only valid for non-logged in users, or null if an error occurred
     */
    @Nullable
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
            AccessToken newToken = null;

            // If we have an access token with a refresh token the token is for a logged in user and can be refreshed
            if (accessToken.getRefreshToken() != null) {
                newToken = refreshToken();

                // Token refreshed
                if (newToken != null) {
                    // The response does not send a new refresh token, so make sure the old one is saved
                    newToken.setRefreshToken(accessToken.getRefreshToken());
                }
            }

            // No new token yet, either no user is logged in or the refresh failed
            if (newToken == null) {
                newToken = newNonLoggedInToken();
            }

            // New token received
            if (newToken != null) {
                setTokenInternal(newToken);

                return response.request().newBuilder()
                        .header("Authorization", newToken.generateHeaderString())
                        .build();
            } else {
                // No new token received, we can't do anything so cancel the request
                return null;
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
                Response<AccessToken> call =  oauthService.refreshToken(
                        basicAuthHeader,
                        accessToken.getRefreshToken(),
                        OAuthConstants.GRANT_TYPE_REFRESH
                ).execute();

                newToken = call.body();

                // If we get a 400 Bad Request when attempting to refresh the token, the token has been
                // invalidated outside of the control of the API (ie. the applications access from reddit.com/prefs/apps
                // was revoked), or the access token set was never valid
                // Call the listener registered when the API object was built to notify that the token isn't valid anymore
                int code = call.code();
                if (code == 400 && onInvalidToken != null) {
                    onInvalidToken.onFailure(
                            new GenericError(code),
                            new InvalidAccessTokenException("The access token couldn't be refreshed. Either the access token set when building the API object" +
                                    " was never valid, or the user has revoked the applications access to their account.")
                    );
                }

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
     * Interceptor that ensures that an access token is set and added as a request header.
     *
     * <p>If no token is found a new token for non-logged in users is retrieved</p>
     */
    private class TokenInterceptor implements Interceptor {
        @NotNull
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder request = original.newBuilder();

            if (accessToken.getAccessToken() == null) {
                AccessToken token = newNonLoggedInToken();

                if (token != null) {
                    setTokenInternal(token);
                }
            }

            request.header("Authorization", accessToken.generateHeaderString());

            return chain.proceed(request.build());
        }
    }
}
