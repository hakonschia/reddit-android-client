package com.example.hakonsreader.viewmodels.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.requestmodels.SubredditRequest
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.viewmodels.ErrorWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SubredditRepository(
        private val subredditName: String,
        private val api: SubredditRequest,
        private val dao: RedditSubredditsDao,
        private val postsDao: RedditPostsDao
) {

    private var infoLoaded = false

    private val _errors = MutableLiveData<ErrorWrapper>()
    private val _loading = MutableLiveData<Boolean>()

    val errors = _errors as LiveData<ErrorWrapper>
    val loading = _loading as LiveData<Boolean>

    fun getSubreddit() : Flow<Subreddit?> {
        if (!infoLoaded) {
            CoroutineScope(IO).launch {
                refresh()
            }
        }
        return dao.getFlow(subredditName)
    }

    suspend fun refresh() {
        _loading.postValue(true)

        when (val resp = api.info()) {
            is ApiResponse.Success -> {
                infoLoaded = true
                dao.insert(resp.value)
            }
            is ApiResponse.Error -> {
                _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }

        _loading.postValue(false)
    }

    /**
     * Update the subscription on a subreddit
     *
     * [Subreddit.isSubscribed] will be flipped, and the subscribe count will be updated
     */
    suspend fun subscribe() {
        val subreddit = dao.get(subredditName) ?: return

        // Assume success
        val newSubscription = !subreddit.isSubscribed
        subreddit.isSubscribed = newSubscription
        subreddit.subscribers += if (newSubscription) 1 else -1

        dao.update(subreddit)

        when (val response = api.subscribe(newSubscription)) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                // Revert back
                subreddit.isSubscribed = !newSubscription
                subreddit.subscribers += if (!newSubscription) 1 else -1
                dao.update(subreddit)
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
        val subreddit = dao.get(subredditName) ?: return

        if (flair != null) {
            subreddit.userFlairBackgroundColor = flair.backgroundColor
            subreddit.userFlairRichText = flair.richtextFlairs
            subreddit.userFlairText = flair.text
            subreddit.userFlairTextColor = flair.textColor

            dao.update(subreddit)

            // Update all posts the user potentially has in the database
            postsDao.getPostsByUser(username).apply {
                forEach {
                    it.authorFlairBackgroundColor = flair.backgroundColor
                    it.authorRichtextFlairs = flair.richtextFlairs
                    it.authorFlairText = flair.text
                    it.authorFlairTextColor = flair.textColor
                }
                postsDao.updateAll(this)
            }

            when (val resp = api.selectFlair(username, flair.id)) {
                is ApiResponse.Success -> { }
                is ApiResponse.Error -> {
                    _errors.postValue(ErrorWrapper(resp.error, resp.throwable))
                }
            }
        } else {
            // TODO "dont show flair on subreddit"
        }
    }
}