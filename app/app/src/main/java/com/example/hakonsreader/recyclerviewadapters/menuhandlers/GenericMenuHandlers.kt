package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.view.View
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.Util
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


/**
 * Blocks a Reddit user. Requires a logged in user
 *
 * A snackbar is shown, which when the block was successful will show a button to unblock the user
 *
 * @param view The view to attach the snackbar to
 * @param username The username of the user to block
 */
fun blockUserOnClick(view: View, username: String) {
    val api = App.get().api
    val loggedInUser = App.storedUser ?: return

    CoroutineScope(IO).launch {
        when (val response = api.user(username).block()) {
            is ApiResponse.Success -> {
                val userBlockedString = view.context.getString(R.string.userBlocked, username)

                Snackbar.make(view, userBlockedString, BaseTransientBottomBar.LENGTH_LONG)
                        .setAction(R.string.unblockUser) {
                            unblockUser(view, username, loggedInUser)
                        }
                        .show()
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}

/**
 * Unblocks a user
 *
 * @param view The view to attach the snackbar to
 * @param username The username to unblock
 * @param loggedInUser The logged in user (the user blocking [username])
 */
fun unblockUser(view: View, username: String, loggedInUser: RedditUser) {
    val api = App.get().api

    CoroutineScope(IO).launch {
        when (val response = api.user(username).unblock(loggedInUser.id)) {
            is ApiResponse.Success -> {
                val userUnblockedString = view.context.getString(R.string.userUnblocked, username)
                Snackbar.make(view, userUnblockedString, BaseTransientBottomBar.LENGTH_SHORT).show()
            }
            is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
        }
    }
}