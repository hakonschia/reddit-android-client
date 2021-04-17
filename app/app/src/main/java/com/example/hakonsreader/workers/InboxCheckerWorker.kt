package com.example.hakonsreader.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.broadcastreceivers.InboxNotificationReceiver
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_ID_INBOX_STATUS
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.states.AppState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

/**
 * A Worker that checks a users inbox messages.
 */
@HiltWorker
class InboxCheckerWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val api: RedditApi,
        private val messagesDao: RedditMessagesDao
) : CoroutineWorker(context, workerParams) {

    companion object {

        /**
         * The counter for how many times the inbox has been checked
         */
        private var counter = 0

        /**
         * The counter for the notification IDs
         */
        private var inboxIdNotificationCounter = 0
    }


    override suspend fun doWork(): Result {
        // Get all messages every 10 times. This is to ensure that our local inbox isn't too much out
        // of sync, as if we always only get the unread messages then messages read somewhere else
        // won't be retrieved/appear in the inbox
        // Never get the full inbox if data saving is on
        val response = if (counter++ % 10 == 0 && !Settings.dataSavingEnabled()) {
            api.messages().inbox()
        } else {
            api.messages().unread()
        }

        // "Unreachable code", well clearly it isn't, Android Studio
        return when (response) {
            is ApiResponse.Success -> {
                // TODO this should also remove previous notifications if they are now seen?

                if (AppState.isDevMode) {
                    createDeveloperNotification(false)
                }

                val messages = response.value
                val previousMessages = messagesDao.getMessagesById(messages.map { it.id })

                filterNewAndNotSeenMessages(previousMessages, messages).forEach { createInboxNotification(it) }

                // Mark all new messages as now seen
                messages.forEach { it.isSeen = true }
                messagesDao.insertAll(messages)

                return Result.success()
            }

            is ApiResponse.Error -> {
                if (AppState.isDevMode) {
                    createDeveloperNotification(true)
                }
                return Result.failure()
            }
        }
    }

    /**
     * Filter out messages where [RedditMessage.isNew] is true and [RedditMessage.isSeen] is false
     * based on two lists
     *
     * @param old The old list of messages. [RedditMessage.isSeen] in the returned list will be taken
     * from these messages
     * @param new The new messages retrieved
     */
    private fun filterNewAndNotSeenMessages(old: List<RedditMessage>, new: List<RedditMessage>): List<RedditMessage> {
        // Group together both lists. This gives a list where the old messages will appear
        // before the new, and distinct will always select the first appearing message
        // This is done to get a list of all messages in `old` and in `new` without duplicates, where the
        // old ones are preferred over new
        return (old + new).distinctBy { it.id }.filter { it.isNew && !it.isSeen }
    }

    /**
     * Creates a notification for an inbox message
     *
     * @param message The message to show the notification for
     */
    private fun createInboxNotification(message: RedditMessage) {
        val title = if (message.wasComment) {
            applicationContext.getString(R.string.notificationInboxCommentReplyTitle, message.author)
        } else {
            applicationContext.getString(R.string.notificationInboxMessageTitle, message.author)
        }

        val id = inboxIdNotificationCounter++

        val (contentIntent, actionIntent) = createIntents(message, id)
        val htmlMessage = Html.fromHtml(message.bodyHtml, Html.FROM_HTML_MODE_COMPACT)

        val notification = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_INBOX_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                // If the message is short then the big text won't be shown until expanded, which just
                // looks weird, so set it on the context text as well
                .setContentText(htmlMessage)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(htmlMessage))
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_markunread_mailbox_24dp, "Mark read", actionIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                // Expected time is milliseconds, createdAt is in seconds. This gives the notification
                // the time the comment was posted, not when the notification was created
                .setWhen(message.createdAt * 1000L)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(id, notification)
        }
    }

    /**
     * Wrapper class for a content intent and an action intent
     */
    private data class Intents(
            val contentIntent: PendingIntent?,
            val actionIntent: PendingIntent?
    )

    /**
     * Create intents for a message
     */
    private fun createIntents(message: RedditMessage, notificationId: Int): Intents {
        val actionIntent = Intent(applicationContext, InboxNotificationReceiver::class.java).apply {
            putExtra(InboxNotificationReceiver.EXTRAS_MESSAGE_ID, message.id)
            putExtra(InboxNotificationReceiver.EXTRAS_WAS_COMMENT, message.wasComment)
            putExtra(InboxNotificationReceiver.EXTRAS_NOTIFICATION_ID, notificationId)
        }
        val pendingActionIntent = PendingIntent.getBroadcast(applicationContext, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return if (message.wasComment) {
            // Only open comments, we don't have anything to do for messages
            // How I want this to work:
            // 1. When using the app, this should open a new activity to display the post. When back is pressed
            // or swiped away it should resume where we were when we opened it (on the emulator this works, not on my phone)
            // 2. When app is not open, just open the activity and when you exit you exit completely (it is like this now)
            val contentIntent = Intent(applicationContext, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, message.context)
            }

            Intents(
                    PendingIntent.getActivity(applicationContext, 0, contentIntent, 0),
                    pendingActionIntent
            )
        } else {
            Intents(null, pendingActionIntent)
        }
    }


    /**
     * Creates, or updates, a notification in the developer mode channel to say when the last time
     * the messages were retrieved
     */
    private fun createDeveloperNotification(failed: Boolean) {
        // Format as "17:45"
        val format = SimpleDateFormat("kk:mm", Locale.getDefault())
        val date = Date(System.currentTimeMillis())

        val content = StringBuilder()
                .append(if (failed) "Request failed. " else " ")
                .append("Counter = $counter")
                .toString()

        val notification = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_DEVELOPER_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(applicationContext.getString(R.string.notificationDeveloperMessagesRetrievedContent, format.format(date)))
                .setContentText(content)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS, DEVELOPER_NOTIFICATION_ID_INBOX_STATUS, notification)
        }
    }
}