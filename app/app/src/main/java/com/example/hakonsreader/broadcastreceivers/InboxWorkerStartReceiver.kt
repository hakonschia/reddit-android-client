package com.example.hakonsreader.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.workers.InboxCheckerWorker
import java.util.concurrent.TimeUnit

/**
 * Broadcast receiver which calls [startInboxWorker] to start
 */
class InboxWorkerStartReceiver : BroadcastReceiver() {
    companion object {

        /**
         * The name of the Worker responsible for checking inbox messages
         */
        private const val WORKER_INBOX = "worker_inbox"

        /**
         * Enqueues a unique periodic request to [WorkManager] that runs an [InboxCheckerWorker].
         *
         * The interval will be [Settings.inboxUpdateFrequency]. If this returns a negative value then
         * no work will be enqueued and any potential worker active will be cancelled
         */
        fun startInboxWorker(context: Context) {
            val updateFrequency = Settings.inboxUpdateFrequency()
            if (updateFrequency <= 0) {
                WorkManager.getInstance(context).cancelUniqueWork(WORKER_INBOX)
            } else {
                val inboxRequest = PeriodicWorkRequestBuilder<InboxCheckerWorker>(updateFrequency.toLong(), TimeUnit.MINUTES)
                        .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORKER_INBOX, ExistingPeriodicWorkPolicy.REPLACE, inboxRequest)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> startInboxWorker(context)
        }
    }
}