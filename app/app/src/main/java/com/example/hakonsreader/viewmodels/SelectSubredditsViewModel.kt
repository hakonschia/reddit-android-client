package com.example.hakonsreader.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.misc.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            CoroutineScope(IO).launch {
                subreddits.postValue(database.subreddits().getSubsById(ids))
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
        Log.d(TAG, "loadSubreddits: LOADING SUBREDDITS FROM KOTLIN")
        onCountChange.value = true

        api.subreddits().getSubreddits("", 0, { subs ->
            onCountChange.value = false
            subreddits.value = subs

            val ids = arrayOfNulls<String>(subs.size)
            subs.forEachIndexed { index, subreddit -> ids[index] = subreddit.id }
            SharedPreferencesManager.put(SUBSCRIBED_SUBREDDITS_KEY, ids)

            // Although NSFW subs might be inserted with this, it's fine as if the user
            // has subscribed to them it's fine (for non-logged in users, default subs don't include NSFW)
            CoroutineScope(IO).launch {
                database.subreddits().insertAll(subs)
            }
        }, { e, t ->
            onCountChange.value = false
            error.value = ErrorWrapper(e, t)
        })
    }
}