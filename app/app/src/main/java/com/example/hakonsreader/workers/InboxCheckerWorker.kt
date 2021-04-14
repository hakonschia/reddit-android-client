package com.example.hakonsreader.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    private var inboxNotificationCounter = 0
    private val notifications = HashMap<String, Int>()

    override suspend fun doWork(): Result {
        // Get all messages every 10 times. This is to ensure that our local inbox isn't too much out
        // of sync, as if we always only get the unread messages then messages that read somewhere else
        // won't be retrieved
        val response = if (counter % 10 == 0) {
            api.messages().inbox()
        } else {
            api.messages().unread()
        }

        counter++

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
        // Only show if this message doesn't have a shown notification already
        if (notifications[message.id] != null) {
            return
        }

        val title = if (message.wasComment) {
            applicationContext.getString(R.string.notificationInboxCommentReplyTitle, message.author)
        } else {
            applicationContext.getString(R.string.notificationInboxMessageTitle, message.author)
        }

        // Only open comments, we don't have anything to do for messages
        val pendingIntent = if (message.wasComment) {
            val intent = Intent(applicationContext, DispatcherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, message.context)
            }
            PendingIntent.getActivity(applicationContext, 0, intent, 0)
        } else {
            null
        }

        val builder = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_INBOX_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                // Expected time is milliseconds, createdAt is in seconds. This gives the notification
                // the time the comment was posted, not when the notification was created
                .setWhen(message.createdAt * 1000L)
                .setContentTitle(title)
                // TODO this should show the "raw" text, without any markdown formatting
                .setContentText(message.body)
                .setContentIntent(pendingIntent)
                // Removes the notification when clicked
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(applicationContext)) {
            val id = inboxNotificationCounter++
            notify(id, builder.build())
            notifications[message.id] = id
        }
    }
}