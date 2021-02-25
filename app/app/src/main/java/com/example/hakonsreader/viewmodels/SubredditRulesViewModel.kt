package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.hakonsreader.api.persistence.RedditSubredditRulesDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditRulesRepository

class SubredditRulesViewModel(
        subredditName: String,
        api: SubredditRequest,
        dao: RedditSubredditRulesDao
) : ViewModel() {
    private val repo = SubredditRulesRepository(subredditName, api, dao)

    val rules = repo.getRules().asLiveData()
    val errors = repo.errors
    val isLoading = repo.isLoading

    /**
     * Refreshes rules for the subreddit from the Reddit API
     *
     * @param force If set to false the rules will only be loaded if they haven't already been
     * loaded from the API
     */
    suspend fun refresh(force: Boolean = false) {
        repo.refresh(force)
    }
}