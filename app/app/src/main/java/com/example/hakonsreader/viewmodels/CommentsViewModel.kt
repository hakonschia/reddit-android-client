package com.example.hakonsreader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hakonsreader.App
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class CommentsViewModel: ViewModel() {

    private val api = App.get().api
    private val database = App.get().database

    private val post = MutableLiveData<RedditPost>()
    private val comments = MutableLiveData<List<RedditComment>>()
    private val loadingChange = MutableLiveData<Boolean>()
    private val error = MutableLiveData<ErrorWrapper>()

    fun getPost() : LiveData<RedditPost> = post
    fun getComments() : LiveData<List<RedditComment>> = comments
    fun onLoadingCountChange() : LiveData<Boolean> = loadingChange
    fun getError() : LiveData<ErrorWrapper> = error

    /**
     * The ID of the post. This must be set before an attempt at loading the comments is made
     */
    var postId = ""


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

        loadingChange.value = true

        CoroutineScope(IO).launch {
            val resp = api.post(postId).comments(loadThirdParty = loadThirdParty)
            loadingChange.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    post.postValue(resp.value.post)
                    comments.postValue(resp.value.comments)
                    database.posts().insert(resp.value.post)
                }
                is ApiResponse.Error -> error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    fun loadMoreComments(comment: RedditComment, parent: RedditComment? = null) {
        if (postId.isBlank()) {
            throw IllegalStateException("Post ID not set")
        }

        loadingChange.value = true

        CoroutineScope(IO).launch {
            val resp = api.post(postId).moreComments(comment.children, parent)
            loadingChange.postValue(false)

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

                    comments.postValue(dataSet)
                }
                is ApiResponse.Error -> {
                    resp.throwable.printStackTrace()
                    error.postValue(ErrorWrapper(resp.error, resp.throwable))
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
        comments.value = ArrayList()
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
        comments.value = dataSet
    }
}