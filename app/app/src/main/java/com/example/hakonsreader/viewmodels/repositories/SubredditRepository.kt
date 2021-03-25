package com.example.hakonsreader.viewmodels.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.viewmodels.ErrorWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubredditRepository(
        private val subredditName: String,
        private val api: RedditApi,
        private val dao: RedditSubredditsDao,
        private val postsDao: RedditPostsDao
) {

    private var infoLoaded = false

    private val subredditNameObservable = MutableLiveData<String>().apply {
        value = subredditName
    }

    private val _errors = MutableLiveData<ErrorWrapper>()
    private val _isLoading = MutableLiveData<Boolean>()

    val errors = _errors as LiveData<ErrorWrapper>
    val isLoading = _isLoading as LiveData<Boolean>

    fun getSubreddit() : LiveData<Subreddit?> {
        if (!infoLoaded) {
            CoroutineScope(IO).launch {
                refresh()
            }
        }

        return Transformations.switchMap(subredditNameObservable) {
            dao.getLive(it)
        }
    }

    suspend fun refresh() {
        _isLoading.postValue(true)

        when (val resp = api.subreddit(subredditNameObservable.value!!).info()) {
            is ApiResponse.Success -> {
                infoLoaded = true
                val sub = resp.value
                dao.insert(sub)

                // If a redirect occurred, a different subreddit will be sent back
                // Eg. if this is for the subreddit "random", a random subreddit will be returned
                if (!subredditName.equals(sub.name, ignoreCase = true)) {
                    subredditNameObservable.postValue(sub.name)
                }
            }
            is ApiResponse.Error -> {
                _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }

        _isLoading.postValue(false)
    }

    /**
     * Update the subscription on a subreddit
     *
     * [Subreddit.isSubscribed] will be flipped, and the subscribe count will be updated
     */
    suspend fun subscribe() {
        val subreddit = withContext(IO) {
            dao.get(subredditNameObservable.value!!)
        } ?: return

        // Assume success
        val newSubscription = !subreddit.isSubscribed
        subreddit.isSubscribed = newSubscription
        subreddit.subscribers += if (newSubscription) 1 else -1

        withContext(IO) {
            dao.update(subreddit)
        }

        when (val response = api.subreddit(subredditNameObservable.value!!).subscribe(newSubscription)) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                // Revert back
                subreddit.isSubscribed = !newSubscription
                subreddit.subscribers += if (!newSubscription) 1 else -1
                withContext(IO) {
                    dao.update(subreddit)
                }

                _errors.postValue(ErrorWrapper(response.error, response.throwable))
            }
        }
    }

    /**
     * Updates the flair for a user on the subreddit. Any posts the user might have in the database
     * will also have the author flair updated
     *
     * @param username The username of the user to change flair for
     * @param flair The flair to change to, or `null` to disable the flair on the subreddit
     */
    suspend fun updateFlair(username: String, flair: RedditFlair?) {
        val subreddit = withContext(IO) {
            dao.get(subredditName)
        } ?: return

        subreddit.userFlairBackgroundColor = flair?.backgroundColor
        subreddit.userFlairRichText = flair?.richtextFlairs
        subreddit.userFlairText = flair?.text
        subreddit.userFlairTextColor = flair?.textColor

        withContext(IO) {
            dao.update(subreddit)

            // Update all posts the user potentially has in the database
            postsDao.getPostsByUser(username).apply {
                forEach {
                    it.authorFlairBackgroundColor = flair?.backgroundColor
                    it.authorRichtextFlairs = flair?.richtextFlairs ?: ArrayList()
                    it.authorFlairText = flair?.text
                    it.authorFlairTextColor = flair?.textColor
                }
                postsDao.updateAll(this)
            }
        }

        when (val resp = api.subreddit(subredditNameObservable.value!!).selectFlair(username, flair?.id)) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }
}