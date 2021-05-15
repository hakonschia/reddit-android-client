package com.example.hakonsreader.viewmodels

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.Thing
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
    private var allComments: List<RedditComment> = ArrayList()

    val post: LiveData<RedditPost> = _post
    val comments: LiveData<List<RedditComment>> = _comments
    val isLoading: LiveData<Boolean> = _isLoading
    val error: LiveData<ErrorWrapper> = _error

    /**
     * The SharedPreferences used to hold the time posts were last opened. This should be set before
     * comments are loaded to provide the expected behaviour
     */
    var preferences: SharedPreferences? = null

    /**
     * The ID of the comment parent coming of the currently show chain, or null if no chain is shown
     */
    var chainId: String? = null
        private set

    /**
     * The ID of the post. This must be set before an attempt at loading the comments is made
     */
    var postId = ""

    /**
     * The saved extras for the post, which can be used to survive configuration changes
     */
    var savedExtras: Bundle? = null

    /**
     * A saved layout state
     */
    var layoutState: Parcelable? = null

    /**
     * This callback is used to notify about the position of the comment that has been updated
     */
    var commentUpdatedCallback: ((Int) -> Unit)? = null

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
        check (postId.isNotBlank()) {
            "postId not set"
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

                    allComments = resp.value.comments

                    // If a chain was set before the comments were loaded then set it now
                    chainId?.let {
                        showChain(it)
                    } ?: run {
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
     * @throws IllegalStateException if [postId] is not set or if [comment] is not a "more comment" comment
     */
    fun loadMoreComments(comment: RedditComment) {
        check (postId.isNotBlank()) {
            "postId not set"
        }
        check (comment.kind == Thing.MORE.value) {
            "Comment passed must be a 'more comment' comment (RedditComment.kind=Thing.MORE.value)"
        }

        val parent: RedditComment? = findParent(comment)

        // Technically this is not a "1 thing loading" since this is something else than the main comments
        // but I don't know how more comments and comments can be loaded at the same time so it's fine
        _isLoading.value = true

        viewModelScope.launch {
            val resp = api.post(postId).moreComments(comment.children, parent)
            _isLoading.postValue(false)

            when (resp) {
                is ApiResponse.Success -> {
                    val newComments = resp.value
                    val dataSet = ArrayList(comments.value)

                    // Find the parent index to know where to insert the new comments
                    // As long as the rest of the code is valid, this will never return
                    val commentPos = dataSet.indexOf(comment)

                    // If the rest of the code is proper, this should never happen, but if this is called
                    // multiple times quickly, the comment might have been removed (if the comment is clicked twice fast)
                    // Multiple API calls will be made, which is not ideal, but at least it wont crash
                    if (commentPos >= 0) {
                        // Remove the original comment (the "2 more comments" comment) and insert the new
                        // comments in its place
                        dataSet.removeAt(commentPos)
                        dataSet.addAll(commentPos, newComments)
                    }

                    // Do the same for allComments to ensure both are up-to-date (in case the comments were
                    // loaded in a chain)
                    // We should not reuse the same list objects, as that can cause issues with DiffUtil
                    // not updating correctly
                    allComments = allComments.toMutableList().apply {
                        val commentPosInAllComments = indexOf(comment)
                        if (commentPosInAllComments >= 0) {
                            removeAt(commentPosInAllComments)
                            addAll(commentPosInAllComments, newComments)
                        }
                    }

                    parent?.removeReply(comment)

                    _comments.postValue(dataSet)
                }

                is ApiResponse.Error -> {
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

            // Set all comments, but remove the replies from hidden comments
            val all = allComments.toMutableList()
            val commentsToRemove = mutableListOf<RedditComment>()
            all.forEach { comment ->
                if (comment.isCollapsed) {
                    commentsToRemove.addAll(comment.replies)
                }
            }

            all.removeAll(commentsToRemove)
            _comments.postValue(all)
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
     * Shows a comment chain that has previously been hidden
     *
     * @param start The start of the chain
     * @see hideComments
     */
    fun showComments(start: RedditComment) {
        val commnts = _comments.value ?: return
        val pos = commnts.indexOf(start)

        if (pos < 0) {
            return
        }

        start.isCollapsed = false

        val replies = getShownReplies(start)

        // When the comment has no replies it won't be redrawn automatically with DiffUtil for some reason
        // and this callback was originally meant to address that issue, but the comment being shown or hidden
        // will disappear for a split second if we let DiffUtil do the changes. If we notify where the
        // the comment is, the observer of this can call notifyItemChanged() manually
        // which looks better as it is updated right away
        // Kind of bad solution but still kinda good?
        commentUpdatedCallback?.invoke(pos)

        if (replies.isNotEmpty()) {
            _comments.value = ArrayList<RedditComment>(commnts).apply {
                // Insert the replies after the start comment
                addAll(pos + 1, getShownReplies(start))
            }
        }
    }

    /**
     * Hides comments from being shown
     *
     * @param start The comment to start at
     * @see showComments
     */
    fun hideComments(start: RedditComment) {
        val commnts = _comments.value ?: return
        val pos = commnts.indexOf(start)

        if (pos < 0) {
            return
        }

        start.isCollapsed = true

        val replies = getShownReplies(start)

        commentUpdatedCallback?.invoke(pos)

        if (replies.isNotEmpty()) {
            _comments.value = ArrayList<RedditComment>(commnts).apply {
                removeAll(getShownReplies(start))
            }
        }
    }

    /**
     * Hides comments from being shown (long clickable)
     *
     * @param start The comment to start at
     * @return True
     * @see showComments
     */
    fun hideCommentsLongClick(start: RedditComment): Boolean {
        hideComments(start)
        return true
    }

    /**
     * Gets a comment by a fullname
     *
     * @param fullname The fullname of the comment to get
     * @return The comment, or null if not found in the adapter
     */
    fun getCommentByFullname(fullname: String) : RedditComment? {
        allComments.forEach { comment ->
            if (comment.fullname == fullname) {
                return comment
            }
        }
        return null
    }

    /**
     * Goes through [comments] and checks if a comments score is below the users threshold or if
     * Reddit has specified that it should be hidden.
     *
     * Comments with [RedditComment.isCollapsed] set to true children are removed
     */
    private fun checkAndSetHiddenComments(comments: List<RedditComment>): List<RedditComment> {
        val commentsToRemove: MutableList<RedditComment> = ArrayList()
        val hideThreshold = Settings.getAutoHideScoreThreshold()

        comments.forEach { comment ->
            if (hideThreshold >= comment.score || comment.isCollapsed) {
                // If we got here from the score threshold make sure collapsed is set to true
                comment.isCollapsed = true
                commentsToRemove.addAll(getShownReplies(comment))
            }
        }

        return comments.toMutableList().apply {
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

    /**
     * Finds the parent comment of a comment
     *
     * @param comment The comment to find a parent for
     * @return The parent comment, or null if the comment has no parent
     */
    private fun findParent(comment: RedditComment): RedditComment? {
        val depth = comment.depth

        // On posts with a lot of comments the last comment is often a "771 more comments" which is a
        // top level comment, which means it won't have a parent so it's no point in trying to find it
        if (depth == 0) {
            return null
        }

        val commnts = comments.value ?: return null

        val pos = commnts.indexOf(comment)

        // The parent is the first comment upwards in the list that has a lower depth
        for (i in pos - 1 downTo 0) {
            val c = commnts[i]
            if (c.depth < depth) {
                return c
            }
        }

        return null
    }
}