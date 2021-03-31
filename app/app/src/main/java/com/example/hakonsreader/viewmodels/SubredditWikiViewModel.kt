package com.example.hakonsreader.viewmodels

import androidx.lifecycle.*
import com.example.hakonsreader.api.model.SubredditWikiPage
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.launch
import kotlin.collections.ArrayDeque
import kotlin.collections.HashMap

/**
 * ViewModel for loading wiki pages for a subreddit
 */
class SubredditWikiViewModel(
        private val api: SubredditRequest
) : ViewModel() {

    class Factory(
            private val api: SubredditRequest
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SubredditWikiViewModel(api) as T
        }
    }

    private val pageStack = ArrayDeque<String>()

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
     * @return True if the pages can go back
     */
    fun canGoBack() = pageStack.size >= 2

    /**
     * Pops the stack of pages and updates [page] with the next item, if [canGoBack] returns true
     */
    fun pop() {
        if (canGoBack()) {
            pageStack.removeLast()
            val pageName = pageStack.last()
            // Since we're popping the stack we don't want to add the page back
            loadPage(pageName, addToStack = false)
        }
    }

    /**
     * Load a wiki page
     *
     * @param pageName The name of the wiki to load (by default, `index` is loaded)
     * @param addToStack True to add this page to the stack. This is primarily used internally and
     * should be used with care
     */
    fun loadPage(pageName: String = "index", addToStack: Boolean = true) {
        // Even if it is empty it would redirect to "index", but this is to always save the correct one
        val actualPageName = if (pageName.isEmpty()) {
            "index"
        } else {
            pageName
        }

        if (addToStack) {
            pageStack.addLast(actualPageName)
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