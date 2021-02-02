package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SearchForSubredditsViewModel : ViewModel() {

    private val api = App.get().api
    private val searchResults = MutableLiveData<List<Subreddit>>()
    private val onCountChange = MutableLiveData<Boolean>()
    private val error = MutableLiveData<ErrorWrapper>()

    /**
     * Map containing the previous search results, mapping a search query to a list of subreddits
     */
    private val cachedSearchResults = HashMap<String, List<Subreddit>>()

    fun getSearchResults() : LiveData<List<Subreddit>> = searchResults
    fun getOnCountChange() : LiveData<Boolean> = onCountChange
    fun getError() : LiveData<ErrorWrapper> = error


    /**
     * Search for subreddits
     *
     * @param query The search query
     */
    fun search(query: String) {
        if (cachedSearchResults.containsKey(query)) {
            searchResults.postValue(cachedSearchResults[query])
            return
        }

        onCountChange.postValue(true)

        CoroutineScope(IO).launch {
            val resp = api.subreditts().search(query)
            onCountChange.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    cachedSearchResults[query] = resp.value
                    searchResults.postValue(resp.value)
                }
                is ApiResponse.Error -> error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    /**
     * Clear the search results
     */
    fun clearSearchResults() {
        searchResults.postValue(ArrayList())
    }
}