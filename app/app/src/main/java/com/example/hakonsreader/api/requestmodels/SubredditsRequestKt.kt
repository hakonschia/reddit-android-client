package com.example.hakonsreader.api.requestmodels

import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.api.service.SubredditsServiceKt
import com.example.hakonsreader.api.utils.apiError
import com.example.hakonsreader.api.utils.Util
import java.lang.Exception

class SubredditsRequestKt(private val accessToken: AccessToken, private val api: SubredditsServiceKt) {

    /**
     * Retrieves a list of subreddits
     * The subreddits retrieved here are either the logged in users subscribed subreddits, or the
     * default subreddits if no user is logged in
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
     * @param after For loading more subreddits, this is the ID of the last subreddit loaded. The new
     * subreddits will be loaded after this. Default to an empty string
     * @param count The count of subreddits previously loaded. Default to 0
     *
     * @see defaultSubreddits
     * @see getSubreddits
     */
    suspend fun subscribedSubreddits(after: String = "", count: Int = 0) : ApiResponse<List<Subreddit>> {
        try {
            Util.verifyLoggedInToken(accessToken)
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
     * Retrieves a list of subreddits
     * The subreddits retrieved here are either the logged in users subscribed subreddits, or the
     * default subreddits if no user is logged in
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
}