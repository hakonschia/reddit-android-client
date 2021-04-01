package com.example.hakonsreader.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.utils.createFullName
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * ViewModel for retrieving Reddit posts from a user or subreddit
 *
 * @param userOrSubredditName The name of the user or subreddit to retrieve posts from. This is
 * mutable, but be aware that if changed it has to match the previous user or subreddit type (ie.
 * it shouldn't go from a user to a subreddit)
 * @param isUser True if the ViewModel is loading posts for a user, false for a subreddit
 */
class PostsViewModel @AssistedInject constructor (
        @Assisted private var userOrSubredditName: String,
        @Assisted private val isUser: Boolean,
        @Assisted private val savedStateHandle: SavedStateHandle,
        private val api: RedditApi,
        private val postsDao: RedditPostsDao,
) : ViewModel() {

    companion object {
        private const val TAG = "PostsViewModel"

        private const val SAVED_POST_IDS = "saved_postIds"
    }

    @AssistedFactory
    interface Factory {
        /**
         * Factory for the ViewModel
         *
         * @param userOrSubredditName The name of the user or subreddit to load posts for
         * @param isUser True if [userOrSubredditName] points to a username, false if for a subreddit
         */
        fun create(
                userOrSubredditName: String,
                isUser: Boolean,
                savedStateHandle: SavedStateHandle
        ) : PostsViewModel
    }

    private var arePostsBeingRestored = false

    private val _posts = MutableLiveData<List<RedditPost>>()
    private val _loadingChange = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    init {
        val savedPostIds: ArrayList<String>? = savedStateHandle[SAVED_POST_IDS]
        if (savedPostIds != null) {
            Log.d(TAG, "name=$userOrSubredditName, postIds.size=${savedPostIds.size}")
            restorePosts(savedPostIds)
        }
    }

    val posts: LiveData<List<RedditPost>> = _posts
    val onLoadingCountChange: LiveData<Boolean> = _loadingChange
    val error: LiveData<ErrorWrapper> = _error

    /**
     * The saved states of the posts
     */
    var savedPostStates: HashMap<String, Bundle>? = null

    var sort: SortingMethods = SortingMethods.HOT
        private set
    var timeSort: PostTimeSort = PostTimeSort.DAY
        private set

    /**
     * The post IDs refer to the IDs of the posts this ViewModel is currently tracking.
     */
    var postIds = mutableListOf<String>()



    /**
     * Restarts posts from start based on the previous sorting
     */
    fun restart() {
        postIds.clear()
        _posts.value = ArrayList()
        loadPosts(sort, timeSort)
    }

    /**
     * Updates how to sort posts and restarts the posts from start
     *
     * @param sort How to sort the posts
     * @param timeSort How to sort the posts based on time. Only applicable for *top* and *controversial*.
     * Default is [PostTimeSort.DAY]
     */
    fun restart(sort: SortingMethods, timeSort: PostTimeSort = PostTimeSort.DAY) {
        this.sort = sort
        this.timeSort = timeSort
        restart()
    }

    /**
     * Loads posts continuing from the current posts, or starting from scratch if there are no
     * posts loaded.
     *
     * If posts are currently being restored from a [SavedStateHandle] then this will not call the API.
     *
     * @param sort How to sort the posts. To change the sort after the first load, use [restart]
     * @param timeSort How to sort the posts based on time. Only applicable for *top* and *controversial*.
     * Default is [PostTimeSort.DAY]. To change the sort after the first load, use [restart]
     */
    fun loadPosts(sort: SortingMethods? = SortingMethods.HOT, timeSort: PostTimeSort? = PostTimeSort.DAY) {
        sort?.let { this.sort = it }
        timeSort?.let { this.timeSort = it }
        val count = postIds.size

        val after = if (count > 0) {
            createFullName(Thing.POST, postIds.last())
        } else {
            ""
        }

        load(after, count)
    }

    /**
     * Loads posts for the user/subreddit
     *
     * @param after The ID of the last post seen
     * @param count The amount of posts already seen
     */
    private fun load(after: String, count: Int) {
        if (arePostsBeingRestored) {
            return
        }
        _loadingChange.value = true

        viewModelScope.launch {
            val resp = if (isUser) {
                api.user(userOrSubredditName).posts(sort, timeSort, after, count)
            } else {
                api.subreddit(userOrSubredditName).posts(sort, timeSort, after, count)
            }
            _loadingChange.postValue(false)

            when (resp) {
                is ApiResponse.Success -> withContext(IO) { onPostsRetrieved(resp.value) }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    private fun onPostsRetrieved(newPosts: List<RedditPost>) {
        val filtered = newPosts.filter {
            val id = it.id
            // If the ID isn't in the list, add it and keep it in the filtered list
            // TODO this has crashed from ArrayIndexOutOfBoundsException with length=19, index= 26
            //  which makes no sense?
            if (!postIds.contains(id)) {
                postIds.add(id)
                true
            } else {
                false
            }
        }

        savedStateHandle[SAVED_POST_IDS] = postIds

        val postsData: List<RedditPost> = if (posts.value != null) {
            val list = posts.value as MutableList
            list.addAll(filtered)
            list
        } else {
            // If there is no previous list then we can safely return all new posts
            newPosts
        }

        _posts.postValue(postsData)

        // Inserting posts sometimes causes ConcurrentModificationException, so only insert posts
        // at the end instead of in the loop and at the end to try and fix it
        val postsToInsertIntoDb = ArrayList<RedditPost>()
        postsToInsertIntoDb.addAll(newPosts)

        // Store the crossposts
        for (newPost in newPosts) {
            val crossposts = newPost.crossposts

            if (!crossposts.isNullOrEmpty()) {
                val crosspostIds = ArrayList<String>()

                // Insert all crossposts and copy the IDs and set that list on the post itself
                // We have to store the crossposts by ID this way since room doesn't like it
                // when there are RedditPost objects inside a RedditPost (or I just don't know how to)
                for (crosspost in crossposts) {
                    postsToInsertIntoDb.add(crosspost)
                    crosspostIds.add(crosspost.id)
                }

                newPost.crosspostIds = crosspostIds
            }
        }

        // Store (or update) the posts in the database
        // We use all the posts here as duplicates will just be updated, which is fine
        // This must be called after the crossposts are set or else the IDs wont be stored
        postsDao.insertAll(postsToInsertIntoDb)
    }

    /**
     * Restores posts from the local database
     *
     * @param ids The IDs to restore
     */
    private fun restorePosts(ids: List<String>) {
        // TODO this wont restore third party
        arePostsBeingRestored = true
        _loadingChange.value = true
        // Posts are saved for 2 days, so if the user hasn't opened the app for a long time they might
        // have been removed (not sure if SavedStateHandle saves for an infinite amount of time?)
        CoroutineScope(IO).launch {
            val posts = postsDao.getPostsById(ids)
            posts.forEach {
                if (!it.crosspostIds.isNullOrEmpty()) {
                    it.crossposts = postsDao.getPostsById(it.crosspostIds!!)
                }
            }

            withContext(Main) {
                _posts.value = posts
                _loadingChange.value = false
            }
            arePostsBeingRestored = false
        }
    }
}