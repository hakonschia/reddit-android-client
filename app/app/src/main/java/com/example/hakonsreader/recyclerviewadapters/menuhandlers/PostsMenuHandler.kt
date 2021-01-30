package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.Util
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenu
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenuBuilder
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val user = App.storedUser
    val context = view.context

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        // Generic for all users
        section {
            item {
                labelRes = R.string.copyPostLink
                icon = R.drawable.ic_baseline_link_24
                callback = { copyPostLinkOnClick(view, post) }
            }

            item {
                labelRes = R.string.copyPostContentLink
                icon = R.drawable.ic_content_copy_24
                callback = { copyPostContentOnClick(view, post) }
            }

            item {
                label = context.getString(R.string.postMenuAddSubredditToFilter, post.subreddit)
                icon = R.drawable.ic_filter_24px
                callback = { filterSubredditOnClick(post.subreddit) }
            }
        }

        // There is a logged in user, show actions that require logged in users
        // Technically this could be null even if there is
        if (App.get().isUserLoggedIn()) {
            section {
                item {
                    labelRes = if (post.isSaved) {
                        R.string.unsavePost
                    } else {
                        R.string.savePost
                    }
                    icon = R.drawable.ic_bookmark_24dp
                    callback = { savePostOnClick(view, post) }
                }

                // Post by the logged in user (technically if we failed to get user information this
                // could be wrong as a post could have been by a user but we don't know it is)
                if (post.author.equals(user?.username, ignoreCase = true)) {
                    // TODO add edit post (if post is selftext)
                    item {
                        labelRes = R.string.deletePost
                        icon = R.drawable.ic_delete_24dp
                        callback = { deletePostOnClick(view, post) }
                    }

                    item {
                        labelRes = if (post.isSpoiler) {
                            R.string.menuPostUnmarkSpoiler
                        } else {
                            R.string.menuPostMarkSpoiler
                        }

                        // This would be kind of fun to have a car spoiler, but I'm definitely not going
                        // to create that myself
                        icon = R.drawable.ic_help_24dp
                        callback = { markSpoilerOnClick(view, post) }
                    }

                    item {
                        labelRes = if (post.isNsfw) {
                            R.string.menuPostUnmarkNsfw
                        } else {
                            R.string.menuPostMarkNsfw
                        }

                        icon = R.drawable.ic_pin_icon_color_24dp
                        callback = { markNsfwOnClick(view, post) }
                    }
                } else {
                    // Post NOT be logged in user
                    item {
                        label = context.getString(R.string.blockUser, post.author)
                        icon = R.drawable.ic_baseline_block_24
                        callback = { blockUserOnClick(view, post.author) }
                    }
                }
            }
        }

        // Logged in user is mod in the subreddit the post is in
        // (this could be in the if above, but this will just be false if there is no user
        if (post.isUserMod) {
            section {
                title = context.getString(R.string.postMenuSectionModeration)

                item {
                    labelRes = if (post.isMod()) {
                        R.string.postRemoveModDistinguish
                    } else {
                        R.string.postDistinguishAsMod
                    }
                    icon = R.drawable.ic_admin_24px
                    callback = { distinguishAsModOnClick(view, post) }
                }

                item {
                    labelRes = if (post.isStickied) {
                        R.string.postRemoveSticky
                    } else {
                        R.string.postSticky
                    }

                    icon = R.drawable.ic_pin_icon_color_24dp
                    callback = { stickyOnClick(view, post) }
                }

                // Don't create the nsfw/spoiler items if the user is also the poster, as they will
                // have it under the section above already
                // I don't know how to create and return an "item" from a function, so I'll have to just
                // copy the code from above
                if (!post.author.equals(user?.username, ignoreCase = true)) {
                    item {
                        labelRes = if (post.isSpoiler) {
                            R.string.menuPostUnmarkSpoiler
                        } else {
                            R.string.menuPostMarkSpoiler
                        }

                        // This would be kind of fun to have a car spoiler, but I'm definitely not going
                        // to create that myself
                        icon = R.drawable.ic_help_24dp
                        callback = { markSpoilerOnClick(view, post) }
                    }

                    item {
                        labelRes = if (post.isNsfw) {
                            R.string.menuPostUnmarkNsfw
                        } else {
                            R.string.menuPostMarkNsfw
                        }

                        icon = R.drawable.ic_pin_icon_color_24dp
                        callback = { markNsfwOnClick(view, post) }
                    }
                }
            }
        }
    }.show(context, view)
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
    val db = App.get().database

    CoroutineScope(IO).launch {
        val response = if (post.isMod()) {
            post.distinguished = null
            db.posts().update(post)
            api.post(post.id).removeModDistinguish()
        } else {
            post.distinguished = "moderator"
            db.posts().update(post)
            api.post(post.id).distinguishAsMod()
        }

        when (response) {
            is ApiResponse.Success -> {
                response.value
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

private fun stickyOnClick(view: View, post: RedditPost) {
    val api = App.get().api
    val db = App.get().database

    CoroutineScope(IO).launch {
        val newSticky = !post.isStickied
        post.isStickied = newSticky
        db.posts().update(post)

        val response = if (newSticky) {
            api.post(post.id).sticky()
        } else {
            api.post(post.id).unsticky()
        }

        when (response) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                // Revert back
                post.isStickied = !post.isStickied
                db.posts().update(post)
                Util.handleGenericResponseErrors(view, response.error, response.throwable)
            }
        }
    }
}

private fun markNsfwOnClick(view: View, post: RedditPost) {
    val api = App.get().api
    val db = App.get().database

    CoroutineScope(IO).launch {
        val newNsfw = !post.isNsfw
        post.isNsfw = newNsfw
        db.posts().update(post)

        val response = if (newNsfw) {
            api.post(post.id).markNsfw()
        } else {
            api.post(post.id).unmarkNsfw()
        }

        when (response) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                // Revert back
                post.isNsfw = !post.isNsfw
                db.posts().update(post)
                Util.handleGenericResponseErrors(view, response.error, response.throwable)
            }
        }
    }
}

private fun markSpoilerOnClick(view: View, post: RedditPost) {
    val api = App.get().api
    val db = App.get().database

    CoroutineScope(IO).launch {
        val newSpoiler = !post.isSpoiler
        post.isSpoiler = newSpoiler
        db.posts().update(post)

        val response = if (newSpoiler) {
            api.post(post.id).markSpoiler()
        } else {
            api.post(post.id).unmarkSpoiler()
        }

        when (response) {
            is ApiResponse.Success -> { }
            is ApiResponse.Error -> {
                // Revert back
                post.isSpoiler = !post.isSpoiler
                db.posts().update(post)
                Util.handleGenericResponseErrors(view, response.error, response.throwable)
            }
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

private fun filterSubredditOnClick(subredditName: String) {
    App.get().addSubredditToPostFilters(subredditName)
}

/**
 * OnClick for deleting a post
 *
 * A Dialog is shown to confirm the user wants to delete the post
 *
 * @param view The view to attach the snackbar to
 * @param post The post to delete
 */
private fun deletePostOnClick(view: View, post: RedditPost) {
    Dialog(view.context).apply {
        setContentView(R.layout.dialog_confirm_delete_post)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val api = App.get().api

            CoroutineScope(IO).launch {
                val response = api.post(post.id).delete()
                withContext(Main) {
                    when (response) {
                        is ApiResponse.Success -> Snackbar.make(view, R.string.postDeleted, BaseTransientBottomBar.LENGTH_SHORT).show()
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