package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.persistence.RedditSubredditRulesDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditRulesRepository
import kotlinx.coroutines.launch

class SubredditRulesViewModel(
        subredditName: String,
        api: SubredditRequest,
        dao: RedditSubredditRulesDao
) : ViewModel() {

    class Factory(
            private val subredditName: String,
            private val api: SubredditRequest,
            private val rulesDao: RedditSubredditRulesDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SubredditRulesViewModel(subredditName, api, rulesDao) as T
        }
    }

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
    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            repo.refresh(force)
        }
    }
}