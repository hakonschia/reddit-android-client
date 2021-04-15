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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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

    /**
     * The amount of times this worker has checked an inbox
     */
    private var counter = 0

    /**
     * The counter for the notification IDs
     */
    private var inboxIdNotificationCounter = 0

    /**
     * Map mapping a comment ID ([RedditMessage.id]) to its notification ID
     */
    private val notifications: MutableMap<String, Int> = HashMap()

    override suspend fun doWork(): Result {
        // Get all messages every 10 times. This is to ensure that our local inbox isn't too much out
        // of sync, as if we always only get the unread messages then messages read somewhere else
        // won't be retrieved/appear in the inbox
        val response = if (counter % 10 == 0) {
            api.messages().inbox()
        } else {
            api.messages().unread()
        }

        counter++

        // "Unreachable code", well clearly it isn't, Android Studio
        return when (response) {
            is ApiResponse.Success -> {
                // TODO this should also remove previous notifications if they are now seen?

                val messages = response.value
                val previousMessages = messagesDao.getMessagesById(messages.map { it.id })

                filterNewAndNotSeenMessages(previousMessages, messages).forEach { createInboxNotification(it) }

                // Mark all new messages as now seen
                messages.forEach { it.isSeen = true }
                messagesDao.insertAll(messages)

                return Result.success()
            }

            is ApiResponse.Error -> {
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

        // Only open comments, we don't have anything to do for messages
        // How I want this to work:
        // 1. When using the app, this should open a new activity to display the post. When back is pressed
        // or swiped away it should resume where we were when we opened it (on the emulator this works, not on my phone)
        // 2. When app is not open, just open the activity and when you exit you exit completely (it is like this now)
        val pendingIntent = if (message.wasComment) {
            val intent = Intent(applicationContext, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, message.context)
            }
            PendingIntent.getActivity(applicationContext, 0, intent, 0)
        } else {
            null
        }

        val htmlMessage = Html.fromHtml(message.bodyHtml, Html.FROM_HTML_MODE_COMPACT)

        val notification = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_INBOX_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                // If the message is short then the big text won't be shown until expanded, which just
                // looks weird, so set it on the context text as well
                .setContentText(htmlMessage)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(htmlMessage))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                // Expected time is milliseconds, createdAt is in seconds. This gives the notification
                // the time the comment was posted, not when the notification was created
                .setWhen(message.createdAt * 1000L)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            // If this message already has an ID it will be updated, otherwise create a new ID
            val id = notifications[message.id] ?: inboxIdNotificationCounter++
            notify(id, notification)
            notifications[message.id] = id
        }
    }
}