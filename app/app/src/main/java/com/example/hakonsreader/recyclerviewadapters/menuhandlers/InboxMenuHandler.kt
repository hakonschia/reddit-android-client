package com.example.hakonsreader.recyclerviewadapters.menuhandlers

import android.view.View
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.github.zawadz88.materialpopupmenu.popupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

fun showInboxMenu(view: View, redditMessage: RedditMessage?, api: RedditApi, messagesDao: RedditMessagesDao) {
    if (redditMessage == null) {
        return
    }

    val context = view.context

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            item {
                labelRes = if (redditMessage.isNew) {
                    R.string.inboxMenuMarkRead
                } else {
                    R.string.inboxMenuMarkUnread
                }

                icon = R.drawable.ic_markunread_mailbox_24dp
                callback = { markUnreadOnClick(view, redditMessage, api, messagesDao) }
            }
        }
    }.show(context, view)
}

private fun markUnreadOnClick(view: View, message: RedditMessage, api: RedditApi, messagesDao: RedditMessagesDao) {
    CoroutineScope(IO).launch {
        val markRead = message.isNew
        message.isNew = !markRead

        messagesDao.insert(message)

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
                messagesDao.insert(message)
                handleGenericResponseErrors(view, resp.error, resp.throwable)
            }
        }
    }
}