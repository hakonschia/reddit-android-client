package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditRepository

/**
 * ViewModel for observing a [Subreddit]
 */
class SubredditViewModel(
        subredditName: String,
        api: SubredditRequest,
        dao: RedditSubredditsDao
) : ViewModel() {
    private val repo = SubredditRepository(subredditName, api, dao)

    val subreddit = repo.getSubreddit().asLiveData()
    val errors = repo.errors
    val loading = repo.loading
}