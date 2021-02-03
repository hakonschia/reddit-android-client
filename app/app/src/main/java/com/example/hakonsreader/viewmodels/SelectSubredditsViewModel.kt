package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * ViewModel for retrieving subreddits. The subreddits retrieved are automatically chosen for
 * a logged in users subscribed subreddits, or for default subreddits for non-logged in users
 */
class SelectSubredditsViewModel : ViewModel() {
    private val database = App.get().database
    private val api = App.get().api

    private val subreddits: MutableLiveData<List<Subreddit>> by lazy {
        MutableLiveData<List<Subreddit>>().also {
            val ids = App.get().currentUserInfo?.subscribedSubreddits
            if (!ids.isNullOrEmpty()) {
                CoroutineScope(IO).launch {
                    subreddits.postValue(database.subreddits().getSubsById(ids))
                }
            }
        }
    }
    private val onCountChange = MutableLiveData<Boolean>()
    private val error = MutableLiveData<ErrorWrapper>()

    fun getSubreddits() : LiveData<List<Subreddit>> = subreddits
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
     * @param loadDefaultSubs Set to *true* to load default subs, *false* to load subs for a logged in user.
     * Default is *true* (load default subs)
     */
    fun loadSubreddits(loadDefaultSubs: Boolean = true) {
        onCountChange.value = true

        CoroutineScope(IO).launch {
            val response = if (loadDefaultSubs) {
                api.subreditts().defaultSubreddits()
            } else {
                api.subreditts().subscribedSubreddits()
            }
            onCountChange.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    val subs = response.value

                    subreddits.postValue(subs)

                    if (!loadDefaultSubs) {
                        // Store the subreddits so they're shown instantly the next time
                        val ids: MutableList<String> = ArrayList()
                        subs.forEach { subreddit -> ids.add(subreddit.id) }
                        App.get().currentUserInfo?.apply {
                            subscribedSubreddits = ids

                            database.userInfo().update(this)
                        }
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
}