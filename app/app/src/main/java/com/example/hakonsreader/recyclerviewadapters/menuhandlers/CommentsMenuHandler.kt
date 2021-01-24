package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.fragments.bottomsheets.PeekCommentBottomSheet
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows the popup for comments for when the comment is posted by the user currently logged in
 *
 * @param view The view clicked (where the popup will be attached)
 * @param comment The comment the popup is for
 * @param adapter The RecyclerView adapter the comment is in
 */
@SuppressWarnings("RestrictedApi")
fun showPopupForCommentExtraForLoggedInUser(view: View, comment: RedditComment, adapter: CommentsAdapter) {
    val user = App.storedUser
    val menu = PopupMenu(view.context, view)
    menu.inflate(R.menu.comment_extra_generic_for_all_users)
    menu.inflate(R.menu.comment_extra_for_logged_in_users)

    // Add menus depending on if the logged in user is the poster of the comment
    if (comment.author == user?.username) {
        menu.inflate(R.menu.comment_extra_by_user)
    } else {
        menu.inflate(R.menu.comment_extra_for_logged_in_users_comment_not_by_user)
    }

    // Add mod specific if user is a mod in the subreddit the post is in
    if (comment.isUserMod) {
        // Only top level comments can be stickied
        val stickyMenu = if (comment.depth == 0) {
            R.menu.comment_extra_by_user_user_is_mod
        } else {
            R.menu.comment_extra_by_user_user_is_mod_no_sticky
        }
        menu.inflate(stickyMenu)

        // Set text to "Undistinguish"
        if (comment.isMod()) {
            val modItem = menu.menu.findItem(R.id.menuDistinguishCommentAsMod)
            modItem.setTitle(R.string.commentRemoveModDistinguish)
        }

        // Set text to "Remove sticky"
        if (comment.depth == 0 && comment.isStickied) {
            menu.menu.findItem(R.id.menuStickyComment)?.setTitle(R.string.commentRemoveSticky)
        }
    }

    // Default is "Save comment", if comment already is saved, change the text
    if (comment.isSaved) {
        val savedItem = menu.menu.findItem(R.id.menuSaveOrUnsaveComment)
        savedItem.title = view.context.getString(R.string.unsaveComment)
    }

    val parentComment = adapter.getCommentById(comment.parentId)
    if (parentComment == null) {
        // No parent comment, remove the "Peek parent" option
        menu.menu.removeItem(R.id.menuPeekParentComment)
    }

    menu.menu.findItem(R.id.menuBlockUser).title = view.context.getString(R.string.blockUser, comment.author)

    menu.setOnMenuItemClickListener { item: MenuItem ->
        return@setOnMenuItemClickListener when (item.itemId) {
            R.id.menuDeleteComment -> { deleteCommentOnClick(view, comment); true }
            R.id.menuSaveOrUnsaveComment -> { saveCommentOnClick(view, comment); true }
            R.id.menuDistinguishCommentAsMod -> { distinguishAsModOnclick(view, comment, adapter); true }
            R.id.menuStickyComment -> { stickyOnClick(view, comment, adapter); true }
            R.id.menuBlockUser -> { blockUserOnClick(view, comment.author); true }
            R.id.menuShowCommentChain -> { adapter.commentIdChain = comment.id; true }
            R.id.menuCopyCommentLink -> { copyCommentLinkOnClick(view, comment); true }
            R.id.menuPeekParentComment -> { peekParentOnClick(view.context, parentComment!!); true }
            else -> false
        }
    }

    val menuHelper = MenuPopupHelper(view.context, menu.menu as MenuBuilder, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
}

/**
 * Shows the popup for comments for when the comment is NOT posted by the user currently logged in
 *
 * @param view The view clicked (where the popup will be attached)
 * @param comment The comment the popup is for
 * @param adapter The RecyclerView adapter the comment is in
 */
@SuppressWarnings("RestrictedApi")
fun showPopupForCommentExtraForNonLoggedInUser(view: View, comment: RedditComment, adapter: CommentsAdapter) {
    val menu = PopupMenu(view.context, view)
    menu.inflate(R.menu.comment_extra_generic_for_all_users)

    val parentComment = adapter.getCommentById(comment.parentId)
    if (parentComment == null) {
        // No parent comment, remove the "Peek parent" option
        menu.menu.removeItem(R.id.menuPeekParentComment)
    }

    menu.setOnMenuItemClickListener { item: MenuItem ->
        return@setOnMenuItemClickListener when (item.itemId) {
            R.id.menuShowCommentChain -> { adapter.commentIdChain = comment.id; true }
            R.id.menuCopyCommentLink -> { copyCommentLinkOnClick(view, comment); true }
            R.id.menuPeekParentComment -> { peekParentOnClick(view.context, parentComment!!); true }
            else -> false
        }
    }

    val menuHelper = MenuPopupHelper(view.context, menu.menu as MenuBuilder, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
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
                        is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
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
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
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
                is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
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
                is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
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

fun peekParentOnClick(context: Context, comment: RedditComment) {
    context as AppCompatActivity
    val bottomSheet = PeekCommentBottomSheet()
    bottomSheet.comment = comment
    bottomSheet.show(context.supportFragmentManager, "peekParentBottomSheet")
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

