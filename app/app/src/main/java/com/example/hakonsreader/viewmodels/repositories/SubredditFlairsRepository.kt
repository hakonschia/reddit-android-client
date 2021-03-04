package com.example.hakonsreader.viewmodels.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.persistence.RedditFlairsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.viewmodels.ErrorWrapper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for a list of [RedditFlair]
 */
class SubredditFlairsRepository(
        private val subredditName: String,
        private val flairType: FlairType,
        private val api: SubredditRequest,
        private val dao: RedditFlairsDao
) {

    private var flairsLoaded = false

    private val _errors = MutableLiveData<ErrorWrapper>()
    private val _isLoading = MutableLiveData<Boolean>()

    val errors = _errors as LiveData<ErrorWrapper>
    val isLoading = _isLoading as LiveData<Boolean>

    fun getFlairs(): Flow<List<RedditFlair>> {
        return dao.getFlairsBySubredditAndType(subredditName, flairType.name)
    }

    /**
     * Refreshes flairs for the subreddit from the Reddit API
     *
     * @param force If set to false the flairs will only be loaded if they haven't already been
     * loaded from the API
     */
    suspend fun refresh(force: Boolean = false) {
        if (!flairsLoaded || force) {
            _isLoading.postValue(true)

            val response = when (flairType) {
                FlairType.SUBMISSION -> api.submissionFlairs()
                FlairType.USER -> api.userFlairs()
            }

            when (response) {
                is ApiResponse.Success -> {
                    flairsLoaded = true
                    withContext(IO) {
                        dao.insert(response.value)
                    }
                }
                is ApiResponse.Error -> {
                    _errors.postValue(ErrorWrapper(response.error, response.throwable))
                }
            }

            _isLoading.postValue(false)
        }
    }
}