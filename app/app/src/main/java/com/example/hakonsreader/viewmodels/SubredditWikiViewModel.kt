package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.model.SubredditWikiPage
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.launch

/**
 * ViewModel for loading wiki pages for a subreddit
 */
class SubredditWikiViewModel(
        private val api: SubredditRequest
) : ViewModel() {

    /**
     * A cache of the previously stored wiki pages
     */
    private val pages = HashMap<String, SubredditWikiPage>()

    private val _page = MutableLiveData<SubredditWikiPage>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val page: LiveData<SubredditWikiPage> = _page
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * Load a wiki page
     *
     * @param pageName The name of the wiki to load (by default, `index` is loaded)
     */
    fun loadPage(pageName: String = "index") {
        // Even if it is empty it would redirect to "index", but this is to always save the correct one
        val actualPageName = if (pageName.isEmpty()) {
            "index"
        } else {
            pageName
        }

        if (pages.containsKey(actualPageName)) {
            _page.value = pages[actualPageName]
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            when (val resp = api.wiki(actualPageName)) {
                is ApiResponse.Success -> {
                    val wikiPage = resp.value
                    pages[actualPageName] = wikiPage
                    _page.postValue(wikiPage)
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }

            _isLoading.postValue(false)
        }
    }
}