package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for retrieving trending subreddits
 */
@HiltViewModel
class TrendingSubredditsViewModel @Inject constructor(
        private val api: RedditApi
) : ViewModel() {
    // Trending subreddits are updated around 0pm est (I think), so it might be possible to do some
    // sort of optimization to only get once from the network if the last one was retrieved after that
    // Although the request is a very low request so it isn't like it's very data intensive

    private val _trendingSubreddits = MutableLiveData<TrendingSubreddits>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val trendingSubreddits: LiveData<TrendingSubreddits> = _trendingSubreddits
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * Loads the trending subreddits
     */
    fun loadSubreddits() {
        viewModelScope.launch {
            _isLoading.postValue(true)

            when (val resp = api.subreditts().trending()) {
                is ApiResponse.Success -> {
                    _trendingSubreddits.postValue(resp.value!!)
                }
                is ApiResponse.Error -> {
                    _error.postValue(ErrorWrapper(resp.error, resp.throwable))
                }
            }
            _isLoading.postValue(false)
        }
    }
}