package com.example.hakonsreader.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
import com.example.hakonsreader.activities.MainActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.broadcastreceivers.InboxNotificationReceiver
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_ID_INBOX_STATUS
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS
import com.example.hakonsreader.misc.Settings
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
        private val settings: Settings,
        private val api: RedditApi,
        private val messagesDao: RedditMessagesDao,
) : CoroutineWorker(context, workerParams) {

    companion object {
        /**
         * The name of the SharedPreferences used to persist values for this Worker
         */
        private const val PREFS_NAME = "inboxWorkerPreferences"

        /**
         * The key used to store the counter of the inbox in SharedPreferences
         */
        private const val PREFS_COUNTER = "prefs_counter"

        /**
         * The key used to store the ID counter of the inbox notifications
         */
        private const val PREFS_INBOX_NOTIFICATION_ID = "prefs_inboxNotificationId"
    }


    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val counter = prefs.getInt(PREFS_COUNTER, 0)

        // Get all messages every 10 times. This is to ensure that our local inbox isn't too much out
        // of sync, as if we always only get the unread messages then messages read somewhere else
        // won't be retrieved/appear in the inbox
        val fetchAll = counter % 10 == 0

        val response = if (fetchAll) {
            api.messages().inbox()
        } else {
            api.messages().unread()
        }

        prefs.edit().putInt(PREFS_COUNTER, counter + 1).apply()

        return when (response) {
            is ApiResponse.Success -> {
                if (settings.devShowInboxNotifications()) {
                    createDeveloperNotification(counter)
                }

                val messages = response.value
                removeNonNewNotifications(messages)

                if (settings.showInboxNotifications()) {
                    // Retrieve the messages that we just retrieved from the local DB (if they have been retrieved earlier)
                    val previousMessages = messagesDao.getMessagesById(messages.map { it.id })
                    // Create notification for those messages not already seen
                    filterNewAndNotSeenMessages(previousMessages, messages).forEach { createInboxNotification(it, prefs) }
                }

                // Mark all new messages as now seen
                messages.forEach { it.isSeen = true }
                messagesDao.insertAll(messages)

                // If we only fetch unread, and it is empty, we can safely mark all messages as now read
                // We can only safely do this if we only got the unread, as if we got the inbox it has a
                // limit to how many messages are retrieved (25 by default), and in the case that all
                // those are read/unread how do we know if the messages before those are read/unread? If we have those
                // in the db they might be marked incorrectly
                val allMessagesRead = !fetchAll && messages.none { it.isNew }
                if (allMessagesRead) {
                    messagesDao.markAllRead()
                }

                Result.success()
            }

            is ApiResponse.Error -> {
                if (settings.devShowInboxNotifications()) {
                    createDeveloperNotification(counter, response.throwable)
                }
                Result.failure()
            }
        }
    }

    /**
     * Removes notifications that are now marked as not new ([RedditMessage.isNew] is false)
     *
     * @param messages The list of messages to check
     */
    private fun removeNonNewNotifications(messages: List<RedditMessage>) {
        val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.activeNotifications
                .filter {
                    // This is probably not strictly necessary, but just to be sure that if we later
                    // add other channels which also use IDs as tags we don't want to remove those
                    return@filter if (Build.VERSION.SDK_INT >= 26) {
                        it.notification.channelId == App.NOTIFICATION_CHANNEL_INBOX_ID
                    } else {
                        // Channels are only a thing on >= 26, so on lower devices this won't filter anything
                        true
                    }
                }
                .forEach { notification ->
                    val message = messages.find { it.id == notification.tag }

                    // message != null means there is a notification for the message
                    // !.isNew means it is now seen, so remove the notification
                    if (message != null && !message.isNew) {
                        notificationManager.cancel(message.id, notification.id)
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
     * @param preferences The preferences used to hold the notification ID
     */
    private fun createInboxNotification(message: RedditMessage, preferences: SharedPreferences) {
        val title = if (message.wasComment) {
            applicationContext.getString(R.string.notificationInboxCommentReplyTitle, message.author)
        } else {
            applicationContext.getString(R.string.notificationInboxMessageTitle, message.author)
        }

        val id = preferences.getInt(PREFS_INBOX_NOTIFICATION_ID, 0)
        // Lets hope no one gets 2.1 billion messages :)
        preferences.edit().putInt(PREFS_INBOX_NOTIFICATION_ID, id + 1).apply()

        val (contentIntent, markAsReadActionIntent) = createIntents(message, id)

        @Suppress("DEPRECATION") val htmlMessage = if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message.bodyHtml, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(message.bodyHtml)
        }

        val notification = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_INBOX_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                // If the message is short then the big text won't be shown until expanded, which just
                // looks weird, so set it on the context text as well
                .setContentText(htmlMessage)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(htmlMessage))
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_markunread_mailbox_24dp, "Mark read", markAsReadActionIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                // Expected time is milliseconds, createdAt is in seconds. This gives the notification
                // the time the comment was posted, not when the notification was created
                .setWhen(message.createdAt * 1000L)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            // Use the message ID as the tag. Ideally we would just convert this from base36 to base10
            // and use it as the ID itself, but the value is above Int.MAX_VALUE
            notify(id, notification)
        }
    }

    /**
     * Wrapper class for a content intent and an action intent
     *
     * @param contentIntent The intent for the content of the notification (when the notification is clicked)
     * @param markAsReadActionIntent The action intent for marking a message as read through a notification
     * action
     */
    private data class Intents(
            val contentIntent: PendingIntent?,
            val markAsReadActionIntent: PendingIntent?,
    )

    /**
     * Create intents for a message
     *
     * @param message The message to create intents for
     * @param id The ID of of the notification
     */
    private fun createIntents(message: RedditMessage, id: Int): Intents {
        val actionIntent = Intent(applicationContext, InboxNotificationReceiver::class.java).apply {
            putExtra(InboxNotificationReceiver.EXTRAS_MESSAGE_ID, message.id)
            putExtra(InboxNotificationReceiver.EXTRAS_WAS_COMMENT, message.wasComment)
            putExtra(InboxNotificationReceiver.EXTRAS_NOTIFICATION_ID, id)
        }
        val pendingActionIntent = PendingIntent.getBroadcast(applicationContext, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Only open comments, we don't have anything to do for messages yet
        return if (message.wasComment) {
            val contentIntent = Intent(applicationContext, DispatcherActivity::class.java).apply {
                putExtra(DispatcherActivity.EXTRAS_URL_KEY, message.context)

                // Opens the activity so that it either starts the app (if not opened) or opens the
                // activity "above" the current, so that we can swipe it away/press back
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
     *
     * @param failCause The reason the request failed. If the request did not fail this can be omitted
     */
    private fun createDeveloperNotification(counter: Int, failCause: Throwable? = null) {
        // Format as "17:45"
        val format = SimpleDateFormat("kk:mm", Locale.getDefault())
        val date = Date(System.currentTimeMillis())

        // This intent will simply open the app without restarting the active activity, or start the app
        // as normal if it isn't opened (ie. this provides the same functionality as clicking the app
        // from a launcher)
        val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val intent = PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(applicationContext, App.NOTIFICATION_CHANNEL_DEVELOPER_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(applicationContext.getString(R.string.notificationDeveloperMessagesRetrievedContent, format.format(date)))
                .setStyle(NotificationCompat.BigTextStyle()
                        .setSummaryText("Couner = $counter")
                        .bigText(if (failCause != null) "Request failed:\n ${failCause.printStackTrace()}" else "Counter = $counter")
                )
                .setContentIntent(intent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS, DEVELOPER_NOTIFICATION_ID_INBOX_STATUS, notification)
        }
    }
}