package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.fragments.bottomsheets.PeekCommentBottomSheet
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.states.LoggedInState
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows a popup for comments
 *
 * @param view The view clicked (where the popup will be attached)
 * @param comment The comment the popup is for
 * @param adapter The RecyclerView adapter the comment is in
 */
fun showPopupForComments(view: View, comment: RedditComment, adapter: CommentsAdapter) {
    val user = App.get().getUserInfo()?.userInfo
    val context = view.context
    val parentComment = adapter.getCommentById(comment.parentId)

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            if (parentComment != null) {
                item {
                    labelRes = R.string.menuCommentPeekParent
                    icon = R.drawable.ic_visibility_24dp
                    callback = { peekCommentOnClick(context, parentComment) }
                }
            }

            item {
                labelRes = R.string.commentShowChain
                icon = R.drawable.ic_chain
                callback = { adapter.commentIdChain = comment.id }
            }

            item {
                labelRes = R.string.commentCopyLink
                icon = R.drawable.ic_baseline_link_24
                callback = { copyCommentLinkOnClick(view, comment) }
            }
        }

        // Logged in user section
        if (App.get().loggedInState.value is LoggedInState.LoggedIn) {
            section {
                item {
                    labelRes = if (comment.isSaved) {
                        R.string.unsaveComment
                    } else {
                        R.string.saveComment
                    }
                    icon = R.drawable.ic_bookmark_24dp
                    callback = { saveCommentOnClick(view, comment) }
                }

                if (comment.author == user?.username) {
                    item {
                        labelRes = R.string.menuDeleteComment
                        icon = R.drawable.ic_delete_24dp
                        callback = { deleteCommentOnClick(view, comment) }
                    }
                } else {
                    item {
                        label = context.getString(R.string.blockUser, comment.author)
                        icon = R.drawable.ic_baseline_block_24
                        callback = { blockUserOnClick(view, comment.author) }
                    }
                }
            }
        }

        if (comment.isUserMod) {
            section {
                title = context.getString(R.string.commentMenuSectionModeration)
                item {
                    labelRes = if (comment.isMod()) {
                        R.string.postRemoveModDistinguish
                    } else {
                        R.string.postDistinguishAsMod
                    }
                    icon = R.drawable.ic_admin_24px
                    callback = { distinguishAsModOnclick(view, comment, adapter) }
                }

                if (comment.depth == 0) {
                    item {
                        labelRes = if (comment.isStickied) {
                            R.string.commentRemoveSticky
                        } else {
                            R.string.commentSticky
                        }

                        icon = R.drawable.ic_pin_icon_color_24dp
                        callback = { stickyOnClick(view, comment, adapter) }
                    }
                }

                item {
                    if (comment.isLocked) {
                        labelRes = R.string.menuPostUnlock
                        icon = R.drawable.ic_baseline_lock_open_24
                    } else {
                        labelRes = R.string.menuPostLock
                        icon = R.drawable.ic_lock_24dp
                    }

                    callback = { lockListingOnClick(view, comment, adapter) }
                }
            }
        }

    }.show(context, view)
}

/**
 * Listener for delete comment clicks
 */
private fun deleteCommentOnClick(view: View, comment: RedditComment) {
    Dialog(view.context).apply {
        setContentView(R.layout.dialog_confirm_delete_comment)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val api = App.get().api

            CoroutineScope(IO).launch {
                val response = api.comment(comment.id).delete()
                withContext(Main) {
                    when (response) {
                        is ApiResponse.Success -> Snackbar.make(view, R.string.commentDeleted, BaseTransientBottomBar.LENGTH_SHORT).show()
                        is ApiResponse.Error -> handleGenericResponseErrors(view, response.error, response.throwable)
                    }
                }
            }

            dismiss()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener { dismiss() }

        show()
    }
}

/**
 * Convenience method for when "Save comment" or "Unsave comment" has been clicked in a menu.
 *
 *
 * Makes an API request to save or unsave the comment based on the current save state
 *
 * @param view The view clicked (used to attach the snackbar with potential error messages)
 * @param comment The comment to save/unsave. This is updated if the request is successful
 */
private fun saveCommentOnClick(view: View, comment: RedditComment) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        val save = !comment.isSaved

        val response = if (save) {
            api.comment(comment.id).save()
        } else {
            api.comment(comment.id).unsave()
        }

        when (response) {
            is ApiResponse.Success -> {
                comment.isSaved = save
                val saveString = if (save) R.string.commentSaved else R.string.commentUnsaved
                Snackbar.make(view, saveString, BaseTransientBottomBar.LENGTH_SHORT).show()
            }
            is ApiResponse.Error -> handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

/**
 * Click listener for distinguishing/undistinguishing a comment as moderator
 *
 * @param view The popup view
 * @param comment The comment to distinguish
 * @param adapter The adapter the comment is in (to notify for updates)
 */
private fun distinguishAsModOnclick(view: View, comment: RedditComment, adapter: CommentsAdapter) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        val response = if (comment.isMod()) {
            api.comment(comment.id).removeModDistinguish()
        } else {
            api.comment(comment.id).distinguishAsMod()
        }

        withContext(Main) {
            when (response) {
                is ApiResponse.Success -> updateDistinguishAndSticky(comment, response.value, adapter)
                is ApiResponse.Error -> handleGenericResponseErrors(view, response.error, response.throwable)
            }
        }
    }
}

/**
 * Click listener for stickying/unstickying a comment as moderator
 *
 * @param view The popup view
 * @param comment The comment to sticky
 * @param adapter The adapter the comment is in (to notify for updates)
 */
private fun stickyOnClick(view: View, comment: RedditComment, adapter: CommentsAdapter) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        val response = if (comment.isStickied) {
            api.comment(comment.id).unsticky()
        } else {
            api.comment(comment.id).sticky()
        }

        withContext(Main) {
            when (response) {
                is ApiResponse.Success -> updateDistinguishAndSticky(comment, response.value, adapter)
                is ApiResponse.Error -> handleGenericResponseErrors(view, response.error, response.throwable)
            }
        }
    }
}

private fun copyCommentLinkOnClick(view: View, comment: RedditComment) {
    val url = "https://reddit.com" + comment.permalink
    val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Reddit comment link", url)
    clipboard.setPrimaryClip(clip)
    Snackbar.make(view, R.string.linkCopied, BaseTransientBottomBar.LENGTH_SHORT).show()
}

/**
 * Shows a BottomSheet with a comment
 *
 * @param context The context to create the BottomSheet with
 * @param comment The comment to peek
 */
fun peekCommentOnClick(context: Context, comment: RedditComment) {
    context as AppCompatActivity
    PeekCommentBottomSheet().run {
        this.comment = comment
        show(context.supportFragmentManager, "peekParentBottomSheet")
    }
}

/**
 * Updates distinguished/stickied in a comment based on a new comment
 *
 * Must run on the main thread
 *
 * @param oldComment The old/original comment to update in the adapter
 * @param newComment The new comment holding the updated information
 * @param adapter The adapter the comment is in (to notify the update to)
 */
private fun updateDistinguishAndSticky(oldComment: RedditComment, newComment: RedditComment, adapter: CommentsAdapter) {
    oldComment.distinguished = newComment.distinguished
    oldComment.isStickied = newComment.isStickied
    adapter.notifyItemChanged(oldComment)
}

