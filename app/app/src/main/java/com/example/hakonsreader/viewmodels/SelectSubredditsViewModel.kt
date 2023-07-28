package com.example.hakonsreader.viewmodels

import androidx.lifecycle.*
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.states.AppState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for retrieving subreddits. The subreddits retrieved are automatically chosen for
 * a logged in users subscribed subreddits, or for default subreddits for non-logged in users
 */
class SelectSubredditsViewModel @AssistedInject constructor(
        private val api: RedditApi,
        private val subredditsDao: RedditSubredditsDao,
        @Assisted var isForLoggedInUser: Boolean
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        /**
         * Factory for the ViewModel
         *
         * @param isForLoggedInUser Set to true if the ViewModel is for the logged in user. This will
         * make the ViewModel load the users subscribed subreddits
         */
        fun create(
                isForLoggedInUser: Boolean
        ): SelectSubredditsViewModel
    }

    private var loadedFromApi = false

    private val ids = MutableLiveData<List<String>>().apply {
        CoroutineScope(IO).launch {
            postValue(if (isForLoggedInUser) {
                subredditsDao.getSubscribedSubredditsNoObservable().map { it.id }
            } else {
                ArrayList()
            })
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val subreddits: LiveData<List<Subreddit>> get() {
        return ids.switchMap {
            subredditsDao.getSubsById(it)
        }
    }
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * Load the subreddits. If a user is logged in their subscribed subreddits are loaded, otherwise
     * default subs are loaded.
     *
     * The list returned is not sorted
     *
     * @param force If true then subreddits will be forced to load, even if previously loaded
     */
    fun loadSubreddits(force: Boolean = false) {
        if (loadedFromApi && !force) {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            val response = if (isForLoggedInUser) {
                api.subreditts().subscribedSubreddits()
            } else {
                api.subreditts().defaultSubreddits()
            }
            _isLoading.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    loadedFromApi = true
                    val subreddits = response.value

                    val subredditIds: MutableList<String> = ArrayList()
                    subreddits.forEach { subreddit -> subredditIds.add(subreddit.id) }

                    ids.postValue(subredditIds)

                    if (isForLoggedInUser) {
                        // Store the subreddits so they're shown instantly the next time
                        AppState.updateUserInfo(subreddits = subredditIds)
                    }

                    // Although NSFW subs might be inserted with this, it's fine as if the user
                    // has subscribed to them it's fine (for non-logged in users, default subs don't include NSFW)
                    withContext(IO) {
                        subredditsDao.insertAll(subreddits)
                    }
                }
                is ApiResponse.Error -> {
                    _error.postValue(ErrorWrapper(response.error, response.throwable))
                }
            }
        }
    }

    /**
     * Updates the favorite for a subreddit. Calls the Reddit API and based on the response
     * updates the favorite status accordingly
     */
    fun favorite(subreddit: Subreddit) {
        val favorite = !subreddit.isFavorited
        subreddit.isFavorited = favorite

        viewModelScope.launch {
            withContext(IO) {
                subredditsDao.update(subreddit)
            }
            when (val response = api.subreddit(subreddit.name).favorite(favorite)) {
                is ApiResponse.Success -> { }
                is ApiResponse.Error -> {
                    _error.postValue(ErrorWrapper(response.error, response.throwable))

                    // Request failed, revert
                    subreddit.isFavorited = !favorite
                    withContext(IO) {
                        subredditsDao.update(subreddit)
                    }
                }
            }
        }
    }
}