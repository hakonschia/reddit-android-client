package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.SubredditsService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception

class SubredditsRequest(
        private val accessToken: AccessToken,
        private val api: SubredditsService
) {

    /**
     * Retrieves a list of subreddits
     *
     * The subreddits retrieved here are either the logged in users subscribed subreddits, or the
     * default subreddits if no user is logged in
     *
     * OAuth scopes required:
     * 1. For default subreddits: *read*
     * 2. For subscribed subreddits: *mysubreddits*
     *
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param count The count of subreddits previously loaded. Default to 0
     *
     * @see subscribedSubreddits
     * @see defaultSubreddits
     */
    suspend fun getSubreddits(after: String = "", count: Int = 0) : ApiResponse<List<Subreddit>> {
        return try {
            subscribedSubreddits(after, count)
        } catch (e: InvalidAccessTokenException) {
            defaultSubreddits(after, count)
        }
    }


    /**
     * Retrieves a list of subreddits that a logged in user is subscribed to
     *
     * OAuth scope required: *mysubreddits*
     *
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param count The count of subreddits previously loaded. Default to 0
     *
     * @see defaultSubreddits
     * @see getSubreddits
     */
    suspend fun subscribedSubreddits(after: String = "", count: Int = 0) : ApiResponse<List<Subreddit>> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(GenericError(-1), InvalidAccessTokenException("Getting subscribed subreddits requires a valid access token for a logged in user", e))
        }

        return try {
            val resp = api.getSubscribedSubreddits(after, count, 100)
            val list = resp.body()?.getListings()

            if (!list.isNullOrEmpty()) {
                ApiResponse.Success(list)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Retrieves the list of default subreddits
     *
     * OAauth scope required: *read*
     *
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param count The count of subreddits previously loaded. Default to 0
     *
     * @see subscribedSubreddits
     * @see getSubreddits
     */
    suspend fun defaultSubreddits(after: String = "", count: Int = 0) : ApiResponse<List<Subreddit>> {
        return try {
            val resp = api.getDefaultSubreddits(after, count, 100)
            val list = resp.body()?.getListings()

            if (!list.isNullOrEmpty()) {
                ApiResponse.Success(list)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }

    /**
     * Search for subreddits
     *
     * OAuth scope required: *read*
     *
     * @param query The search query
     * @param includeNsfw If set to *true* NSFW search results will be included. Default value is *true*
     * @return
     */
    suspend fun search(query: String, includeNsfw: Boolean = true) : ApiResponse<List<Subreddit>> {
        return try {
            val resp = api.search(query, includeNsfw)
            val subreddits = resp.body()?.getListings()

            if (subreddits != null) {
                ApiResponse.Success(subreddits)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }


    /**
     * Gets todays trending subreddits
     */
    suspend fun trending() : ApiResponse<TrendingSubreddits> {
        return try {
            val resp = api.getTrendingSubreddits()
            val body = resp.body()

            if (body != null) {
                ApiResponse.Success(body)
            } else {
                apiError(resp)
            }
        } catch (e: Exception) {
            ApiResponse.Error(GenericError(-1), e)
        }
    }
}