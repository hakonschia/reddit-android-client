package com.example.hakonsreader.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver for performing actions on inbox notifications. This will mark a message as read
 */
@AndroidEntryPoint
class InboxNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InboxNotificationReceiv"


        /**
         * Extras passed to this receiver to tell the comment ID notification represents
         *
         * The key with this value should be a `boolean`
         */
        const val EXTRAS_MESSAGE_ID = "extras_messageId"

        /**
         * Extras passed to this receiver to tell if the comment ID passed with [EXTRAS_MESSAGE_ID]
         * is for a comment reply (not a direct message)
         *
         * The key with this value should be a `boolean`
         */
        const val EXTRAS_WAS_COMMENT = "extras_wasComment"

        /**
         * Extras passed to this receiver to tell the ID of the notification. This ID will be used
         * to dismiss the notification
         *
         * The key with this value should be an integer
         */
        const val EXTRAS_NOTIFICATION_ID = "extras_notificationId"
    }

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var messagesDao: RedditMessagesDao

    override fun onReceive(context: Context, intent: Intent) {
        val inboxMessageId = intent.extras?.getString(EXTRAS_MESSAGE_ID) ?: return
        val wasComment = intent.extras?.getBoolean(EXTRAS_WAS_COMMENT) ?: return
        val notificationId = intent.extras?.getInt(EXTRAS_NOTIFICATION_ID) ?: return

        CoroutineScope(IO).launch {
            val response = api.messages().markRead(RedditMessage().apply {
                this.wasComment = wasComment
                id = inboxMessageId
            })

            when (response) {
                is ApiResponse.Success -> {
                    with(NotificationManagerCompat.from(context)) {
                        cancel(inboxMessageId, notificationId)
                    }
                    messagesDao.markRead(inboxMessageId)
                }
                is ApiResponse.Error -> {
                    Toast.makeText(context, R.string.inboxNotificationMarkReadFailed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}