package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.model.thirdparty.ThirdPartyOptions
import com.example.hakonsreader.api.requestmodels.thirdparty.ThirdPartyRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.thirdparty.GfycatService
import com.example.hakonsreader.api.service.SubredditService
import com.example.hakonsreader.api.service.thirdparty.ImgurService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.apiListingErrors
import com.example.hakonsreader.api.utils.createFullName
import com.example.hakonsreader.api.utils.verifyLoggedInToken


/**
 * Interface for communicating with a subreddit
 */
interface SubredditRequest {

    /**
     * Retrieve information about the subreddit
     *
     * OAuth scope required: *read*
     */
    suspend fun info() : ApiResponse<Subreddit>

    /**
     * Retrieve subreddit rules
     *
     * OAuth scope required: *read*
     */
    suspend fun rules() : ApiResponse<List<SubredditRule>>

    /**
     * Retrieves posts from the subreddit
     *
     * If an access token for a user is set posts are customized for the user
     *
     * OAuth scope required: *read*
     *
     * @param postSort sort for the posts (new, hot, top, or controversial). Default is [SortingMethods.HOT]
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial.
     * Default is [PostTimeSort.DAY]
     * @param after The ID of the last post seen. Default is an empty string (ie. no last post)
     * @param count The amount of posts already retrieved. Default is *0* (ie. no posts already)
     * @param limit The amount of posts to retrieve. Default is *25*
     */
    suspend fun posts(postSort: SortingMethods = SortingMethods.HOT, timeSort: PostTimeSort = PostTimeSort.DAY, after: String = "", count: Int = 0, limit: Int = 25) : ApiResponse<List<RedditPost>>

    /**
     * Subscribe or unsubscribe to the subreddit
     *
     * OAuth scope required: *subscribe*
     *
     * @param subscribe True to subscribe, false to unsubscribe
     * @return If successful no value is returned
     */
    suspend fun subscribe(subscribe: Boolean) : ApiResponse<Unit>

    /**
     * Favorite or un-favorite a subreddit
     *
     * @param favorite True if the action should be to favorite, false to un-favorite
     * @return If successful no value is returned
     */
    suspend fun favorite(favorite: Boolean) : ApiResponse<Unit>

    /**
     * Submit a text post to the subreddit
     *
     * OAuth scope required: *submit*
     *
     * @param title The title of the post. Max characters is 300
     * @param text The text of the post. Can be omitted for title-only posts
     * @param nsfw True if the post should be marked as NSFW (18+). Default to *false*
     * @param spoiler True if the post should be marked as a spoiler. Default to *false*
     * @param receiveNotifications True if the user wants to receive notifications from the post. Default to *true*
     * @param flairId The ID of the flair to submit as the link flair for the post. Should be the the value
     * retrieved with [RedditFlair.id]. Default is an empty string (ie. no flair)
     *
     * @return An [ApiResponse] holding a [Submission] of the newly created post
     */
    suspend fun submitTextPost(
            title: String,
            text: String = "",

            nsfw: Boolean = false,
            spoiler: Boolean = false,
            receiveNotifications: Boolean = true,

            flairId: String = ""
    ) : ApiResponse<Submission>

    /**
     * Submit a link post to the subreddit
     *
     * OAuth scope required: *submit*
     *
     * @param title The title of the post. Max characters is 300
     * @param link The link the post is referencing. This should be a valid URL, verification is not done
     * by the API. Spaces in the link will be converted to *%20*
     * @param nsfw True if the post should be marked as NSFW (18+). Default to *false*
     * @param spoiler True if the post should be marked as a spoiler. Default to *false*
     * @param receiveNotifications True if the user wants to receive notifications from the post. Default to *true*
     * @param flairId The ID of the flair to submit as the link flair for the post. Should be the the value
     * retrieved with [RedditFlair.id]. Default is an empty string (ie. no flair)
     *
     * @return An [ApiResponse] holding a [Submission] of the newly created post
     */
    suspend fun submitLinkPost(
            title: String,
            link: String,

            nsfw: Boolean = false,
            spoiler: Boolean = false,
            receiveNotifications: Boolean = true,

            flairId: String = ""
    ) : ApiResponse<Submission>

    /**
     * Submit a text post to the subreddit
     *
     * OAuth scope required: *submit*
     *
     * @param title The title of the post. Max characters is 300
     * @param crosspostId The ID of the post this post is crossposting
     * @param nsfw True if the post should be marked as NSFW (18+). Default to *false*
     * @param spoiler True if the post should be marked as a spoiler. Default to *false*
     * @param receiveNotifications True if the user wants to receive notifications from the post. Default to *true*
     * @param flairId The ID of the flair to submit as the link flair for the post. Should be the the value
     * retrieved with [RedditFlair.id]. Default is an empty string (ie. no flair)
     *
     * @return An [ApiResponse] holding a [Submission] of the newly created post
     */
    suspend fun submitCrosspost(
            title: String,
            crosspostId: String,

            nsfw: Boolean = false,
            spoiler: Boolean = false,
            receiveNotifications: Boolean = true,

            flairId: String = ""
    ) : ApiResponse<Submission>

    /**
     * Gets the submission flairs for the subreddit
     *
     * OAuth scope required: *flair*
     *
     * If the subreddit doesn't allow submission flairs, a 403 Forbidden error is returned
     */
    suspend fun submissionFlairs() : ApiResponse<List<RedditFlair>>

    /**
     * Gets the user flairs for the subreddit
     *
     * OAuth scope required: *flair*
     *
     * If the subreddit doesn't allow submission flairs, a 403 Forbidden error is returned
     */
    suspend fun userFlairs() : ApiResponse<List<RedditFlair>>

    /**
     * Select a flair for a user on the subreddit. If [flairId] is not called with `null`, then
     * [enableUserFlair] is called with `true` automatically
     *
     * OAuth scope required: *flair*
     *
     * @param username The username to select a flair for
     * @param flairId The ID of the flair to select (as from [RedditFlair.id]), or `null` to clear
     * the users flair
     */
    suspend fun selectFlair(username: String, flairId: String?) : ApiResponse<Unit>

    /**
     * Enables or disables user flairs on the subreddit
     *
     * @param enable True to enable flairs, false to disable
     */
    suspend fun enableUserFlair(enable: Boolean) : ApiResponse<Unit>

    /**
     * Gets a wiki page for the subreddit
     *
     * OAuth scope required: `wikiread`
     *
     * @param page The name of the page to retrieve. Default to `index` (the start of the wiki)
     */
    suspend fun wiki(page: String = "index") : ApiResponse<SubredditWikiPage>
}

/**
 * Standard [SubredditRequest] implementation
 */
class SubredditRequestImpl(
        private val subredditName: String,
        private val accessToken: AccessToken,
        private val api: SubredditService,
        imgurApi: ImgurService?,
        gfycatApi: GfycatService,
        thirdPartyOptions: ThirdPartyOptions
) : SubredditRequest {

    private val thirdPartyRequest = ThirdPartyRequest(imgurApi, gfycatApi, thirdPartyOptions)

    override suspend fun info() : ApiResponse<Subreddit> {
        if (RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())) {
            return ApiResponse.Error(GenericError(-1), NoSubredditInfoException("The subreddits: " + RedditApi.STANDARD_SUBS.toString() + " do not have any info to retrieve"))
        }
        return try {
            val resp = api.getSubredditInfo(subredditName)
            val sub = resp.body()

            if (sub != null) {
                sub as Subreddit

                // If there is no name, the subreddit wasn't found (this happens on redirects since the subreddit wasn't found
                // and the cast is successful, but the object is empty with default values)
                if (sub.name.isNotBlank()) {
                    ApiResponse.Success(sub)
                } else {
                    ApiResponse.Error(GenericError(-1), SubredditNotFoundException("The subreddit '$subredditName' was not found"))
                }
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun rules() : ApiResponse<List<SubredditRule>>  {
        return try {
            val resp = api.getRules(subredditName)
            val rules = resp.body()?.rules

            if (rules != null) {
                // Rules aren't connected automatically to its subreddit
                rules.forEach { it.subreddit = subredditName }

                ApiResponse.Success(rules)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun posts(postSort: SortingMethods, timeSort: PostTimeSort, after: String, count: Int, limit: Int) : ApiResponse<List<RedditPost>> {
        // If not blank (ie. front page) add "r/" at the start
        val sub = if (subredditName.isBlank()) {
            ""
        } else {
            "r/$subredditName"
        }

        return try {
            val resp = api.getPosts(
                    sub,
                    postSort.value,
                    timeSort.value,
                    after,
                    count,
                    limit
            )

            // If the subreddit doesn't exist, Reddit wants to be helpful (or something) and redirects
            // the response to a search request instead. This causes issues as the response being sent back
            // now holds subreddits instead of posts, so if we have a prior request (which is the actual original request)
            // then call the failure handler as the user of the API might want to know that the sub doesn't exist
            // Additionally, if the search request returned subreddits, body.getListings() will hold a List<Subreddit> which will cause issues
            // This is also an "issue" for SubredditRequest.info(), but it will manage to convert that to a RedditListing
            // and the check for getId() will return null, so it doesn't have to be handled directly
            // We could disable redirects, but I'm afraid of what issues that would cause later (edit: god prediction tbh)

            // Some redirects are on purpose, such as going to "r/random" which will redirect to a random subreddit
            val prior = resp.raw().priorResponse()
            if (prior != null) {

                // If the prior response was a redirect then we exit. "prior" can be a 401 Unauthorized because the
                // access token was invalid and automatically refreshed, so if that happens we don't want to exit
                // as the new response will hold the posts and be correct
                val code = prior.code()
                if (code in 300..399) {
                    // If the redirect is because it doesn't exist return, otherwise we can continue
                    val pathSegments = resp.raw().request().url().pathSegments()
                    if (pathSegments.size >= 2 && pathSegments[0] == "subreddits" && pathSegments[1] == "search") {
                        return ApiResponse.Error(GenericError(code), SubredditNotFoundException("No subreddit found with name: $subredditName"))
                    }
                }
            }

            val posts = resp.body()?.getListings()

            if (posts != null) {
                thirdPartyRequest.loadAll(posts)
                ApiResponse.Success(posts)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    override suspend fun subscribe(subscribe: Boolean) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), e)
        }

        val subAction = if (subscribe) {
            "sub"
        } else {
            "unsub"
        }

        return try {
            val resp = api.subscribeToSubreddit(subAction, subredditName)

            if (resp.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun favorite(favorite: Boolean) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), e)
        }

        return try {
            val resp = api.favoriteSubreddit(favorite, subredditName)

            if (resp.isSuccessful) {
                ApiResponse.Success(Unit)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    // TODO possible submit errors:
    //  {"json": {"errors": [["BAD_SR_NAME", "det navnet kommer ikke til \u00e5 virke", "sr"]]}}

    // TODO submit response: {"json": {"errors": [], "data": {"url": "https://www.reddit.com/r/hakonschia/comments/k1yj0s/hello_reddit/", "drafts_count": 0, "id": "k1yj0s", "name": "t3_k1yj0s"}}}
    //  can probably just return the ID of the post

    override suspend fun submitTextPost(
            title: String,
            text: String,

            nsfw: Boolean,
            spoiler: Boolean,
            receiveNotifications: Boolean,

            flairId: String
    ) : ApiResponse<Submission> {
        val submissionError = verifyGenericSubmission(title)
        if (submissionError != null) {
            return submissionError
        }

        return try {
            val resp = api.submit(
                    subredditName,
                    kind = "self",
                    title = title,
                    text = text,
                    nsfw = nsfw,
                    spoiler = spoiler,
                    sendNotifications = receiveNotifications,
                    flairId = flairId
            )
            val body = resp.body()?.getListing()

            if (body != null) {
                ApiResponse.Success(body)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun submitLinkPost(
            title: String,
            link: String,

            nsfw: Boolean,
            spoiler: Boolean,
            receiveNotifications: Boolean,

            flairId: String
    ) : ApiResponse<Submission> {
        val submissionError = verifyGenericSubmission(title)
        if (submissionError != null) {
            return submissionError
        }

        val linkSpaceConverted = link.replace(" ", "%20")

        return try {
            val resp = api.submit(
                    subredditName,
                    kind = "link",
                    title = title,
                    link = linkSpaceConverted,
                    nsfw = nsfw,
                    spoiler = spoiler,
                    sendNotifications = receiveNotifications,
                    flairId = flairId
            )
            val body = resp.body()?.getListing()

            if (body != null) {
                ApiResponse.Success(body)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun submitCrosspost(
            title: String,
            crosspostId: String,

            nsfw: Boolean,
            spoiler: Boolean,
            receiveNotifications: Boolean,

            flairId: String
    ) : ApiResponse<Submission> {
        val submissionError = verifyGenericSubmission(title)
        if (submissionError != null) {
            return submissionError
        }

        // kind = crosspost
        // "crosspost_fullname"
        val fullname = createFullName(Thing.POST, crosspostId)

        return try {
            val resp = api.submit(
                    subredditName,
                    kind = "crosspost",
                    title = title,
                    crosspostFullname = fullname,
                    nsfw = nsfw,
                    spoiler = spoiler,
                    sendNotifications = receiveNotifications,
                    flairId = flairId
            )
            val body = resp.body()?.getListing()

            if (body != null) {
                ApiResponse.Success(body)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }

        // TODO possible error:
        //  {"json": {"errors": [["INVALID_CROSSPOST_THING", "that isn't a valid crosspost url", "crosspost_thing"]]}}
    }

    private fun verifyGenericSubmission(title: String) : ApiResponse.Error? {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Submitting a post requires a valid access token for a logged in user", e))
        }

        if (title.length > 300) {
            return ApiResponse.Error(GenericError(-1), IllegalStateException("Post titles cannot be longer than 300 characters"))
        } else if (title.isBlank()) {
            return ApiResponse.Error(GenericError(-1), IllegalStateException("Post titles cannot be empty"))
        }

        return null
    }


    override suspend fun submissionFlairs() : ApiResponse<List<RedditFlair>> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Retrieving submission flairs requires a valid access token for a logged in user", e))
        }

        return try {
            val response = api.getLinkFlairs(subredditName)
            val flairs = response.body()

            if (flairs != null) {
                flairs.forEach {
                    it.subreddit = subredditName
                    it.flairType = FlairType.SUBMISSION
                }
                ApiResponse.Success(flairs)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun userFlairs() : ApiResponse<List<RedditFlair>> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Retrieving user flairs requires a valid access token for a logged in user", e))
        }

        return try {
            val response = api.getUserFlairs(subredditName)
            val flairs = response.body()

            if (flairs != null) {
                flairs.forEach {
                    it.subreddit = subredditName
                    it.flairType = FlairType.USER
                }
                ApiResponse.Success(flairs)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun selectFlair(username: String, flairId: String?) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Setting a flair requires a valid access token for a logged in user", e))
        }

        return try {
            val response = api.selectFlair(subredditName, username, flairId)
            val body = response.body()

            if (body != null) {
                if (!body.hasErrors()) {
                    if (flairId != null) {
                        enableUserFlair(true)
                    }
                    ApiResponse.Success(Unit)
                } else {
                    apiListingErrors(body.errors() as List<List<String>>)
                }
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun enableUserFlair(enable: Boolean) : ApiResponse<Unit> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Enabling flairs requires a valid access token for a logged in user", e))
        }

        return try {
            val response = api.enableUserFlair(subredditName, enable)
            val body = response.body()

            if (body != null) {
                if (!body.hasErrors()) {
                    ApiResponse.Success(Unit)
                } else {
                    apiListingErrors(body.errors() as List<List<String>>)
                }
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    override suspend fun wiki(page: String) : ApiResponse<SubredditWikiPage> {
        return try {
            val response = api.getWikiPage(subredditName, page)
            val body = response.body()?.data
            if (body != null) {
                body.subreddit = subredditName
                ApiResponse.Success(body)
            } else {
                apiError(response)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}