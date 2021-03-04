package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SearchForSubredditsViewModel : ViewModel() {

    private val api = App.get().api
    private val _searchResults = MutableLiveData<List<Subreddit>>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    /**
     * Map containing the previous search results, mapping a search query to a list of subreddits
     */
    private val cachedSearchResults = HashMap<String, List<Subreddit>>()

    val searchResults : LiveData<List<Subreddit>> = _searchResults
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error


    /**
     * Search for subreddits
     *
     * @param query The search query
     */
    fun search(query: String) {
        if (cachedSearchResults.containsKey(query)) {
            _searchResults.postValue(cachedSearchResults[query])
            return
        }

        _isLoading.postValue(true)

        viewModelScope.launch {
            val resp = api.subreditts().search(query)
            _isLoading.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    cachedSearchResults[query] = resp.value
                    _searchResults.postValue(resp.value!!)
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    /**
     * Clear the search results
     */
    fun clearSearchResults() {
        _searchResults.postValue(ArrayList())
    }
}