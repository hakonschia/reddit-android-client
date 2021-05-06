package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.responses.ListingResponse
import com.example.hakonsreader.api.service.SubredditsService
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.verifyLoggedInToken
import java.lang.Exception


/**
 * Interface for communicating with a group of subreddits
 */
interface SubredditsRequest {

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
     * @param fetchAll If true then all subreddits will be fetched, otherwise a max of 100. Note that this
     * will potentially trigger multiple API calls as Reddit puts a hard limit on 100 for each request.
     *
     * @see subscribedSubreddits
     * @see defaultSubreddits
     */
    suspend fun getSubreddits(after: String = "", fetchAll: Boolean = true) : ApiResponse<List<Subreddit>>

    /**
     * Retrieves a list of subreddits that a logged in user is subscribed to
     *
     * OAuth scope required: *mysubreddits*
     *
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param fetchAll If true then all subreddits will be fetched, otherwise a max of 100. Note that this
     * will potentially trigger multiple API calls as Reddit puts a hard limit on 100 for each request.
     *
     * @see defaultSubreddits
     * @see getSubreddits
     */
    suspend fun subscribedSubreddits(after: String = "", fetchAll: Boolean = true) : ApiResponse<List<Subreddit>>

    /**
     * Retrieves the list of default subreddits
     *
     * OAauth scope required: *read*
     *
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param fetchAll If true then all subreddits will be fetched, otherwise a max of 100. Note that this
     * will potentially trigger multiple API calls as Reddit puts a hard limit on 100 for each request.
     *
     * @see subscribedSubreddits
     * @see getSubreddits
     */
    suspend fun defaultSubreddits(after: String = "", fetchAll: Boolean = true) : ApiResponse<List<Subreddit>>

    /**
     * Search for subreddits
     *
     * OAuth scope required: *read*
     *
     * @param query The search query
     * @param includeNsfw If set to *true* NSFW search results will be included. Default value is *true*
     * @return
     */
    suspend fun search(query: String, includeNsfw: Boolean = true) : ApiResponse<List<Subreddit>>

    /**
     * Gets todays trending subreddits
     */
    suspend fun trending() : ApiResponse<TrendingSubreddits>
}

/**
 * Standard [SubredditsRequest] implementation
 */
class SubredditsRequestImpl(
        private val accessToken: AccessToken,
        private val api: SubredditsService
) : SubredditsRequest {

    override suspend fun getSubreddits(after: String, fetchAll: Boolean) : ApiResponse<List<Subreddit>> {
        return try {
            subscribedSubreddits(after, fetchAll)
        } catch (e: InvalidAccessTokenException) {
            defaultSubreddits(after, fetchAll)
        }
    }


    override suspend fun subscribedSubreddits(after: String, fetchAll: Boolean) : ApiResponse<List<Subreddit>> {
        try {
            verifyLoggedInToken(accessToken)
        } catch (e: InvalidAccessTokenException) {
            return ApiResponse.Error(
                GenericError(-1),
                InvalidAccessTokenException("Getting subscribed subreddits requires a valid access token for a logged in user", e)
            )
        }

        return getSubsInternal(after, fetchAll, getSubscribed = true)
    }

    override suspend fun defaultSubreddits(after: String, fetchAll: Boolean) : ApiResponse<List<Subreddit>> {
        return getSubsInternal(after, fetchAll, getSubscribed = false)
    }

    private suspend fun getSubsInternal(
        after: String,
        fetchAll: Boolean,
        getSubscribed: Boolean
    ): ApiResponse<List<Subreddit>> {
        val subs: MutableList<Subreddit> = ArrayList()

        var lastResponse: ListingResponse<Subreddit>? = null

        do {
            val aft = try {
                val resp = if (getSubscribed) {
                    api.getSubscribedSubreddits(
                        lastResponse?.getAfter() ?: after,
                        count = subs.size,
                        limit = 10
                    ) } else {
                    api.getDefaultSubreddits(
                        lastResponse?.getAfter() ?: after,
                        count = subs.size,
                        limit = 100
                    )
                }
                lastResponse = resp.body()

                if (lastResponse != null) {
                    lastResponse.getListings()?.let { subs.addAll(it) }
                    lastResponse.getAfter()
                } else {
                    return apiError(resp)
                }
            } catch (e: Exception) {
                return ApiResponse.Error(GenericError(-1), e)
            }
        // "aft" will be null if there are no more subs to retrieve
        } while (!aft.isNullOrEmpty() && fetchAll)

        return ApiResponse.Success(subs)
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
    override suspend fun search(query: String, includeNsfw: Boolean) : ApiResponse<List<Subreddit>> {
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
    override suspend fun trending() : ApiResponse<TrendingSubreddits> {
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