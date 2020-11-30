package com.example.hakonsreader.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for retrieving Reddit posts from a user or subreddit
 *
 * @param context An Android context used to create the local database if needed
 * @param userOrSubredditName The name of the user or subreddit to retrieve posts from. This is
 * mutable, but be aware that if changed it has to match the previous user or subreddit type (ie.
 * it shouldn't go from a user to a subreddit)
 * @param isUser True if the ViewModel is loading posts for a user, false for a subreddit
 */
class PostsViewModel(
        val context: Context,
        var userOrSubredditName: String,
        private val isUser: Boolean
) : ViewModel() {

    private val database = AppDatabase.getInstance(context)
    private val api = App.get().api

    private val posts = MutableLiveData<List<RedditPost>>()
    private val loadingChange = MutableLiveData<Boolean>()
    private val error = MutableLiveData<ErrorWrapper>()

    private var sort: SortingMethods = SortingMethods.HOT
    private var timeSort: PostTimeSort = PostTimeSort.DAY

    /**
     * The post IDs refer to the IDs of the posts this ViewModel is currently tracking.
     *
     * Setting this value will retrieve the posts from the local database and set the value on [posts]
     */
    var postIds = mutableListOf("")
        set(value) {
            field = value

            // TODO this
            CoroutineScope(IO).launch {
                val postsFromDb = database.posts().getPostsById(value)
                val sorted = ArrayList<RedditPost>()

                value.forEach {
                    // TODO crossposts aren't stored/retrieved correctly and they will cause a crash
                    //  when attempting to create content
                    val post = findPost(postsFromDb, it)
                    if (post != null) {
                        sorted.add(post)
                        val crosspostIds = post.crosspostIds
                        if (crosspostIds?.isNotEmpty() == true) {
                           post.crossposts = database.posts().getPostsById(crosspostIds)
                        }
                    }
                }

                posts.postValue(sorted)
            }
        }

    private fun findPost(list: List<RedditPost>, id: String) : RedditPost? {
        list.forEach {
            if (it.id == id) {
                return it
            }
        }

        return null
    }

    fun getPosts() : LiveData<List<RedditPost>> = posts
    fun onLoadingCountChange() : LiveData<Boolean> = loadingChange
    fun getError() : LiveData<ErrorWrapper> = error


    /**
     * Restarts posts from start based
     */
    fun restart() {
        postIds.clear()
        posts.value = ArrayList<RedditPost>()
        loadPosts()
    }

    /**
     * Updates how to sort posts and restarts the posts from start
     *
     * @param sort How to sort the posts
     * @param timeSort How to sort the posts based on time. Only applicable for *top* and *controversial*
     */
    fun restart(sort: SortingMethods?, timeSort: PostTimeSort?) {
        // TODO until the fragments using this method use Kotlin these can be null (for new, hot)
        if (sort != null) {
            this.sort = sort
        }
        if (timeSort != null) {
            this.timeSort = timeSort
        }
        restart()
    }

    fun loadPosts() {
        val count = postIds.size

        val after = if (count > 0) {
            Thing.POST.value + "_" + postIds.last()
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
        loadingChange.value = true
        CoroutineScope(IO).launch {
            val resp = if (isUser) {
                api.userKt(userOrSubredditName).posts(sort, timeSort, after, count)
            } else {
                api.subredditKt(userOrSubredditName).posts(sort, timeSort, after, count)
            }
            loadingChange.postValue(false)

            when (resp) {
                is ApiResponse.Success -> onPostsRetrieved(resp.value)
                is ApiResponse.Error -> error.postValue(ErrorWrapper(resp.error, resp.throwable))
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

        val postsData: List<RedditPost> = if (posts.value != null) {
            val list = posts.value as MutableList
            list.addAll(filtered)
            list
        } else {
            // If there is no previous list then we can safely return all new posts
            newPosts
        }

        posts.postValue(postsData)

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
                    database.posts().insert(crosspost)
                    postsToInsertIntoDb.add(crosspost)
                    crosspostIds.add(crosspost.id)
                }

                newPost.crosspostIds = crosspostIds
            }
        }

        // Store (or update) the posts in the database
        // We use all the posts here as duplicates will just be updated, which is fine
        // This must be called after the crossposts are set or else the IDs wont be stored
        database.posts().insertAll(postsToInsertIntoDb)
    }
}