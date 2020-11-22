package com.example.hakonsreader.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * The key used to store the list of subreddit IDs the user is subscribed to
 */
private const val TAG = "SelectSubredditsViewModelK"


/**
 * ViewModel for retrieving subreddits. The subreddits retrieved are automatically chosen for
 * a logged in users subscribed subreddits, or for default subreddits for non-logged in users
 */
class SelectSubredditsViewModelK(context: Context) : ViewModel() {
    companion object {
        const val SUBSCRIBED_SUBREDDITS_KEY = "subscribedSubreddits"
    }

    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val api = App.get().api

    private val subreddits: MutableLiveData<List<Subreddit>> by lazy {
        MutableLiveData<List<Subreddit>>().also {
            val ids = SharedPreferencesManager.get(SUBSCRIBED_SUBREDDITS_KEY, Array<String>::class.java)
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
     * The IDs are stored in SharedPreferences with the key [SUBSCRIBED_SUBREDDITS_KEY]
     */
    fun loadSubreddits() {
        onCountChange.value = true

        CoroutineScope(IO).launch {
            val response = api.subredittsKt().getSubreddits()
            onCountChange.postValue(false)

            when (response) {
                is ApiResponse.Success -> {
                    val subs = response.value

                    subreddits.postValue(subs)

                    // Store the subreddits so they're shown instantly the next time
                    val ids = arrayOfNulls<String>(subs.size)
                    subs.forEachIndexed { index, subreddit -> ids[index] = subreddit.id }
                    SharedPreferencesManager.put(SUBSCRIBED_SUBREDDITS_KEY, ids)

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