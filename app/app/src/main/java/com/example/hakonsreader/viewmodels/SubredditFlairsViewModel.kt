package com.example.hakonsreader.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.persistence.RedditFlairsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.viewmodels.repositories.SubredditFlairsRepository

/**
 * ViewModel for retrieving link/user flairs
 */
class SubredditFlairsViewModel(
        subredditName: String,
        flairType: FlairType,
        api: SubredditRequest,
        dao: RedditFlairsDao
) : ViewModel() {
    private val repo = SubredditFlairsRepository(subredditName, flairType, api, dao)

    val flairs = repo.getFlairs().asLiveData()
    val errors = repo.errors
    val loading = repo.loading

    /**
     * Refreshes flairs for the subreddit from the Reddit API
     *
     * @param force If set to false the flairs will only be loaded if they haven't already been
     * loaded from the API
     */
    suspend fun refresh(force: Boolean = false) {
        repo.refresh(force)
    }
}