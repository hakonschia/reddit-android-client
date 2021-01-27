package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditRepository

/**
 * ViewModel for observing a [Subreddit]
 */
class SubredditViewModel(
        subredditName: String,
        api: SubredditRequest,
        dao: RedditSubredditsDao,
        postsDao: RedditPostsDao
) : ViewModel() {
    private val repo = SubredditRepository(subredditName, api, dao, postsDao)

    val subreddit = repo.getSubreddit().asLiveData()
    val errors = repo.errors
    val loading = repo.loading

    /**
     * Update the subscription on a subreddit
     *
     * [Subreddit.isSubscribed] will be flipped, and the subscribe count will be updated. This assumes
     * success, which means the values will be updated right away, and if the request fails will be
     * reverted back
     */
    suspend fun subscribe() {
        repo.subscribe()
    }

    suspend fun updateFlair(username: String, flair: RedditFlair?) {
        repo.updateFlair(username, flair)
    }
}