package com.example.hakonsreader.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.workers.InboxCheckerWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Broadcast receiver which calls [startInboxWorker] to start
 */
@AndroidEntryPoint
class InboxWorkerStartReceiver : BroadcastReceiver() {
    companion object {

        /**
         * The name of the Worker responsible for checking inbox messages
         */
        private const val WORKER_INBOX = "worker_inbox"

        /**
         * Enqueues a unique periodic request to [WorkManager] that runs an [InboxCheckerWorker].
         *
         * @param updateFrequency The update frequency of the worker, if this is 0 or negative no
         * work will be initiated, and any potentially active workers will be cancelled
         * @param replace If true any active worker will be replaced, if false the active worker will be kept
         */
        fun startInboxWorker(context: Context, updateFrequency: Int, replace: Boolean) {
            if (updateFrequency > 0) {
                val inboxRequest = PeriodicWorkRequestBuilder<InboxCheckerWorker>(updateFrequency.toLong(), TimeUnit.MINUTES)
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                val policy = if (replace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORKER_INBOX, policy, inboxRequest)
            } else {
                WorkManager.getInstance(context).cancelUniqueWork(WORKER_INBOX)
            }
        }
    }

    @Inject
    lateinit var settings: Settings

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> startInboxWorker(context, settings.inboxUpdateFrequency(), replace = false)
        }
    }
}