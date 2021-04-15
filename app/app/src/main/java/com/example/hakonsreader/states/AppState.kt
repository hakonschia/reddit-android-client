package com.example.hakonsreader.states

import android.content.Context
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hakonsreader.activities.MainActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.AccessToken
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.misc.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Global app state. Must be initialized during startup be using [init]
 */
object AppState {
    private lateinit var database: RedditDatabase
    private lateinit var userInfoDatabase: RedditUserInfoDatabase
    private lateinit var api: RedditApi

    private val _loggedInState = MutableLiveData<LoggedInState>()

    /**
     * A LiveData observable for the user state of the application. This will update either if
     * some user info was updated for the current user, the current user account changed, or if the
     * current user logged out
     */
    val loggedInState: LiveData<LoggedInState> = _loggedInState

    /**
     * True if the user has put the application in developer mode
     *
     * This can be toggled with [toggleDeveloperMode]
     */
    var isDevMode = false
        private set

    /**
     * Initialize the app state. This will update [loggedInState]
     *
     * This should only be called during startup
     */
    fun init(api: RedditApi, database: RedditDatabase, userInfoDatabase: RedditUserInfoDatabase) {
        AppState.database = database
        AppState.userInfoDatabase = userInfoDatabase
        AppState.api = api

        isDevMode = SharedPreferencesManager.get(SharedPreferencesConstants.DEVELOPER_MODE_ENABLED, Boolean::class.java) ?: false

        val token = TokenManager.getToken()

        // We have a token, and it is for a user
        val state = if (token != null && token.userId != AccessToken.NO_USER_ID) {
            // This database allows for main thread queries, since this has to be set before anything
            // is started to ensure that the state is correct (this might be kind of bad, but the query
            // should be fast enough that it doesn't impact startup by any noticeable amount)
            val info = userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token)
            if (api.isPrivatelyBrowsing()) {
                LoggedInState.PrivatelyBrowsing(info)
            } else {
                LoggedInState.LoggedIn(info)
            }
        } else {
            LoggedInState.LoggedOut
        }

        // Tests might run on a background thread, but when in production this must be set right away
        // otherwise some parts using the value might check the value before it's set, causing unwanted behaviour
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _loggedInState.value = state
        } else {
            _loggedInState.postValue(state)
        }
    }


    /**
     * Adds a new user
     */
    // Database operations must be suspended
    @Suppress("RedundantSuspendModifier")
    suspend fun addNewUser(token: AccessToken) {
        TokenManager.saveToken(token)
        val userInfo = RedditUserInfo(token)

        withContext(Dispatchers.Main) {
            _loggedInState.value = LoggedInState.LoggedIn(userInfo)
        }

        userInfoDatabase.userInfo().insert(userInfo)
    }

    /**
     * Saves user info and updates the local database for the currently logged in user. This uses
     * the value from [loggedInState] to determine the user
     *
     * Pass parameters to this function to update the relevant values.
     *
     * @param info The information about the user
     * @param subreddits The list of subreddit IDs the user is subscribed to
     */
    suspend fun updateUserInfo(info: RedditUser? = null, subreddits: List<String>? = null, nsfwAccount: Boolean? = null) {
        val state = loggedInState.value
        val userInfo = when (state) {
            is LoggedInState.LoggedIn -> state.userInfo
            is LoggedInState.PrivatelyBrowsing -> state.userInfo
            else -> return
        }

        if (info != null) {
            userInfo.userInfo = info
        }
        if (subreddits != null) {
            userInfo.subscribedSubreddits = subreddits
        }
        if (nsfwAccount != null) {
            userInfo.nsfwAccount = nsfwAccount
        }

        withContext(Dispatchers.Main) {
            // The state should not change, just update the information
            if (state is LoggedInState.LoggedIn) {
                _loggedInState.value = LoggedInState.LoggedIn(userInfo)
            } else {
                _loggedInState.value = LoggedInState.PrivatelyBrowsing(userInfo)
            }
        }

        if (userInfoDatabase.userInfo().userExists(userInfo.userId)) {
            userInfoDatabase.userInfo().update(userInfo)
        } else {
            userInfoDatabase.userInfo().insert(userInfo)
        }
    }


    /**
     * Gets the current user info, or null if there is no user logged in.
     *
     * Note this can be null even with a logged in user and should not be used as verification for
     * the logged in state. Use [loggedInState] for that.
     */
    fun getUserInfo() : RedditUserInfo? {
        return when (val state = _loggedInState.value) {
            is LoggedInState.LoggedIn -> state.userInfo
            is LoggedInState.PrivatelyBrowsing -> state.userInfo
            else -> null
        }
    }

    /**
     * Callback for when new access tokens are received. This will save the token to [TokenManager]
     *
     * @param token The new token
     */
    fun onNewToken(token: AccessToken) {
        TokenManager.saveToken(token)
        if (token.userId != AccessToken.NO_USER_ID) {
            CoroutineScope(Dispatchers.IO).launch {
                getUserInfoFromToken(token).apply {
                    accessToken = token

                    // New token is for a user
                    if (userInfoDatabase.userInfo().userExists(userId)) {
                        userInfoDatabase.userInfo().update(this)
                    } else {
                        userInfoDatabase.userInfo().insert(this)
                    }
                }
            }
        }
    }

    /**
     * Gets a [RedditUserInfo] object corresponding to an access token.
     */
    // Database operations must be suspended
    @Suppress("RedundantSuspendModifier")
    private suspend fun getUserInfoFromToken(token: AccessToken) : RedditUserInfo {
        return userInfoDatabase.userInfo().getById(token.userId) ?: RedditUserInfo(token)
    }

    /**
     * Switches which account is the active account.
     *
     * @param token The token to use for the new active account
     * @param activity The activity currently active. The activity will be recreated
     */
    fun switchAccount(token: AccessToken, activity: AppCompatActivity) {
        CoroutineScope(Dispatchers.IO).launch {
            // Ensure no user state from one account is used for the new one
            database.clearUserState()

            api.switchAccessToken(token)
            TokenManager.saveToken(token)

            SharedPreferencesManager.putNow(SharedPreferencesConstants.PRIVATELY_BROWSING, false)

            withContext(Dispatchers.Main) {
                _loggedInState.value = LoggedInState.LoggedIn(getUserInfoFromToken(token))

                if (activity is MainActivity) {
                    activity.recreateAsNewUser()
                } else {
                    activity.recreate()
                }
            }
        }
    }

    /**
     * Toggles private browsing. This is just a convenience method for [enablePrivateBrowsing] with
     * null passed as the argument
     */
    fun togglePrivateBrowsing() {
        enablePrivateBrowsing()
    }

    /**
     * Enable or disable the APIs private browsing and stores locally that private browsing is enabled/disabled
     *
     * @param enable True to enable private browsing, false to disable. Use null to toggle
     */
    fun enablePrivateBrowsing(enable: Boolean? = null) {
        // Use the value passed if not null, otherwise toggle based on the APIs state
        val enableActual = enable ?: !api.isPrivatelyBrowsing()

        api.enablePrivateBrowsing(enableActual)

        SharedPreferencesManager.put(SharedPreferencesConstants.PRIVATELY_BROWSING, enableActual)

        // When enabling/disabling private browsing we *should* have a user, but if not return
        val userInfo = getUserInfo() ?: return
        val state = if (enableActual) {
            LoggedInState.PrivatelyBrowsing(userInfo)
        } else {
            LoggedInState.LoggedIn(userInfo)
        }

        _loggedInState.postValue(state)
    }


    /**
     * Clears any user information stored, logging a user out. The application will be restarted
     *
     * @param context The context requesting the log out. If this is a [MainActivity] then [MainActivity.recreateAsNewUser]
     * will be called. Otherwise, if it is an activity then [AppCompatActivity.recreate] will be called
     */
    fun logOut(context: Context? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            // Revoke token, the response to this never holds any data. If it fails we could potentially
            // store that it failed and retry again later
            TokenManager.getToken()?.let { api.accessToken().revoke(it) }

            api.logOut()

            // Clear any user specific state from database records (such as vote status on posts)
            database.clearUserState()

            getUserInfo()?.let {
                userInfoDatabase.userInfo().delete(it)
            }

            SharedPreferencesManager.remove(SharedPreferencesConstants.PRIVATELY_BROWSING)

            TokenManager.removeToken()

            withContext(Dispatchers.Main) {
                // We could potential look for other users stored here, but we might not want to
                // assume which account to use then, so it's better to just let the user choose later
                _loggedInState.value = LoggedInState.LoggedOut

                if (context is MainActivity) {
                    context.recreateAsNewUser()
                } else if (context is AppCompatActivity) {
                    context.recreate()
                }
            }
        }
    }

    /**
     * Toggles developer mode
     *
     * @return The new state
     */
    fun toggleDeveloperMode(): Boolean {
        isDevMode = !isDevMode
        SharedPreferencesManager.put(SharedPreferencesConstants.DEVELOPER_MODE_ENABLED, isDevMode)
        return isDevMode
    }
}