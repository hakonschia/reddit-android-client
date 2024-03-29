package com.example.hakonsreader

import android.app.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Configuration
import com.example.hakonsreader.activities.InvalidAccessTokenActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.*
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.broadcastreceivers.InboxWorkerStartReceiver
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.TokenManager
import dagger.hilt.android.HiltAndroidApp
import io.noties.markwon.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject


/**
 * Entry point for the application
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "App"

        /**
         * The ID of the inbox notification channel
         */
        const val NOTIFICATION_CHANNEL_INBOX_ID = "notificationChannelInbox"

        /**
         * The ID of the developer notification channel
         */
        const val NOTIFICATION_CHANNEL_DEVELOPER_ID = "notificationChannelDeveloper"
    }


    /**
     * A [RedditApi] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var api: RedditApi

    /**
     * A [RedditDatabase] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var database: RedditDatabase

    /**
     * A [Settings] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var settings: Settings

    /**
     * A [RedditUserInfoDatabase] instance. Outside classes should inject this themselves
     */
    @Inject
    lateinit var userInfoDatabase: RedditUserInfoDatabase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        // This has to be before super.onCreate() as the injections happen then, and this
        // must be set before the API is injected
        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE)
        SharedPreferencesManager.create(prefs)
        TokenManager.init(createEncryptedSharedPreferences())

        super.onCreate()

        AppState.init(api, database, userInfoDatabase)

        createInboxNotificationChannel()
        createDeveloperNotificationChannel()

        // Always restart the worker on startup (so that messages are retrieved on startup)
        InboxWorkerStartReceiver.startInboxWorker(this, settings.inboxUpdateFrequency(), replace = true)

        updateTheme()
        removeOldValues()
        removePrehistoricValues()

        registerActivityLifecycleCallbacks(this)

        // Must be commented out for release builds (as well as the import removed)
        //LeakCanary.config = LeakCanary.config.copy(dumpHeap = false)
        //LeakCanary.showLeakDisplayActivityLauncherIcon(false)
    }


    /**
     * Creates the inbox notification channel, if the build version allows it
     */
    private fun createInboxNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notificationChannelInbox)
            val descriptionText = getString(R.string.notificationChannelInboxDescription)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(NOTIFICATION_CHANNEL_INBOX_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the developer notification channel, if the build version allows it and [AppState.isDevMode] is true
     */
    private fun createDeveloperNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (AppState.isDevMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notificationChannelDeveloper)
            val descriptionText = getString(R.string.notificationChannelDeveloperDescription)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(NOTIFICATION_CHANNEL_DEVELOPER_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Removes old database records and post opened values
     */
    private fun removeOldValues() {
        CoroutineScope(IO).launch {
            removeOldDatabasePosts()

            // Calling this from the Coroutine because then we can pretend to be smart about performance :)
            removeOldPostOpenedPreferences()
        }
    }

    /**
     * Removes various values from SharedPreferences and such that are no longer used and should be
     * removed.
     *
     * Calling this ensures that those values aren't stored on the device when it is no longer needed
     */
    private fun removePrehistoricValues() {
        // This has been moved to TokenManager with EncryptedSharedPreferences
        SharedPreferencesManager.remove(SharedPreferencesConstants.ACCESS_TOKEN)
    }

    /**
     * Removes posts from the database that are older than 2 days
     */
    @Suppress("RedundantSuspendModifier")
    private suspend fun removeOldDatabasePosts() {
        // Remove records that are older than 2 days, as they likely won't be used again
        val maxAge = 60L * 60 * 24 * 2
        val count = database.posts().getCount()
        val deleted = database.posts().deleteOld(maxAge)

        Log.d(TAG, "onCreate: # of records=$count; # of deleted=$deleted")
    }

    /**
     * Removes keys older than 2 weeks from the SharedPreferences holding the opened post values
     */
    private fun removeOldPostOpenedPreferences() {
        // Having these keys in SharedPreferences is kind of weird, but it's way easier than creating
        // a database for it, so it works I guess

        val twoWeeksAgo = (System.currentTimeMillis() / 1000L) - 60 * 60 * 24 * 14

        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME_POST_OPENED, MODE_PRIVATE)
        val editor = prefs.edit()

        prefs.all.forEach { (key, value) ->
            // If the timestamp two weeks ago is larger than the stored value it is older
            // Now: 1000
            // 2 weeks ago: 986
            // Post opened 5 days ago: 995. 986 is not larger than 995, keep it
            // Post opened 15 days ago: 985. 986 is larger than 985, remove it
            if (twoWeeksAgo > value as Long) {
                editor.remove(key)
            }
        }

        editor.apply()
    }

    /**
     * Sets the theme (night mode) based on what is in the default SharedPreferences
     */
    private fun updateTheme() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)

        if (settings.getBoolean(getString(R.string.prefs_key_theme), resources.getBoolean(R.bool.prefs_default_theme))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Handles when the API notifies that the access token is no longer valid, and the user should
     * be logged out and prompted to log back in.
     */
    fun onInvalidAccessToken() {
        CoroutineScope(IO).launch {
            // Revoke token, the response to this never holds any data. If it fails we could potentially
            // store that it failed and retry again later
            TokenManager.getToken()?.let { api.accessToken().revoke(it) }

            api.logOut()

            // Clear any user specific state from database records (such as vote status on posts)
            database.clearUserState()

            AppState.getUserInfo()?.let {
                userInfoDatabase.userInfo().delete(it)
            }

            // Not logged in, cannot be browsing privately
            SharedPreferencesManager.removeNow(SharedPreferencesConstants.PRIVATELY_BROWSING)
            // Token no longer valid, remove it
            TokenManager.removeToken()

            // Show a dialog to let the user know they are now logged out
            withContext(Main) {
                Intent(this@App, InvalidAccessTokenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(this)
                }
            }
        }
    }

    private fun createEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            this,
            "encrypted_shared_preferences",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
    }

    override fun onActivityResumed(activity: Activity) {
        // The problem this is fixing is that if WiFi goes on/off when the app is in the background
        // the callback inside Settings might not be active, which means data saving settings can be out of sync
        settings.forceWiFiCheck(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not implemented
    }

    override fun onActivityStarted(activity: Activity) {
        // Not implemented
    }

    override fun onActivityPaused(activity: Activity) {
        // Not implemented
    }

    override fun onActivityStopped(activity: Activity) {
        // Not implemented
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not implemented
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Not implemented
    }
}