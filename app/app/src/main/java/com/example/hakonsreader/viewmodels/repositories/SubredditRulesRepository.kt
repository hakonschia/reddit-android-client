package com.example.hakonsreader.viewmodels.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hakonsreader.api.model.SubredditRule
import com.example.hakonsreader.api.persistence.RedditSubredditRulesDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.viewmodels.ErrorWrapper
import kotlinx.coroutines.flow.Flow

class SubredditRulesRepository(
        private val subredditName: String,
        private val api: SubredditRequest,
        private val rulesDao: RedditSubredditRulesDao
) {

    private var rulesLoaded = false

    private val _errors = MutableLiveData<ErrorWrapper>()
    private val _loading = MutableLiveData<Boolean>()

    val errors = _errors as LiveData<ErrorWrapper>
    val loading = _loading as LiveData<Boolean>

    /**
     * Gets a Flow of the list of rules for the given subreddit
     */
     fun getRules() : Flow<List<SubredditRule>> {
        return rulesDao.getAllRules(subredditName)
     }

    /**
     * Refreshes rules for the subreddit from the Reddit API
     *
     * @param force If set to false the rules will only be loaded if they haven't already been
     * loaded from the API
     */
    suspend fun refresh(force: Boolean = false) {
        if (!rulesLoaded || force) {
            _loading.postValue(true)

            when (val resp = api.rules()) {
                is ApiResponse.Success -> {
                    rulesLoaded = true
                    rulesDao.insertAll(resp.value)
                }
                is ApiResponse.Error -> {
                    _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
                }
            }
            _loading.postValue(false)
        }
    }

}