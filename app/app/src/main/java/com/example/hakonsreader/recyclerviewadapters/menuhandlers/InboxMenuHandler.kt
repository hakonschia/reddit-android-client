package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

@SuppressWarnings("RestrictedApi")
fun showInboxMenu(view: View, redditMessage: RedditMessage?) {
    if (redditMessage == null) {
        return
    }

    val menu = PopupMenu(view.context, view)
    menu.inflate(R.menu.inbox_menu)

    // Set as "Mark read"
    if (redditMessage.isNew) {
        menu.menu.findItem(R.id.menuInboxMarkUnread).title = view.context.getString(R.string.inboxMenuMarkRead)
    }

    menu.setOnMenuItemClickListener {
        return@setOnMenuItemClickListener when (it.itemId) {
            R.id.menuInboxMarkUnread -> { markUnreadOnClick(view, redditMessage); true }
            else -> false
        }
    }

    val menuHelper = MenuPopupHelper(view.context, menu.menu as MenuBuilder, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
}

private fun markUnreadOnClick(view: View, message: RedditMessage) {
    val api = App.get().api
    val db = RedditDatabase.getInstance(view.context)

    CoroutineScope(IO).launch {
        val markRead = message.isNew
        message.isNew = !markRead

        db.messages().insert(message)

        val resp = if (markRead) {
            api.messages().markRead(message)
        } else {
            api.messages().markUnread(message)
        }

        when (resp) {
            // Do nothing on success as it is assumed success
            is ApiResponse.Success -> {}
            is ApiResponse.Error -> {
                // Rollback to original
                message.isNew = markRead
                db.messages().insert(message)
                Util.handleGenericResponseErrors(view, resp.error, resp.throwable)
            }
        }
    }
}