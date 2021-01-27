package com.example.hakonsreader.viewmodels.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.viewmodels.ErrorWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SubredditRepository(
        private val subredditName: String,
        private val api: SubredditRequest,
        private val dao: RedditSubredditsDao
) {

    private var infoLoaded = false

    private val _errors = MutableLiveData<ErrorWrapper>()
    private val _loading = MutableLiveData<Boolean>()

    val errors = _errors as LiveData<ErrorWrapper>
    val loading = _loading as LiveData<Boolean>

    fun getSubreddit() : Flow<Subreddit?> {
        if (!infoLoaded) {
            CoroutineScope(IO).launch {
                refresh()
            }
        }
        return dao.getFlow(subredditName)
    }

    suspend fun refresh() {
        _loading.postValue(true)

        when (val resp = api.info()) {
            is ApiResponse.Success -> {
                infoLoaded = true
                dao.update(resp.value)
            }
            is ApiResponse.Error -> {
                _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }

        _loading.postValue(false)
    }
}