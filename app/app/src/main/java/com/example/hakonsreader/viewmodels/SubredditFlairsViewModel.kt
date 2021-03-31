package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.persistence.RedditFlairsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditFlairsRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for retrieving link/user flairs
 */
class SubredditFlairsViewModel(
        subredditName: String,
        flairType: FlairType,
        api: SubredditRequest,
        dao: RedditFlairsDao
) : ViewModel() {

    class Factory(
            private val subredditName: String,
            private val flairType: FlairType,
            private val api: SubredditRequest,
            private val dao: RedditFlairsDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SubredditFlairsViewModel(subredditName, flairType, api, dao) as T
        }
    }

    private val repo = SubredditFlairsRepository(subredditName, flairType, api, dao)

    val flairs = repo.getFlairs().asLiveData()
    val errors = repo.errors
    val isLoading = repo.isLoading

    /**
     * Refreshes flairs for the subreddit from the Reddit API
     *
     * @param force If set to false the flairs will only be loaded if they haven't already been
     * loaded from the API
     */
    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            repo.refresh(force)
        }
    }
}