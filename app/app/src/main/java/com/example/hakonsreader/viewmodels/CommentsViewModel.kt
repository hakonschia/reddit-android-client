package com.example.hakonsreader.viewmodels

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsViewModel: ViewModel() {

    private val api = App.get().api
    private val database = App.get().database

    private val _post = MutableLiveData<RedditPost>()
    private val _comments = MutableLiveData<List<RedditComment>>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    val post: LiveData<RedditPost> = _post
    val comments: LiveData<List<RedditComment>> = _comments
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * The ID of the post. This must be set before an attempt at loading the comments is made
     */
    var postId = ""

    var savedExtras: Bundle? = null


    /**
     * Loads comments for the post.
     *
     * This will also fetch the post itself. The post will be updated in [RedditDatabase] and must
     * be observed from there to receieve changes
     *
     * @param loadThirdParty If true, third party requests (such as retrieving gifs from Gfycat directly)
     * will be made. This is default to `false`. If only the comments of the post (and potentially updated
     * post information) is needed, consider keeping this to `false` to not make unnecessary API calls.
     * In other words, this should only be set to `true` if the post is loaded for the first time and
     * the content of the post has to be drawn.
     *
     * @throws IllegalStateException if [postId] is not set
     */
    @Throws(IllegalStateException::class)
    fun loadComments(loadThirdParty: Boolean = false) {
        if (postId.isBlank()) {
            throw IllegalStateException("Post ID not set")
        }

        _isLoading.value = true

        viewModelScope.launch {
            val resp = api.post(postId).comments(loadThirdParty = loadThirdParty)
            _isLoading.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    _post.postValue(resp.value.post)
                    _comments.postValue(resp.value.comments)
                    withContext(IO) {
                        insertPostIntoDb(resp.value.post)
                    }
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    fun loadMoreComments(comment: RedditComment, parent: RedditComment? = null) {
        if (postId.isBlank()) {
            throw IllegalStateException("Post ID not set")
        }

        // Technically this is not a "1 thing loading" since this is something else than the main comments
        // but I don't know how more comments and comments can be loaded at the same time so it's fine
        _isLoading.value = true

        viewModelScope.launch {
            val resp = api.post(postId).moreComments(comment.children, parent)
            _isLoading.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    val dataSet = ArrayList(comments.value)

                    // Find the parent index to know where to insert the new comments
                    val commentPos = dataSet.indexOf(comment)

                    // Remove the original comment (the "2 more comments" comment) and insert the new
                    // comments in its place
                    dataSet.removeAt(commentPos)
                    dataSet.addAll(commentPos, resp.value)

                    parent?.removeReply(comment)

                    _comments.postValue(dataSet)
                }
                is ApiResponse.Error -> {
                    resp.throwable.printStackTrace()
                    _error.postValue(ErrorWrapper(resp.error, resp.throwable))
                }
            }
        }
    }

    /**
     * Loads comments from scratch
     *
     * @throws IllegalStateException If [postId] is not set
     */
    @Throws(IllegalStateException::class)
    fun restart() {
        _comments.value = ArrayList()
        loadComments()
    }

    /**
     * Inserts a comment. Top-level comments are inserted at the start of the list
     *
     * @param newComment The comment to insert
     * @param parent The parent of the comment. If the comment is a top-level comment this should
     * be omitted (or set to *null*)
     */
    fun insertComment(newComment: RedditComment, parent: RedditComment? = null) {
        val dataSet = ArrayList(comments.value)

        val posToInsert = if (parent != null) {
            // Insert after the parent
            dataSet.indexOf(parent) + 1
        } else {
            0
        }

        dataSet.add(posToInsert, newComment)
        _comments.value = dataSet
    }

    private fun insertPostIntoDb(post: RedditPost) {
        val crossposts = post.crossposts
        val postsToInsertIntoDb = java.util.ArrayList<RedditPost>().apply {
            add(post)
        }

        if (!crossposts.isNullOrEmpty()) {
            val crosspostIds = java.util.ArrayList<String>()

            // Insert all crossposts and copy the IDs and set that list on the post itself
            // We have to store the crossposts by ID this way since room doesn't like it
            // when there are RedditPost objects inside a RedditPost (or I just don't know how to)
            for (crosspost in crossposts) {
                postsToInsertIntoDb.add(crosspost)
                crosspostIds.add(crosspost.id)
            }

            post.crosspostIds = crosspostIds
        }

        database.posts().insertAll(postsToInsertIntoDb)
    }
}