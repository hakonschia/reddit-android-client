package com.example.hakonsreader.api

import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.interceptors.UserAgentInterceptor
import com.example.hakonsreader.api.interfaces.OnFailure
import com.example.hakonsreader.api.interfaces.OnNewToken
import com.example.hakonsreader.api.interfaces.VoteableListing
import com.example.hakonsreader.api.interfaces.VoteableRequest
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditListing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.requestmodels.*
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.*
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*

/**
 * Android wrapper for the Reddit API for installed applications.
 * For an example application see [reddit-android-client](https://github.com/hakonschia/reddit-android-client)
 *
 * When building objects most parameters are optional. Read the documentation to ensure the objects fit
 * your needs.
 *
 * Example creation of an object:
 * ```
 * val api = RedditApi(
 *    // User-Agent and client ID must always be set and cannot be empty
 *    userAgent = "android:package-name v1.0.0 (by /u/hakonschia)",
 *    clientId = "Client-ID",
 *
 *    // Set the initial access token to use (when the application has previously saved one)
 *    accessToken = savedAccessToken,
 *    // Register the callback for when new access tokens have been retrieved
 *    onNewToken = saveTokenCallback
 * )
 * ```
 *
 * API endpoints are grouped together based on functionality, and are exposed through request models.
 * The request models are returned from functions in this class, such as [subreddit] returning a
 * [SubredditRequest] object.
 *
 * The API endpoints are suspended using Kotlin coroutines to make the network requests. The functions
 * will return an [ApiResponse] object. This class is a wrapper for either a [ApiResponse.Success] or
 * [ApiResponse.Error].
 *
 * On successful requests, [ApiResponse.Success] will be populated with with the response data,
 * retrieved with [ApiResponse.Success.value]. On failed requests, [ApiResponse.Error] will be populated
 * with a [GenericError] and a [Throwable].The error code in [GenericError] will be the HTTP error code,
 * or if another type of error occurred this will be -1 and the throwable will hold the information needed to identify the issue.
 *
 * Usage example:
 * ```
 * val api = RedditApi()...
 *
 * // Retrieve information about the "GlobalOffensive" subreddit
 * // The response for this is ApiResponse<Subreddit>
 * CoroutineScope(IO).launch {
 *     val response = api.subreddit("GlobalOffensive").info()
 *
 *     when (response) {
 *         is ApiResponse.Success -> {
 *             val subreddit = response.value
 *             val subredditName = subreddit.name
 *             val subscribers = subreddit.subscribers
 *         }
 *         is ApiResponse.Error {
 *             val errorCode = response.error.code
 *             responses.throwable.printStackTrace()
 *         }
 *     }
 * }
 *
 * // Subscribe to the subreddit "Norge"
 * CoroutineScope(IO).launch {
 *     val response = api.subreddit("Norge").subscribe(subscribe = true)
 *
 *     when (response) {
 *         is ApiResponse.Success -> {
 *             // Some endpoints won't have a return value (they return ApiResponse<Any?>)
 *             // For these endpoints "response" will still be "ApiResponse.Success", but
 *             // "response.value" will be null
 *             val isNull = response.value == null // true
 *         }
 *         is ApiResponse.Error -> {
 *              val errorCode = response.error.code
 *              responses.throwable.printStackTrace()
 *         }
 *     }
 * }
 * ```
 *
 * Models relating to Reddit specific content (such as posts or comments) inherit from [RedditListing].
 * Most listings will also implement a variety of interfaces depending on what kind of listing it is.
 * For example, [RedditPost] and [RedditComment] implement [VoteableListing]. The respective request models
 * (eg. [PostRequest] for [RedditPost]) will also implement the corresponding interface [VoteableRequest].
 * When performing API requests on these listings these interfaces can be taken advantage of to generify the code:
 *
 * ```
 * val api = ...
 * val listing: VoteableListing = ...
 * val voteType = VoteType.UPVOTE
 *
 * CoroutineScope(IO).launch {
 *     // Currently only RedditPost and RedditComment implement VoteableListing
 *     val response = if (listing is RedditPost) {
 *         api.post(listing.id)
 *     } else {
 *         api.comment(listing.id)
 *     }.vote(voteType)
 *     // The value returned is a VoteableRequest, and we invoke the function "vote(VoteType)" from
 *     // that interface which performs the API request
 *
 *     when (response) {
 *         is ApiResponse.Success -> {}
 *         is ApiResponse.Error -> {}
 *     }
 * }
 * ```
 *
 *
 * @param userAgent The user agent for the application.
 * See [Reddit documentation](https://github.com/reddit-archive/reddit/wiki/API) on creating your user agent
 *
 * @param clientId The client ID of the application. To find your client ID see [Reddit apps](https://www.reddit.com/prefs/apps)
 *
 * @param accessToken Sets the initial access token to use for authorized API calls
 *
 * When new tokens are retrieved the internal value is set automatically. To retrieve the new token
 * register a callback with [onNewToken]
 *
 * @param onNewToken The callback for when new access tokens have been received. If an access token is
 * set a new one is automatically retrieved when a request is attempted with an invalid token.
 * This sets the listener for what to do when a new token is received by the API
 *
 * @param onInvalidToken The callback for when the API attempts to refresh an access token that is no longer valid.
 *
 * This will be called if the access token from [accessToken] wasn't valid (ie. it can't be refreshed
 * anymore), or if the user has revoked the applications access from [reddit.com/prefs/apps](https://reddit.com/prefs/apps)
 *
 * When the API notices an invalid token, an attempt to get a token for a non-logged in user
 * is automatically attempted. If this also fails, an empty access token is set. This change
 * is communicated through [onNewToken] as with other new tokens.
 *
 * @param callbackUrl The callback URL used for OAuth. This is used when retrieving access tokens
 * <p>This must match the callback URL set in <a href="https://www.reddit.com/prefs/apps">Reddit apps</a></p>
 *
 * @param deviceId The device ID to use to receive access tokens for non-logged in users
 * If this is null or empty "DO_NOT_TRACK_THIS_DEVICE" will be used. See
 * [Reddit OAuth documentation](https://github.com/reddit-archive/reddit/wiki/OAuth2#application-only-oauth)
 *
 * @param imgurClientId The client ID for your Imgur OAuth application. Only public endpoints are used, so
 * an OAuth client for anonymous use is sufficient. If set this will perform certain API calls
 * towards Imgur to load post content directly. Currently albums and gifs are loaded.
 *
 * Typically, an Imgur album will be represented as [PostType.LINK]. With this set to
 * true the API will automatically call the Imgur API when posts are received and get the individual
 * images and store them so they are accessible through [RedditPost.galleryImages]
 * in the same way as a normal Reddit gallery would. The post type will be [PostType.GALLERY].
 * While Imgur albums are typically for multiple images, these albums sometimes only contain one image.
 * The API will still treat one image albums as a gallery.
 *
 * Note that since this will call the Imgur API loading times for posts will increase.
 *
 * Using this option requires an Imgur OAuth client. Only public endpoints are used, so
 * an OAuth client for anonymous use is sufficient.
 *
 * The Client ID for your Imgur OAuth application. See [imgur.com](https://api.imgur.com/oauth2/addclient)
 *
 * @param cache The cache to use for network requests. This cache will be used for requests sent to
 * Reddit, for third party caching see [thirdPartyCache]. When setting this, you should also set [cacheAge]
 * @param cacheAge The max age for using [cache]
 *
 * @param thirdPartyCache Sets the cache to use for network requests to third party service, such as Gfycat and Imgur.
 * For the cache for reddit requests, see [cache]. When setting this, you should also set [thirdPartyCacheAge]
 * @param thirdPartyCacheAge The max age for the third party cache
 *
 * @param loggerLevel The [HttpLoggingInterceptor.Level] to use for logging of the API calls
 *
 * @throws IllegalStateException If [userAgent] or [clientId] is empty
 */
class RedditApi constructor(
        private val userAgent: String,
        private val clientId: String,

        private val accessToken: AccessToken? = null,
        private val onNewToken: OnNewToken? = null,
        private val onInvalidToken: OnFailure? = null,

        private val callbackUrl: String? = null,
        private val deviceId: String? = null,
        private val imgurClientId: String? = null,

        private val cache: Cache? = null,
        private val cacheAge: Long = 0L,
        private val thirdPartyCache: Cache? = null,
        private val thirdPartyCacheAge: Long = 0L,

        private val loggerLevel: HttpLoggingInterceptor.Level? = null,
) {
    companion object {
        /**
         * The list of standard subs: front page (represented as an empty string), popular, all.
         *
         * Note: The elements in this list are case sensitive and in this list are all lower case.
         * When using this list to check against a standard sub you should ensure the string is lower cased, or use
         * [String.equals] with ignoreCase = true
         *
         * Note: This is an unmodifiable list. Attempting to change it will throw an exception
         */
        val STANDARD_SUBS: List<String> = Collections.unmodifiableList(listOf("", "popular", "all"))
    }


    init {
        check(userAgent.isNotBlank()) { "User-Agent must not be empty" }
        check(clientId.isNotBlank()) { "Client ID must not be empty" }
        createServices()
    }

    /**
     * If true [onNewToken] should not be called the next time [accessTokenInternal] is set
     */
    private var ignoreNextTokenChange = false

    /**
     * The access token that is used internally. When this is set, [onNewToken] is notified
     * if [isPrivatelyBrowsing] returns false
     */
    private var accessTokenInternal = accessToken ?: AccessToken()
        set(value) {
            field = value

            if (ignoreNextTokenChange) {
                ignoreNextTokenChange = false
                return
            }

            if (!isPrivatelyBrowsing()) {
                onNewToken?.newToken(field)
            }
        }


    /**
     * The saved access token stored when the API is in a private browsing context. This will reference
     * what [accessToken] was at the point when private browsing was set, and should
     * be used to set the token again when private browsing is disabled
     *
     * This should be set to `null` when private browsing is disabled
     */
    private var savedToken: AccessToken? = null

    /**
     * The authentication header to use when retrieving access tokens for the first time
     *
     * This is formatted as "Basic <value>" and can be used directly as a header value
     */
    private val basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString("$clientId:".toByteArray())


    // ------------------ Service objects ------------------
    /**
     * The service object used to communicate with the Reddit API about access token calls (OAuth calls)
     */
    private lateinit var accessTokenApi: AccessTokenService

    /**
     * The service object used to communicate with the Reddit API about user related calls
     */
    private lateinit var userApi: UserService

    /**
     * The service object used to communicate with the Reddit API about subreddit related calls,
     * such as getting posts for the subreddit and subscribing
     */
    private lateinit var subredditApi: SubredditService

    /**
     * The service object used to communicate with the Reddit API about multiple subreddits related calls
     */
    private lateinit var subredditsApi: SubredditsService

    /**
     * The service object used to communicate with the Reddit API about post related calls
     */
    private lateinit var postApi: PostService

    /**
     * The service object used to communicate with the Reddit API about comment related calls
     */
    private lateinit var commentApi: CommentService

    /**
     * The service object used to communicate with the Reddit API about message related calls
     */
    private lateinit var messageApi: MessageService

    /**
     * The service object used to communicate with the Imgur API
     */
    private var imgurService: ImgurService? = null

    /**
     * The service object used to communicate with the Gfycat/Redgifs API
     */
    private lateinit var gfycatService: GfycatService
    // ------------------ End service objects ------------------

    private fun createServices() {
        val logger = HttpLoggingInterceptor().apply {
            level = loggerLevel ?: HttpLoggingInterceptor.Level.NONE
        }

        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                    .header("Cache-Control", "public, max-age=$cacheAge")
                    .removeHeader("Pragma")
                    .build()
            chain.proceed(request)
        }

        // Http client for API calls that use an access token as the authorization
        val redditClient = OkHttpClient.Builder()
                // Automatically refresh access token on authentication errors (401)
                .authenticator(Authenticator())
                // Add User-Agent header to every request
                .addInterceptor(UserAgentInterceptor(userAgent))
                // Ensure that an access token is always set before sending a request
                .addInterceptor(TokenInterceptor())
                .addNetworkInterceptor(cacheInterceptor)
                .cache(cache)
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(logger)
                .build()

        // Create the API service used to make calls towards oauth.reddit.com. This is used for
        // any API call towards Reddit that doesn't have to do with authentication
        Retrofit.Builder()
                .baseUrl("https://oauth.reddit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(redditClient)
                .build()
                .apply {
                    userApi = create(UserService::class.java)
                    subredditApi = create(SubredditService::class.java)
                    subredditsApi = create(SubredditsService::class.java)
                    postApi = create(PostService::class.java)
                    commentApi = create(CommentService::class.java)
                    messageApi = create(MessageService::class.java)
                }

        val thirdPartyCacheInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                    .header("Cache-Control", "public, max-age=$thirdPartyCacheAge")
                    .removeHeader("Pragma")
                    .build()
            chain.proceed(request)
        }

        if (!imgurClientId.isNullOrBlank()) {
            val imgurClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                                .header("Authorization", "Client-ID $imgurClientId")
                                .build()
                        chain.proceed(request)
                    }
                    .addInterceptor(thirdPartyCacheInterceptor)
                    .cache(thirdPartyCache)
                    .addInterceptor(logger)
                    .build()

            Retrofit.Builder()
                    .baseUrl("https://api.imgur.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(imgurClient)
                    .build()
                    .apply {
                        imgurService = create(ImgurService::class.java)
                    }
        }

        val gfycatClient = OkHttpClient.Builder()
                .addInterceptor(thirdPartyCacheInterceptor)
                .cache(thirdPartyCache)
                .addInterceptor(logger)
                .build()
        Retrofit.Builder()
                .baseUrl("https://api.gfycat.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(gfycatClient)
                .build()
                .apply {
                    gfycatService = create(GfycatService::class.java)
                }

        // Http client for OAuth related API calls (such as retrieving access tokens)
        // The service created with this is for "RedditApi.accessToken()"
        val oauthClient = OkHttpClient.Builder()
                .addInterceptor(BasicAuthInterceptor())
                // Logger has to be at the end or else it won't log what has been added before
                .addInterceptor(logger)
                .build()
        Retrofit.Builder()
                // Authentication calls go to www.reddit.com, not oauth.reddit.com
                .baseUrl("https://www.reddit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(oauthClient)
                .build()
                .apply {
                    accessTokenApi = create(AccessTokenService::class.java)
                }
    }


    /**
     * Switches which access token to use. This should only be used to switch which account
     * the API is now for, and should not be used to manually update the token.
     *
     * Calling this will disable private browsing, if enabled
     *
     * @param accessToken The token to switch to
     */
    fun switchAccessToken(accessToken: AccessToken) {
        ignoreNextTokenChange = true
        accessTokenInternal = accessToken
        enablePrivateBrowsing(false)
    }

    /**
     * Checks if the API is currently in a private browsing context, meaning that there is a user
     * logged in, but the API calls should currently not be made on behalf of that user
     *
     * @return True if private browsing is enabled
     * @see enablePrivateBrowsing
     */
    fun isPrivatelyBrowsing() : Boolean {
        return savedToken != null
    }

    /**
     * Enable or disable private browsing. Enabling private browsing will temporarily set an anonymous
     * access token to be used for API calls
     *
     * When non-logged in access tokens are retrieved, the listener set with
     * [onNewToken] will NOT be notified
     *
     * This operation is idempotent
     *
     * @param enable True to enable private browsing, false to disable it
     * @see isPrivatelyBrowsing
     */
    fun enablePrivateBrowsing(enable: Boolean) {
        if (enable) {
            // If we're in a private browsing context already, calling this again would override the stored token
            if (savedToken != null) {
                return
            }
            savedToken = accessToken
            accessTokenInternal = AccessToken()
        } else {
            // Ie. if we weren't in a private browsing context, this shouldn't do anything
            if (savedToken == null) {
                return
            }

            accessTokenInternal = savedToken!!
            savedToken = null
        }
    }

    /**
     * Retrieves a [AccessTokenModel] that can be used to make API calls for access tokens
     * for logged in users.
     *
     * Access tokens for non-logged in users are handled automatically and doesn't require any specific code
     *
     * @return An object that can perform various access token related API requests
     * @throws IllegalStateException If [callbackUrl] is null or empty
     */
    fun accessToken(): AccessTokenModel {
        check(!callbackUrl.isNullOrBlank()) { "RedditApi.callbackUrl cannot be empty" }
        return AccessTokenModel(accessTokenApi, callbackUrl) { token: AccessToken? -> accessTokenInternal = token ?: AccessToken() }
    }

    /**
     * Retrieve a [PostRequest] object that can be used to make API calls towards posts
     *
     * @param postId The ID of the post to make calls towards
     * @return An object that can perform various post related API requests
     */
    fun post(postId: String): PostRequest {
        return PostRequest(accessTokenInternal, postApi, postId, imgurService, gfycatService)
    }

    /**
     * Retrieve a [CommentRequest] object that can be used to make API calls towards comments
     *
     * @param commentId The ID of the comment
     * @return An object that can perform various comment related API requests
     */
    fun comment(commentId: String): CommentRequest {
        return CommentRequest(accessTokenInternal, commentApi, commentId)
    }

    /**
     * Retrieve a [SubredditRequest] object that can be used to make API calls towards subreddits
     *
     * @param subredditName The name of the subreddit to make calls towards
     * @return An object that can perform various subreddit related API requests
     */
    fun subreddit(subredditName: String): SubredditRequest {
        return SubredditRequest(subredditName, accessTokenInternal, subredditApi, imgurService, gfycatService)
    }

    /**
     * Retrieve a [SubredditsRequest] object that can be used to make API calls towards subreddits.
     * This differs from [subreddit] as this is for multiple subreddits (like getting subreddits
     * a user is subscribed to), not one specific subreddit
     *
     * @return An object that can perform various subreddits related API requests
     */
    fun subreditts(): SubredditsRequest {
        return SubredditsRequest(accessTokenInternal, subredditsApi)
    }

    /**
     * Retrieve a [UserRequests] object that can get handle requests for non-logged in users.
     *
     * @param username the username to to make calls towards.
     * @return An object that can perform various user related API requests for non-logged in users
     */
    fun user(username: String): UserRequests {
        return UserRequests(username, accessTokenInternal, userApi, imgurService, gfycatService)
    }

    /**
     * Retrieve a Kotlin based request object that offers API calls for logged in users
     *
     * For logged in users use [user]
     *
     * @return An object that can perform various user related API requests for logged in users
     */
    fun user(): UserRequestsLoggedInUser {
        return UserRequestsLoggedInUser(accessTokenInternal, userApi)
    }

    /**
     * Retrieves a request object that offers API calls towards messages (inbox)
     *
     * @return An object that can perform various message related API requests for logged in users
     */
    fun messages(): MessagesRequestModel {
        return MessagesRequestModel(accessTokenInternal, messageApi)
    }

    /* ----------------- Misc ----------------- */
    /**
     * Retrieve a new access token valid for non-logged in users
     *
     * @return A new access token only valid for non-logged in users, or null if an error occurred
     */
    private fun newNonLoggedInToken(): AccessToken? {
        return try {
            val device = if (deviceId == null || deviceId.isEmpty()) "DO_NOT_TRACK_THIS_DEVICE" else deviceId
            accessTokenApi.getAccessTokenNoUserNoSuspend(device).execute().body()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Authenticator that automatically retrieves a new access token. This will at first attempt
     * to refresh [accessTokenInternal]. If the refresh fails, or this token has no refresh token, then
     * a new anonymous/non-logged in access token is retrieved. If this also fails, the request will
     * be cancelled.
     */
    private inner class Authenticator : okhttp3.Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // If we have an access token with a refresh token the token is for a logged in user and can be refreshed
            val newToken = if (accessTokenInternal.refreshToken != null) {
                // The response does not send a new refresh token, so make sure the old one is saved
                refreshToken()?.apply {
                    refreshToken = accessTokenInternal.refreshToken
                } ?: newNonLoggedInToken()
            } else {
                newNonLoggedInToken()
            }

            // New token received
            return if (newToken != null) {
                accessTokenInternal = newToken
                response.request().newBuilder()
                        .header("Authorization", newToken.generateHeaderString())
                        .build()
            } else {
                // No new token received, we can't do anything so cancel the request
                null
            }
        }

        /**
         * Synchronously refreshes the access token
         *
         * @return The new access token, or null if it couldn't be refreshed
         */
        private fun refreshToken() : AccessToken? {
            return try {
                val call = accessTokenApi.refreshTokenNoSuspend(
                        basicAuthHeader,
                        accessTokenInternal.refreshToken,
                ).execute()

                // If we get a 400 Bad Request when attempting to refresh the token, the token has been
                // invalidated outside of the control of the API (ie. the applications access from reddit.com/prefs/apps
                // was revoked), or the access token set was never valid
                // Call the listener registered when the API object was built to notify that the token isn't valid anymore
                val code = call.code()

                if (code == 400) {
                    onInvalidToken?.onFailure(
                            GenericError(code),
                            InvalidAccessTokenException("The access token couldn't be refreshed. Either the access token set when building the API object" +
                                    " was never valid, or the user has revoked the applications access to their account.")
                    )
                }

                call.body()?.apply {
                    refreshToken = accessTokenInternal.refreshToken
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }


    /**
     * Interceptor that ensures that an access token is set and added as a request header.
     *
     * If no token is found a new token for non-logged in users is retrieved
     */
    private inner class TokenInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val request = original.newBuilder()
            if (accessTokenInternal.accessToken == null) {
                newNonLoggedInToken()?.let {
                    accessTokenInternal = it
                }
            }

            request.header("Authorization", accessTokenInternal.generateHeaderString())
            return chain.proceed(request.build())
        }
    }


    /**
     * Interceptor that adds the "Authorization: basic " header to a request. This should be used for OAuth related
     * API calls that do not use the access token as the authorization header
     *
     * The interceptor adds the value found in [basicAuthHeader]
     */
    private inner class BasicAuthInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val request = original.newBuilder()
                    .header("Authorization", basicAuthHeader)
                    .build()
            return chain.proceed(request)
        }
    }
}