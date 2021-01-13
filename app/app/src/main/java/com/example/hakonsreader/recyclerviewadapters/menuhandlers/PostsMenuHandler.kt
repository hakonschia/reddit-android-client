package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.DispatcherActivity
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.Util
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * Shows the extra popup for posts. Based on the user status, a different popup is shown
 * (ie. if the logged in user is the post poster a different popup is shown)
 *
 * @param view The view clicked
 * @param post The post the popup is for
 */
fun showPopupForPost(view: View, post: RedditPost?) {
    // If the menu is clicked before the post loads, it will be passed as null
    if (post == null) {
        return
    }

    if (App.get().isUserLoggedIn()) {
        showPopupForPostExtraForLoggedInUser(view, post)
    } else {
        showPopupForPostExtraForNonLoggedInUser(view, post)
    }
}


/**
 * Shows the extra popup for posts for when the post is by the logged in user
 *
 * @param view The view clicked (where the popup will be attached)
 * @param post The post the popup is for
 */
private fun showPopupForPostExtraForLoggedInUser(view: View, post: RedditPost) {
    val user = App.storedUser
    val menu = PopupMenu(view.context, view)
    menu.inflate(R.menu.post_extra_generic_for_all_users)
    menu.inflate(R.menu.post_extra_for_logged_in_users)

    // Add menus depending on if the logged in user is the poster of the post
    if (post.author.equals(user?.username, ignoreCase = true)) {
        menu.inflate(R.menu.post_extra_by_user)
    } else {
        menu.inflate(R.menu.post_extra_not_by_user)
    }

    if (post.isUserMod) {
        menu.inflate(R.menu.post_extra_user_is_mod)

        // Set text to "Undistinguish"
        if (post.isMod()) {
            val modItem = menu.menu.findItem(R.id.menuDistinguishPostAsMod)
            modItem.setTitle(R.string.postRemoveModDistinguish)
        }

        // Set text to "Unsticky"
        if (post.isStickied) {
            val modItem = menu.menu.findItem(R.id.menuStickyPost)
            modItem.setTitle(R.string.postRemoveSticky)
        }
    }

    // Default is "Save post", if post already is saved, change the text
    if (post.isSaved) {
        val savedItem = menu.menu.findItem(R.id.menuSaveOrUnsavePost)
        savedItem.title = view.context.getString(R.string.unsavePost)
    }

    // For self posts it doesn't make sense to have both the copy links, as they point to the same thing
    if (post.isSelf || post.crossposts?.get(0)?.isSelf == true) {
        menu.menu.removeItem(R.id.menuCopyPostContent)
    }

    menu.menu.findItem(R.id.menuPostAddSubredditToFilter).title = view.context.getString(R.string.postMenuAddSubredditToFilter, post.subreddit)

    menu.setOnMenuItemClickListener { item: MenuItem ->
        // TODO add delete post, edit post (if selftext and logged in user is poster)
        return@setOnMenuItemClickListener when (item.itemId) {
            R.id.menuSaveOrUnsavePost -> { savePostOnClick(view, post); true }
            R.id.menuDistinguishPostAsMod -> { distinguishAsModOnClick(view, post); true }
            R.id.menuStickyPost -> { stickyOnClick(view, post); true }
            R.id.menuBlockUser -> { blockUserOnClick(view, post); true }
            R.id.menuCopyPostLink -> { copyPostLinkOnClick(view, post); true }
            R.id.menuCopyPostContent -> { copyPostContentOnClick(view, post); true }
            R.id.menuViewOnRemoveddit -> { viewOnRemoveddit(view.context, post); true }
            R.id.menuPostAddSubredditToFilter -> { filterSubredditOnClick(post.subreddit); true }
            else -> false
        }
    }

    val menuHelper = MenuPopupHelper(view.context, menu.menu as MenuBuilder, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
}

/**
 * Shows the extra popup for posts for when the post is NOT by the logged in user
 *
 * @param view The view clicked (where the popup will be attached)
 * @param post The post the popup is for
 */
fun showPopupForPostExtraForNonLoggedInUser(view: View, post: RedditPost) {
    val menu = PopupMenu(view.context, view)
    menu.inflate(R.menu.post_extra_generic_for_all_users)

    menu.menu.findItem(R.id.menuPostAddSubredditToFilter).title = view.context.getString(R.string.postMenuAddSubredditToFilter, post.subreddit)

    menu.setOnMenuItemClickListener { item: MenuItem ->
        return@setOnMenuItemClickListener when (item.itemId) {
            R.id.menuCopyPostLink -> { copyPostLinkOnClick(view, post); true }
            R.id.menuCopyPostContent -> { copyPostContentOnClick(view, post); true }
            R.id.menuViewOnRemoveddit -> { viewOnRemoveddit(view.context, post); true }
            R.id.menuPostAddSubredditToFilter -> { filterSubredditOnClick(post.subreddit); true }
            else -> false
        }
    }

    val menuHelper = MenuPopupHelper(view.context, menu.menu as MenuBuilder, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
}


private fun savePostOnClick(view: View, post: RedditPost) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        val save = !post.isSaved
        val response = if (save) {
            api.post(post.id).save()
        } else {
            api.post(post.id).unsave()
        }

        when (response) {
            is ApiResponse.Success -> {
                post.isSaved = save
                val saveString = if (save) {
                    R.string.postSaved
                } else {
                    R.string.postUnsaved
                }
                Snackbar.make(view, saveString, BaseTransientBottomBar.LENGTH_SHORT).show()
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

private fun distinguishAsModOnClick(view: View, post: RedditPost) {
    val api = App.get().api

    // TODO this should update the UI (need to notify either PostActivity or the adapter)
    CoroutineScope(IO).launch {
        val response = if (post.isMod()) {
            api.post(post.id).removeModDistinguish()
        } else {
            api.post(post.id).distinguishAsMod()
        }

        when (response) {
            is ApiResponse.Success -> {
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

private fun stickyOnClick(view: View, post: RedditPost) {
    val api = App.get().api

    // TODO this should update the UI (need to notify either PostActivity or the adapter)
    CoroutineScope(IO).launch {
        val response = if (post.isStickied) {
            api.post(post.id).unsticky()
        } else {
            api.post(post.id).sticky()
        }

        when (response) {
            is ApiResponse.Success -> {
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

private fun blockUserOnClick(view: View, post: RedditPost) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        when (val response = api.user(post.author).block()) {
            is ApiResponse.Success -> Snackbar.make(view, R.string.userBlocked, BaseTransientBottomBar.LENGTH_SHORT).show()
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

/**
 * Copies the link to the post itself to the clipboard
 *
 * @see copyPostContentOnClick
 */
private fun copyPostLinkOnClick(view: View, post: RedditPost) {
    val url = "https://reddit.com" + post.permalink
    val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Reddit post link", url)
    clipboard.setPrimaryClip(clip)
    Snackbar.make(view, R.string.linkCopied, BaseTransientBottomBar.LENGTH_SHORT).show()
}

/**
 * Copies the link to the content of the post to the clipboard
 *
 * @see copyPostLinkOnClick
 */
private fun copyPostContentOnClick(view: View, post: RedditPost) {
    val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Reddit post content link", post.url)
    clipboard.setPrimaryClip(clip)
    Snackbar.make(view, R.string.linkCopied, BaseTransientBottomBar.LENGTH_SHORT).show()
}

private fun viewOnRemoveddit(context: Context, post: RedditPost) {
    val url = "https://removeddit.com" + post.permalink
    val intent = Intent(context, DispatcherActivity::class.java).apply {
        putExtra(DispatcherActivity.URL_KEY, url)
    }
    context.startActivity(intent)
}

private fun filterSubredditOnClick(subredditName: String) {
    App.get().addSubredditToPostFilters(subredditName)
}