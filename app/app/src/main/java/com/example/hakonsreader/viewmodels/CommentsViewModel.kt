package com.example.hakonsreader.viewmodels

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.misc.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * ViewModel for loading comments for a Reddit post. The ID of the post to load should be set with
 * [postId] before an attempt is made, otherwise an exception will be thrown.
 *
 * [savedExtras] can be used to store post extras to survive configuration changes
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
        private val api: RedditApi,
        private val postsDao: RedditPostsDao
) : ViewModel() {

    private val _post = MutableLiveData<RedditPost>()
    private val _comments = MutableLiveData<List<RedditComment>>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _error = MutableLiveData<ErrorWrapper>()

    /**
     * All comments the view model has, independent of the comments passed to [_comments] for chains
     */
    private val allComments: MutableList<RedditComment> = ArrayList()

    /**
     * The SharedPreferences used to hold the time posts were last opened
     */
    var preferences: SharedPreferences? = null

    /**
     * The ID of the comment parent coming of the currently show chain, or null if no chain is shown
     */
    var chainId: String? = null
        private set

    val post: LiveData<RedditPost> = _post
    val comments: LiveData<List<RedditComment>> = _comments
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * The ID of the post. This must be set before an attempt at loading the comments is made
     */
    var postId = ""

    /**
     * The saved extras for the post, which can be used to survive configuration changes
     */
    var savedExtras: Bundle? = null


    /**
     * Loads comments for the post.
     *
     * This will also fetch the post itself. The post will be updated in the local database and can either
     * by observed from there, or from [post]
     *
     * @param loadThirdParty If true, third party requests (such as retrieving gifs from Gfycat directly)
     * will be made. This is default to `false`. If only the comments of the post (and potentially updated
     * post information) is needed, consider keeping this to `false` to not make unnecessary API calls.
     * In other words, this should only be set to `true` if the post is loaded for the first time and
     * the content of the post has to be drawn.
     * @param thirdPartyObject If the post already has a third party object it can be passed here so
     * the object will survive configuration changes without having to JSON the post
     *
     * @throws IllegalStateException if [postId] is not set
     */
    @Throws(IllegalStateException::class)
    fun loadComments(loadThirdParty: Boolean = false, thirdPartyObject: Any? = null) {
        if (postId.isBlank()) {
            throw IllegalStateException("Post ID not set")
        }

        _isLoading.value = true

        viewModelScope.launch {
            val resp = api.post(postId).comments(loadThirdParty = loadThirdParty)
            _isLoading.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    if (thirdPartyObject != null) {
                        resp.value.post.thirdPartyObject = thirdPartyObject
                    }

                    preferences?.let {
                        // Update the value for when the post was opened
                        val lastTimeOpenedKey = postId + SharedPreferencesConstants.POST_LAST_OPENED_TIMESTAMP
                        preferences!!.edit().putLong(lastTimeOpenedKey, System.currentTimeMillis() / 1000L).apply()
                    }

                    allComments.clear()
                    allComments.addAll(resp.value.comments)

                    if (chainId != null) {
                        showChain(chainId!!)
                    } else {
                        _comments.postValue(checkAndSetHiddenComments(allComments))
                    }

                    _post.postValue(resp.value.post)
                    withContext(IO) {
                        insertPostIntoDb(resp.value.post)
                    }
                }
                is ApiResponse.Error -> _error.postValue(ErrorWrapper(resp.error, resp.throwable))
            }
        }
    }

    /**
     * Loads more comments (from "2 more comments" type comments)
     *
     * @param comment The "2 more comment" clicked, holding the IDs of the comments to load
     * @param parent The parent of the comment, or `null` if the comments are top-level comments (ie.
     * the post is the parent)
     *
     * @throws IllegalStateException if [postId] is not set
     */
    fun loadMoreComments(comment: RedditComment, parent: RedditComment?) {
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
                    // TODO chain stuff

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

    /**
     * Removes the currently shown chain and shows all comments, if a chain is shown
     */
    fun removeChain() {
        if (chainId != null) {
            chainId = null
            _comments.postValue(allComments)
        }
    }

    /**
     * Shows a chain of comments by providing the ID of the root comment. If no comments are currently
     * shown  then the chain will be set when comments are loaded.
     *
     * If the ID is not found in the comments then nothing is done
     *
     * @param id The ID of the root comment
     */
    fun showChain(id: String) {
        chainId = id

        val comment = allComments.find { it.id == id }
        if (comment != null) {
            showChain(comment)
        }
    }

    /**
     * Shows a chain of comments starting with a given comment as the root
     *
     * @param comment The root comment
     */
    fun showChain(comment: RedditComment) {
        chainId = comment.id

        val commnts: MutableList<RedditComment> = ArrayList()

        // Add in the original comment (the start)
        commnts.add(comment)

        // We have to create a new list of the replies to avoid modifying the comments replies (when adding the start)
        commnts.addAll(comment.replies)

        _comments.postValue(commnts)
    }

    /**
     * Goes through [comments] and checks if a comments score is below the users threshold or if
     * Reddit has specified that it should be hidden.
     *
     * Comments with [RedditComment.isCollapsed] set to true children are removed
     */
    private fun checkAndSetHiddenComments(comments: MutableList<RedditComment>): List<RedditComment> {
        val commentsToRemove: MutableList<RedditComment> = ArrayList()
        val hideThreshold = Settings.getAutoHideScoreThreshold()

        comments.forEach { comment ->
            if (hideThreshold >= comment.score || comment.isCollapsed) {
                // If we got here from the score threshold make sure collapsed is set to true
                comment.isCollapsed = true
                commentsToRemove.addAll(getShownReplies(comment))
            }
        }

        return comments.apply {
            // We can't modify the comments list while looping over it, so we have to store the comments
            // that should be removed and remove them afterwards
            removeAll(commentsToRemove)
        }
    }


    /**
     * Retrieve the list of replies to a comment that are shown
     *
     * @param parent The parent to retrieve replies for
     * @return The list of children of [parent] that are shown. Children of children are also
     * included in the list
     */
    private fun getShownReplies(parent: RedditComment) : List<RedditComment> {
        val replies = ArrayList<RedditComment>()

        parent.replies.forEach {
            // Only add direct children, let the children handle their children
            if (it.depth - 1 == parent.depth) {
                replies.add(it)

                // Reply isn't hidden which means it potentially has children to show
                if (!it.isCollapsed) {
                    replies.addAll(getShownReplies(it))
                }
            }
        }

        return replies
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

        postsDao.insertAll(postsToInsertIntoDb)
    }

    /**
     * @return The time the post was last opened, or -1 if not applicable
     */
    fun getLastTimePostOpened(): Long {
        if (postId.isBlank() || preferences == null) {
            return -1
        }

        val lastTimeOpenedKey = postId + SharedPreferencesConstants.POST_LAST_OPENED_TIMESTAMP
        return preferences!!.getLong(lastTimeOpenedKey, -1)
    }
}