package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.ImgurService
import com.example.hakonsreader.api.service.SubredditServiceKt
import com.example.hakonsreader.api.utils.Util
import com.example.hakonsreader.api.utils.apiError
import java.lang.Exception

class SubredditRequestKt(
        private val subredditName: String,
        private val accessToken: AccessToken,
        private val api: SubredditServiceKt,
        private val imgurApi: ImgurService?
        ) {

    private val imgurRequest: ImgurRequest = ImgurRequest(imgurApi)
    private val loadImgurAlbumsAsRedditGalleries: Boolean = imgurApi != null

    /**
     * Retrieve information about the subreddit
     *
     * OAuth scope required: *read*
     */
    suspend fun info() : ApiResponse<Subreddit> {
        if (RedditApi.STANDARD_SUBS.contains(subredditName)) {
            return ApiResponse.Error(GenericError(-1), NoSubredditInfoException("The subreddits: " + RedditApi.STANDARD_SUBS.toString() + " do not have any info to retrieve"))
        }

        val resp = api.getSubredditInfo(subredditName)
        val sub = resp.body()
        return try {
            if (sub != null) {
                ApiResponse.Success(sub)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
    /**
     *
     * Retrieves posts from the subreddit. The subreddits sorted here are by *hot*
     *
     * If an access token for a user is set posts are customized for the user
     *
     * No specific OAuth scope is required
     *
     * @param after The ID of the last post seen. Default value is an empty string (ie. no post seen)
     * @param count The amount of posts already retrieved. Default value is *0* (ie. no posts seen)
     *
     * @see posts
     */
    suspend fun posts(after: String = "", count: Int = 0) : ApiResponse<List<RedditPost>> {
        return getPosts("hot", after = after, count = count)
    }

    /**
     * Retrieve an object that can retrieve posts by different sorts
     */
    fun posts() : SubredditPostsRequest = SubredditPostsRequest()

    inner class SubredditPostsRequest {
        /**
         * Retrieves *controversial* posts from the subreddit
         *
         * If an access token for a user is set posts are customized for the user
         *
         * No specific OAuth scope is required
         *
         * @param timeSort The time sort for the posts to retireve
         * @param after The ID of the last post seen. Default value is an empty string (ie. no post seen)
         * @param count The amount of posts already retrieved. Default value is *0* (ie. no posts seen)
         */
        suspend fun controversial(timeSort: PostTimeSort, after: String = "", count: Int = 0) : ApiResponse<List<RedditPost>> {
            return getPosts("controversial", timeSort, after, count)
        }

        /**
         * Retrieves *top* posts from the subreddit
         *
         * If an access token for a user is set posts are customized for the user
         *
         * No specific OAuth scope is required
         *
         * @param timeSort The time sort for the posts to retireve
         * @param after The ID of the last post seen. Default value is an empty string (ie. no post seen)
         * @param count The amount of posts already retrieved. Default value is *0* (ie. no posts seen)
         */
        suspend fun top(timeSort: PostTimeSort, after: String, count: Int) : ApiResponse<List<RedditPost>> {
            return getPosts("top", timeSort, after, count)
        }

        /**
         * Retrieves *new* posts from the subreddit
         *
         * If an access token for a user is set posts are customized for the user
         *
         * No specific OAuth scope is required
         *
         * @param after The ID of the last post seen. Default value is an empty string (ie. no post seen)
         * @param count The amount of posts already retrieved. Default value is *0* (ie. no posts seen)
         */
        suspend fun newPosts(after: String = "", count: Int = 0) : ApiResponse<List<RedditPost>> {
            return getPosts("new", after = after, count = count)
        }
    }

    /**
     * Retrieves posts from the subreddit
     *
     * If an access token for a user is set posts are customized for the user
     *
     * No specific OAuth scope is required
     *
     * @param sort The sort for the posts (new, hot, top, or controversial)
     * @param timeSort How the posts should be time sorted. This only has an affect on top and controversial,
     *                 and can be set to null for new and hot
     * @param after The ID of the last post seen (or an empty string if first time loading)
     * @param count The amount of posts already retrieved (0 if first time loading)
     */
    private suspend fun getPosts(sort: String = "hot", timeSort: PostTimeSort? = null, after: String = "", count: Int = 0) : ApiResponse<List<RedditPost>> {
        // If not blank (ie. front page) add "r/" at the start
        val sub = if (subredditName.isBlank()) {
            ""
        } else {
            "r/$subredditName"
        }

        return try {
            val resp = api.getPosts(
                    sub,
                    sort,
                    if (timeSort == null) "" else timeSort.value,
                    after,
                    count
            )

            // If the subreddit doesn't exist, Reddit wants to be helpful (or something) and redirects
            // the response to a search request instead. This causes issues as the response being sent back
            // now holds subreddits instead of posts, so if we have a prior request (which is the actual original request)
            // then call the failure handler as the user of the API might want to know that the sub doesn't exist
            // Additionally, if the search request returned subreddits, body.getListings() will hold a List<Subreddit> which will cause issues
            // This is also an "issue" for SubredditRequest.info(), but it will manage to convert that to a RedditListing
            // and the check for getId() will return null, so it doesn't have to be handled directly
            // We could disable redirects, but I'm afraid of what issues that would cause later
            val prior = resp.raw().priorResponse()
            if (prior != null) {

                // If the prior response was a redirect then we exit. "prior" can be a 401 Unauthorized because the
                // access token was invalid and automatically refreshed, so if that happens we don't want to exit
                // as the new response will hold the posts and be correct
                val code = prior.code()
                if (code in 300..399) {
                    return ApiResponse.Error(GenericError(code), SubredditNotFoundException("No subreddit found with name: $subredditName"))
                }
            }

            val posts = resp.body()?.getListings()

            if (!posts.isNullOrEmpty()) {
                if (loadImgurAlbumsAsRedditGalleries) {
                    imgurRequest.loadAlbums(posts)
                }

                ApiResponse.Success(posts)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Subscribe or unsubscribe to the subreddit
     *
     * OAuth scope required: *subscribe*
     *
     * @param subscribe True to subscribe, false to unsubscribe
     * @return If successful no value is returned, so Success will hold *null*
     */
    suspend fun subscribe(subscribe: Boolean) : ApiResponse<Nothing?> {
        try {
            Util.verifyLoggedInToken(accessToken)
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
                ApiResponse.Success(null)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Favorite or un-favorite a subreddit
     *
     * @param favorite True if the action should be to favorite, false to un-favorite
     * @return If successful no value is returned, so Success will hold *null*
     */
    suspend fun favorite(favorite: Boolean) : ApiResponse<Nothing?> {
        try {
            Util.verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), e)
        }

        return try {
            val resp = api.favoriteSubreddit(favorite, subredditName)

            if (resp.isSuccessful) {
                ApiResponse.Success(null)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}