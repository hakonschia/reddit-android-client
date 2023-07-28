package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for observing a [Subreddit]
 */
class SubredditViewModel(
        subredditName: String,
        api: RedditApi,
        dao: RedditSubredditsDao,
        postsDao: RedditPostsDao,
) : ViewModel() {

    class Factory(
            private val subredditName: String,
            private val api: RedditApi,
            private val dao: RedditSubredditsDao,
            private val postsDao: RedditPostsDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SubredditViewModel(subredditName, api, dao, postsDao) as T
        }
    }

    private val repo = SubredditRepository(subredditName, api, dao, postsDao)

    val subreddit = repo.getSubreddit()
    val errors = repo.errors
    val isLoading = repo.isLoading

    /**
     * Update the subscription on a subreddit
     *
     * [Subreddit.isSubscribed] will be flipped, and the subscribe count will be updated. This assumes
     * success, which means the values will be updated right away, and if the request fails will be
     * reverted back
     */
    fun subscribe() {
        viewModelScope.launch {
            repo.subscribe()
        }
    }

    fun updateFlair(username: String, flair: RedditFlair?) {
        viewModelScope.launch {
            repo.updateFlair(username, flair)
        }
    }
}