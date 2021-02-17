package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class TrendingSubredditsViewModel : ViewModel() {
    // Trending subreddits are updated around 0pm est (I think), so it might be possible to do some
    // sort of optimization to only get once from the network if the last one was retrieved after that
    // Although the request is a very low request so it isn't like it's very data intensive

    private val _trendingSubreddits = MutableLiveData<TrendingSubreddits>()
    private val _onCountChange = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val trendingSubreddits: LiveData<TrendingSubreddits> = _trendingSubreddits
    val onCountchange: LiveData<Boolean> = _onCountChange
    val error: LiveData<ErrorWrapper> = _error

    /**
     * Loads the trending subreddits
     */
    fun loadSubreddits() {
        val api = App.get().api

        CoroutineScope(IO).launch {
            _onCountChange.postValue(true)

            when (val resp = api.subreditts().trending()) {
                is ApiResponse.Success -> {
                    _trendingSubreddits.postValue(resp.value)
                }
                is ApiResponse.Error -> {
                    _error.postValue(ErrorWrapper(resp.error, resp.throwable))
                }
            }
            _onCountChange.postValue(false)
        }
    }
}