package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for retrieving subreddits. The subreddits retrieved are automatically chosen for
 * a logged in users subscribed subreddits, or for default subreddits for non-logged in users
 */
class SelectSubredditsViewModel(private val isForLoggedInUser: Boolean) : ViewModel() {
    private val database = App.get().database
    private val api = App.get().api
    private var loadedFromApi = false

    private val loggedInUserLiveData = database.subreddits().getSubscribedSubreddits()
    private val defaultLiveData = MutableLiveData<List<Subreddit>>()

    private val onCountChange = MutableLiveData<Boolean>()
    private val error = MutableLiveData<ErrorWrapper>()

    // Kind of weird probably? The subscribed subreddits can easily be observed and automatically updated
    // but the default subreddits don't have anything identifying them, and aren't the only subreddits stored
    fun getSubreddits() : LiveData<List<Subreddit>> = if (isForLoggedInUser) loggedInUserLiveData else defaultLiveData
    fun getOnCountChange() : LiveData<Boolean> = onCountChange
    fun getError() : LiveData<ErrorWrapper> = error

    /**
     * Load the subreddits. If a user is logged in their subscribed subreddits are loaded, otherwise
     * default subs are loaded.
     *
     * The list returned is not sorted
     *
     * The IDs are stored in [App.currentUserInfo]
     *
     * @param force If true then subreddits will be forced to load, even if previously loaded
     */
    fun loadSubreddits(force: Boolean = false) {
        if (loadedFromApi && !force) {
            return
        }
        onCountChange.value = true

        CoroutineScope(IO).launch {
            val response = if (isForLoggedInUser) {
                api.subreditts().subscribedSubreddits()
            } else {
                api.subreditts().defaultSubreddits()
            }
            onCountChange.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    loadedFromApi = true
                    val subs = response.value

                    if (isForLoggedInUser) {
                        // Store the subreddits so they're shown instantly the next time
                        val ids: MutableList<String> = ArrayList()
                        subs.forEach { subreddit -> ids.add(subreddit.id) }
                        App.get().updateUserInfo(subreddits = ids)
                    } else {
                        defaultLiveData.postValue(subs)
                    }

                    // Although NSFW subs might be inserted with this, it's fine as if the user
                    // has subscribed to them it's fine (for non-logged in users, default subs don't include NSFW)
                    database.subreddits().insertAll(subs)
                }
                is ApiResponse.Error -> {
                    error.postValue(ErrorWrapper(response.error, response.throwable))
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

        CoroutineScope(IO).launch {
            database.subreddits().update(subreddit)
            when (val response = api.subreddit(subreddit.name).favorite(favorite)) {
                is ApiResponse.Success -> { }
                is ApiResponse.Error -> {
                    // Request failed, revert
                    subreddit.isFavorited = !favorite
                    database.subreddits().update(subreddit)
                    error.postValue(ErrorWrapper(response.error, response.throwable))
                }
            }
        }
    }
}