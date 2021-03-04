package com.example.hakonsreader.activities

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMessage
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityMainBinding
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter
import com.example.hakonsreader.fragments.*
import com.example.hakonsreader.interfaces.*
import com.example.hakonsreader.misc.TokenManager
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.misc.Util.setAgeTextTrendingSubreddits
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter
import com.example.hakonsreader.recyclerviewadapters.TrendingSubredditsAdapter
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel
import com.example.hakonsreader.viewmodels.TrendingSubredditsViewModel
import com.example.hakonsreader.viewmodels.factories.SelectSubredditsFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.fixedRateTimer

class MainActivity : BaseActivity(), OnSubredditSelected, OnInboxClicked, OnUnreadMessagesBadgeSettingChanged {

    companion object {
        private const val TAG = "MainActivity"

        private const val POSTS_FRAGMENT = "postsFragment"
        private const val ACTIVE_SUBREDDIT_FRAGMENT = "activeSubredditFragment"
        private const val SELECT_SUBREDDIT_FRAGMENT = "selectSubredditFragment"
        private const val PROFILE_FRAGMENT = "profileFragment"
        private const val SETTINGS_FRAGMENT = "settingsFragment"

        /**
         * The saved position of the nav bar
         */
        private const val ACTIVE_NAV_ITEM = "activeNavItem"

        /**
         * The key used to store the name of the subreddit represented in [MainActivity.activeSubreddit]
         *
         * When this key holds a value it does not necessarily mean the subreddit was active (shown on screen)
         * at the time the instance was saved, but means that when clicking on the subreddit navbar, this should
         * be shown again instead of the list of subreddits
         */
        private const val ACTIVE_SUBREDDIT_NAME = "main_activity_active_subreddit_name"

        /**
         * When creating this activity, set this on the extras to select the subreddit to show by default
         *
         * The value with this key should be a [String]
         */
        const val START_SUBREDDIT = "startSubreddit"
    }

    private lateinit var binding: ActivityMainBinding

    private val db = App.get().database

    /**
     * The amount of unread messages in the inbox
     */
    private var unreadMessages = 0

    private var inboxNotificationCounter = 0

    private val notifications = HashMap<String, Int>()

    // The fragments to show in the nav bar
    private var standardSubFragment: StandardSubContainerFragment? = null
    private var activeSubreddit: SubredditFragment? = null
    private var selectSubredditFragment: SelectSubredditFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var inboxFragment: InboxFragment? = null
    private var logInFragment: LogInFragment? = null
    private var settingsFragment: SettingsFragment? = null
    private var lastShownFragment: Fragment? = null
    private val navigationViewListener: BottomNavigationViewListener = BottomNavigationViewListener()

    private var subredditsAdapter: SubredditsAdapter? = null
    private var subredditsLayoutManager: LinearLayoutManager? = null
    private val subredditsViewModel: SelectSubredditsViewModel by viewModels {
        SelectSubredditsFactory(App.get().isUserLoggedIn() && !App.get().isUserLoggedInPrivatelyBrowsing())
    }

    private var trendingSubredditsAdapter: TrendingSubredditsAdapter? = null
    private var trendingSubredditsLayoutManager: LinearLayoutManager? = null
    private val trendingSubredditsViewModel: TrendingSubredditsViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch back to the app theme (from the launcher theme)
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // For testing purposes hardcode going into a subreddit/post etc.
        // TODO there are some issues with links, if a markdown link has superscript inside of it, markwon doesnt recognize it (also spaces in links causes issues)
        //  https://www.reddit.com/r/SpeedyDrawings/comments/jgg06k/this_gave_me_a_mild_heart_attack/
        Intent(this, DispatcherActivity::class.java).run {
            putExtra(DispatcherActivity.URL_KEY, "https://www.reddit.com/r/GlobalOffensive/comments/kul6ye/ww2_plane_inspired_skin_for_awp/")
            //startActivity(this)
        }

        attachFragmentChangeListener()
        setupNavBar()

        if (savedInstanceState != null) {
            restoreFragmentStates(savedInstanceState)
            restoreNavBar(savedInstanceState)
        } else {
            // Use empty string as default (ie. front page)
            val startSubreddit = intent.extras?.getString(START_SUBREDDIT) ?: ""
            // Only setup the start fragment if we have no state to restore (as this is then a new activity)
            setupStartFragment(startSubreddit)

            // Only start the inbox listener once, or else every configuration change would start another timer
            // TODO this has to be improved as now it leaks and doesn't really work at all
            //startInboxListener()
            trendingSubredditsViewModel.loadSubreddits()
        }

        setupNavDrawer()
        checkAccessTokenScopes()
        setProfileNavBarTitle()

        App.get().run {
            registerReceivers()
            privatelyBrowsing.observe(this@MainActivity) {
                privateBrowsingStateChanged(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val uri = intent.data ?: return

        // Resumed from OAuth authorization
        if (uri.toString().startsWith(NetworkConstants.CALLBACK_URL)) {
            handleOAuthResume(uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.get().run {
            unregisterReceivers()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // If fragments aren't null, save them
        // Save which fragment is the active one as well

        standardSubFragment?.let {
            supportFragmentManager.putFragment(outState, POSTS_FRAGMENT, it)
        }

        activeSubreddit?.let {
            // If there is an active subreddit it won't be null, store the state of it even if it isn't
            // currently added (shown on screen)
            outState.putString(ACTIVE_SUBREDDIT_NAME, it.subredditName)
            supportFragmentManager.putFragment(outState, ACTIVE_SUBREDDIT_FRAGMENT, it)
        }

        profileFragment?.let {
            supportFragmentManager.putFragment(outState, PROFILE_FRAGMENT, it)
        }

        selectSubredditFragment?.let {
            supportFragmentManager.putFragment(outState, SELECT_SUBREDDIT_FRAGMENT, it)
        }

        settingsFragment?.let {
            supportFragmentManager.putFragment(outState, SETTINGS_FRAGMENT, it)
        }

        // Login can just be recreated when needed as they don't store any specific state

        // Store state of navbar
        outState.putInt(ACTIVE_NAV_ITEM, binding.bottomNav.selectedItemId)
    }

    override fun onBackPressed() {
        if (binding.mainParentLayout.isDrawerOpen(GravityCompat.START)) {
            binding.mainParentLayout.closeDrawer(GravityCompat.START)
            return
        }

        val activeFragment = getActiveFragment()
        // In an active subreddit
        if (activeFragment is SubredditFragment) {
            // In a subreddit, and the last item was the list, go back to the list
            if (lastShownFragment is SelectSubredditFragment) {
                activeSubreddit = null

                // If the fragment has been killed by the OS make a new one (after a while it might be killed)
                if (selectSubredditFragment == null) {
                    selectSubredditFragment = SelectSubredditFragment.newInstance()
                }

                // Since we are in a way going back in the same navbar item, use the close transition
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, selectSubredditFragment!!)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .addToBackStack(null)
                        .commit()
            } else if (!activeFragment.closeDrawerIfOpen()) {
                // The active subreddit didn't have a drawer open that it closed, go back to main menu
                binding.bottomNav.selectedItemId = R.id.navHome
            }
        } else if (activeFragment is InboxFragment && lastShownFragment is ProfileFragment) {
            // In the inbox, and the last active was the profile, go back to the profile
            if (profileFragment == null) {
                profileFragment = ProfileFragment.newInstance()
            }
            profileFragment

            navigationViewListener.profileLastShownIsProfile = true

            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, profileFragment!!)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .addToBackStack(null)
                    .commit()
        } else {
            if (binding.bottomNav.selectedItemId == R.id.navHome) {
                // moveTaskToBack() is like pressing home, which closes the app without killing it
                // This fails/returns false when the app was started from somewhere (like an intent),
                // and we can use onBackPressed() as this will go back (normally onBackPressed() would
                // use the fragment backstack which causes some weird issues that doesn't close the app)
                if (!moveTaskToBack(true)) {
                    super.onBackPressed()
                }
            } else {
                binding.bottomNav.selectedItemId = R.id.navHome
            }
        }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_round_menu_24)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.mainParentLayout.openDrawer(GravityCompat.START); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Called when a subreddit has been selected from a [SelectSubredditFragment] fragment
     *
     * A new instance of [SubredditFragment] is created and shown
     *
     * This will hide the keyboard, if shown
     *
     * @param subredditName The subreddit selected
     */
    override fun subredditSelected(subredditName: String) {
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        val lowerCased = subredditName.toLowerCase()
        // If default sub, use the home navbar instead
        if (RedditApi.STANDARD_SUBS.contains(lowerCased)) {
            val sub = StandardSubContainerFragment.StandarSub.values().find { it.value == lowerCased }
                    ?: StandardSubContainerFragment.StandarSub.FRONT_PAGE

            if (standardSubFragment == null) {
                standardSubFragment = StandardSubContainerFragment.newInstance()
            }

            // I don't think this would trigger a reselect if already selected
            binding.bottomNav.selectedItemId = R.id.navHome
            standardSubFragment!!.setActiveSubreddit(sub)
        } else {
            // If the current subreddit is the same, use the old instead of creating a new one
            // If we're going to "r/random" we should recreate it, as that isn't really a specific subreddit
            // (SubredditFragment should probably just change the subredditName tbh)
            if (subredditName.equals("random", ignoreCase = true) || activeSubreddit?.subredditName != subredditName) {
                activeSubreddit = SubredditFragment.newInstance(subredditName)
            }

            // Already in the subreddit navbar, open subreddit with opening
            if (binding.bottomNav.selectedItemId == R.id.navSubreddit) {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, activeSubreddit!!)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                        .commit()
            } else {
                // Otherwise select the subreddit navbar, which will use activeSubreddit and deal with animations
                binding.bottomNav.selectedItemId = R.id.navSubreddit
            }
        }

        // If this was called from a drawer it should close (otherwise the drawer already is closed)
        binding.mainParentLayout.closeDrawer(GravityCompat.START)
    }

    override fun onInboxClicked() {
        if (inboxFragment == null) {
            inboxFragment = InboxFragment.newInstance()
        }

        navigationViewListener.profileLastShownIsProfile = false

        // Should maybe be sure that the profile navbar is clicked? Dunno
        // Change the navbar name to be "Inbox" maybe?
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, inboxFragment!!)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
    }

    override fun showUnreadMessagesBadge(show: Boolean) {
        val badge = binding.bottomNav.getBadge(binding.bottomNav.menu.findItem(R.id.navProfile).itemId)
        badge?.isVisible = show
    }

    private fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        with(binding.navDrawer) {
            this.privatelyBrowsing = privatelyBrowsing

            // This doesn't work programmatically
            profilePicture.borderColor = ContextCompat.getColor(
                    this@MainActivity,
                    if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
            )
        }
    }

    /**
     * Handles when the activity resumes from an OAuth intent. If the intent is a successful
     * login attempt, then the user info is retrieved
     */
    private fun handleOAuthResume(uri: Uri) {
        val state = uri.getQueryParameter("state")

        // Not a match from the state we generated, something weird is happening
        if (state == null || state != App.get().oauthState) {
            Util.showErrorLoggingInSnackbar(binding.mainParentLayout)
            return
        }

        val code = uri.getQueryParameter("code")
        if (code == null) {
            Util.showErrorLoggingInSnackbar(binding.mainParentLayout)
            return
        }

        App.get().clearOAuthState()

        // This might be bad, but onResume is called when opening a post and going back and still
        // holds the same intent which causes this branch to execute again, causing issues
        intent.replaceExtras(Bundle())
        intent.action = ""
        intent.data = null
        intent.flags = 0

        val api = App.get().api

        CoroutineScope(IO).launch {
            when (val resp = api.accessToken().get(code)) {
                is ApiResponse.Success -> {
                    getUserInfo()

                    // Re-create the start fragment as it now should load posts for the logged in user
                    // TODO this is kinda bad as it gets posts and then gets posts again for the logged in user
                    withContext(Main) {
                        standardSubFragment = null
                        setupStartFragment("")
                        Snackbar.make(binding.mainParentLayout, R.string.loggedIn, BaseTransientBottomBar.LENGTH_SHORT).show()
                    }
                }

                is ApiResponse.Error -> {
                    Util.handleGenericResponseErrors(binding.mainParentLayout, resp.error, resp.throwable)
                }
            }
        }
    }

    /**
     * Gets user info, if this fails then a snackbar is shown to let the user know and attempt again
     */
    private fun getUserInfo() {
        val api = App.get().api

        CoroutineScope(IO).launch {
            when (val userInfo = api.user().info()) {
                is ApiResponse.Success -> {
                    // TODO this has to update the nav drawer (which should happen automatically with observables)
                    App.get().updateUserInfo(info = userInfo.value)
                    // This will be called after the activity has been restarted when logging in
                    // so call it when user information is retrieved as well
                    withContext(Main) {
                        setProfileNavBarTitle()
                    }
                }
                is ApiResponse.Error -> {
                    // Seeing as this is called when the access token was just retrieved, it is very
                    // unlikely to fail, but just in case
                    Snackbar.make(binding.root, R.string.userInfoFailedToGet, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.userInfoFailedToGetTryAgain) {
                                getUserInfo()
                            }
                            .show()
                }
            }
        }
    }

    /**
     * Checks the scopes of the access token stored in the application against [NetworkConstants.SCOPE]
     * to see if scopes have been added since the access token was retrieved
     */
    private fun checkAccessTokenScopes() {
        val token = TokenManager.getToken()
        // No token, or token for a non-logged in user
        if (token == null || token.refreshToken == null) {
            return
        }

        val requiredScopesAsArray = NetworkConstants.SCOPE.split(" ").toTypedArray()
        val storedScopesAsArray = listOf(*token.scopesAsArray)

        val missingScopes = ArrayList<String>()

        for (scope in requiredScopesAsArray) {
            if (!storedScopesAsArray.contains(scope)) {
                missingScopes.add(scope)
            }
        }

        // Missing scope found
        if (missingScopes.isNotEmpty()) {
            // Check if we have already shown the dialog, if we have don't show again
            // This will only be stored in SharedPreferences if the user clicked "Dont show again"
            val preferences = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE)
            val checkedScopes = preferences.getString(SharedPreferencesConstants.ACCESS_TOKEN_SCOPES_CHECKED, "")!!.split(" ").toTypedArray()

            Arrays.sort(checkedScopes)
            Arrays.sort(requiredScopesAsArray)
            if (requiredScopesAsArray.contentEquals(checkedScopes)) {
                return
            }

            val adapter = OAuthScopeAdapter(this, R.layout.list_item_oauth_explanation, missingScopes)
            val view = layoutInflater.inflate(R.layout.dialog_title_new_permissions, binding.mainParentLayout, false)

            AlertDialog.Builder(this)
                    .setCustomTitle(view)
                    .setAdapter(adapter, null)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                    .setNegativeButton(R.string.newPermissionsDontShowAgain) { _, _ ->
                        // Store in SharedPreferences so the next time the code above will trigger a return
                        preferences.edit().putString(SharedPreferencesConstants.ACCESS_TOKEN_SCOPES_CHECKED, NetworkConstants.SCOPE).apply()
                    }
                    .show()
        }
    }


    /**
     * Starts a timer that runs at a given interval. Each run will make an API call for the users inbox
     * to retrieve new messages
     *
     * If there is no user logged in then the timer is not started
     */
    private fun startInboxListener() {
        // The activity should be recreated when a user logs in, so this should be fine
        if (!App.get().isUserLoggedIn()) {
            return
        }

        observeUnreadMessages()

        // This runs when the application is minimized, might be bad? Can obviously send notifications this way, but
        // it should probably be done in a different way

        val api = App.get().api

        // This wont be updated until the app restarts
        val updateFrequency = App.get().inboxUpdateFrequency()
        Log.d(TAG, "InboxTimer frequency: $updateFrequency minutes")
        if (updateFrequency != -1) {
            var counter = 0

            fixedRateTimer("inboxTimer", false, 0L, updateFrequency * 60 * 1000L) {
                Log.d(TAG, "InboxTimer running")

                CoroutineScope(IO).launch {
                    // Get all messages every 10th request, in case a message has been seen outside the
                    // application then it won't be in the unread messages, so get all every once in a while
                    val response = if (counter % 10 == 0) {
                        api.messages().inbox()
                    } else {
                        api.messages().unread()
                    }

                    counter++

                    when (response) {
                        is ApiResponse.Success -> {
                            // TODO this should also remove previous notifications if they are now seen
                            //  Or possibly in observeUnreadMessages?
                            response.value.filter { it.isNew }.forEach { createInboxNotification(it) }
                            db.messages().insertAll(response.value)
                        }
                        is ApiResponse.Error -> {
                        }
                    }
                }
            }
        }
    }

    /**
     * Observes the unread messages in the local database and updates the profile navbar accordingly
     */
    private fun observeUnreadMessages() {
        val unread = db.messages().getUnreadMessages()
        unread.observe(this, { m ->
            unreadMessages = m.size

            if (App.get().showUnreadMessagesBadge()) {
                val profileItemId = binding.bottomNav.menu.findItem(R.id.navProfile).itemId

                // No unread messages, remove the badge
                if (unreadMessages == 0) {
                    binding.bottomNav.removeBadge(profileItemId)
                } else {
                    // Add or create a badge and update the number
                    binding.bottomNav.getOrCreateBadge(profileItemId).apply {
                        isVisible = true
                        number = unreadMessages
                    }
                }
            }
        })
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
            getString(R.string.notificationInboxCommentReplyTitle, message.author)
        } else {
            getString(R.string.notificationInboxMessageTitle, message.author)
        }

        // Only open messages, we don't have anything to do for messages
        val pendingIntent = if (message.wasComment) {
            val intent = Intent(this, DispatcherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(DispatcherActivity.URL_KEY, message.context)
            }
            PendingIntent.getActivity(this, 0, intent, 0)
        } else {
            null
        }

        val builder = NotificationCompat.Builder(this@MainActivity, App.NOTIFICATION_CHANNEL_INBOX_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                // TODO this should show the "raw" text, without any markdown formatting
                .setContentText(message.body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this@MainActivity)) {
            val id = inboxNotificationCounter++
            notify(id, builder.build())
            notifications[message.id] = id
        }
    }

    /**
     * Attaches a listener to [getSupportFragmentManager] that stores fragments when detached
     * and saves it to [lastShownFragment]
     *
     * In addition, if the fragment detached was [activeSubreddit], its state is saved with [SubredditFragment.saveState]
     * to [savedState]
     */
    private fun attachFragmentChangeListener() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            // onFragmentDetached suddenly stopped working. onFragmentViewDestroyed works as long as only one fragment
            // is shown at a time (which is obviously true for how it is now, but if for instance this was in
            // a tablet it could show more fragments at once, so it wouldn't work)
            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentViewDestroyed(fm, f)

                // The active subreddit doesn't have a chance to save its state, so it would be reloaded
                // when detached
                if (f == activeSubreddit) {
                    f as SubredditFragment
                }

                // InboxGroupFragment is an "inner" fragment and not one we want to store directly
                // (it is inside InboxFragment)
                if (f !is InboxGroupFragment) {
                    lastShownFragment = f
                }
            }
        }, false)
    }

    /**
     * Sets the profile nav bar title to the logged in users name, if there is a logged in user and
     * there is information about the user stored
     */
    private fun setProfileNavBarTitle() {
        val user = App.get().currentUserInfo?.userInfo
        if (user != null) {
            binding.bottomNav.menu.findItem(R.id.navProfile).title = user.username
        }
    }

    /**
     * Updates the language for the application
     *
     * @param language The language to switch to. If this is `null` the language found in
     * SharedPreferences will be used. Default is `null`
     * @param recreate True if the activity should be recreated. This should only be set to true
     * if the language is updated while the application is running. If this is true during the
     * activity setup, it will cause an infinite loop. Default is `false`
     */
    fun updateLanguage(language: String? = null, recreate: Boolean = false) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val lang = language ?: settings.getString(getString(R.string.prefs_key_language), getString(R.string.prefs_default_language)) ?: return

        val config = resources.configuration
        config.setLocale(Locale(lang))
        resources.updateConfiguration(config, resources.displayMetrics)

        if (recreate) {
            recreate()
        }
    }

    /**
     * Gets the active fragment currently shown in the fragment container
     */
    private fun getActiveFragment() : Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer)
    }

    /**
     * Restores the state of the fragments as saved in [onSaveInstanceState]
     *
     * @param restoredState The bundle with the state of the fragments
     */
    private fun restoreFragmentStates(restoredState: Bundle) {
        standardSubFragment = supportFragmentManager.getFragment(restoredState, POSTS_FRAGMENT) as StandardSubContainerFragment?
        activeSubreddit = supportFragmentManager.getFragment(restoredState, ACTIVE_SUBREDDIT_FRAGMENT) as SubredditFragment?
        selectSubredditFragment = supportFragmentManager.getFragment(restoredState, SELECT_SUBREDDIT_FRAGMENT) as SelectSubredditFragment?
        profileFragment = supportFragmentManager.getFragment(restoredState, PROFILE_FRAGMENT) as ProfileFragment?
        settingsFragment = supportFragmentManager.getFragment(restoredState, SETTINGS_FRAGMENT) as SettingsFragment?

        if (standardSubFragment == null) {
            standardSubFragment = StandardSubContainerFragment.newInstance()
        }

        if (activeSubreddit == null) {
            // Active subreddit not restored directly, check if it should be restored manually
            val activeSubredditName = restoredState.getString(ACTIVE_SUBREDDIT_NAME)
            if (activeSubredditName != null) {
                activeSubreddit = SubredditFragment.newInstance(activeSubredditName)
            }
        }

        // Restore the listeners as it won't be set automatically
        profileFragment?.onInboxClicked = this
        selectSubredditFragment?.subredditSelected = this
    }

    /**
     * Restores the state of the nav bar
     */
    private fun restoreNavBar(restoredState: Bundle) {
        val active = restoredState.getInt(ACTIVE_NAV_ITEM)

        // When we're restoring a state we don't want to play an animation, as the user hasn't manually
        // selected a change
        navigationViewListener.disableAnimationForNextChange()
        binding.bottomNav.selectedItemId = active
    }

    /**
     * Sets up the fragment to display at startup. This will create [standardSubFragment]
     *
     * @param startSubreddit The name of the subreddit to display. If this is a standard subreddit
     * [standardSubFragment] will be shown with the corresponding subreddit selected. Otherwise,
     * the [activeSubreddit] is set with the subreddit and is shown in the subreddit navbar
     */
    private fun setupStartFragment(startSubreddit: String) {
        if (standardSubFragment == null) {
            standardSubFragment = StandardSubContainerFragment.newInstance()
        }

        val defaultSub = StandardSubContainerFragment.StandarSub.values().find { it.value == startSubreddit.toLowerCase() }

        if (defaultSub != null) {
            standardSubFragment!!.apply {
                this.defaultSub = defaultSub
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, this)
                        .addToBackStack(null)
                        .commit()
            }
        } else {
            activeSubreddit = SubredditFragment.newInstance(startSubreddit)
            binding.bottomNav.selectedItemId = R.id.navSubreddit
        }
    }

    /**
     * Sets up the nav bar to be able to switch between the fragments
     */
    private fun setupNavBar() {
        binding.bottomNav.setOnNavigationItemSelectedListener(navigationViewListener)

        // Set listener for when an item has been clicked when already selected
        binding.bottomNav.setOnNavigationItemReselectedListener { item ->
            when (item.itemId) {
                // When "Subreddit" is clicked when already in "subreddit" go back to the subreddit list
                R.id.navSubreddit -> {
                    activeSubreddit = null
                    if (selectSubredditFragment == null) {
                        selectSubredditFragment = SelectSubredditFragment.newInstance().apply {
                            subredditSelected = this@MainActivity
                        }
                    }

                    // Since we are in a way going back in the same navbar item, use the close transition
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, selectSubredditFragment!!)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            .addToBackStack(null)
                            .commit()
                }

                // In the inbox, go back to profile
                R.id.navProfile -> {
                    if (profileFragment == null) {
                        profileFragment = ProfileFragment.newInstance()
                    }

                    profileFragment!!.onInboxClicked = this

                    navigationViewListener.profileLastShownIsProfile = true

                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, profileFragment!!)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            .addToBackStack(null)
                            .commit()
                }
            }
        }
    }


    /**
     * Selects the profile nav bar. If the nav drawer is open it will be closed
     */
    fun selectProfileNavBar() {
        binding.mainParentLayout.closeDrawer(GravityCompat.START)
        binding.bottomNav.selectedItemId = R.id.navProfile
    }

    /**
     * Selects the settings nav bar. If the nav drawer is open it will be closed
     */
    fun selectSettingsNavBar() {
        binding.mainParentLayout.closeDrawer(GravityCompat.START)
        binding.bottomNav.selectedItemId = R.id.navSettings
    }

    /**
     * Sets up the nav drawer, including initialization of [subredditsViewModel].
     *
     * [loadSubreddits] is automatically called
     */
    private fun setupNavDrawer() {
        with (subredditsViewModel) {
            subreddits.observe(this@MainActivity, { subreddits ->
                subredditsAdapter?.submitList(subreddits as MutableList<Subreddit>, true)
            })

            isLoading.observe(this@MainActivity) {
                binding.navDrawer.subredditsLoader.visibility = if (it) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            error.observe(this@MainActivity, { error ->
                Util.handleGenericResponseErrors(binding.mainParentLayout, error.error, error.throwable)
            })
        }

        with(trendingSubredditsViewModel) {
            trendingSubreddits.observe(this@MainActivity) { trending ->
                trending.subreddits?.let {
                    trendingSubredditsAdapter?.submitList(it)
                }

                setTrendingSubredditsLastUpdated(trending)
            }

            isLoading.observe(this@MainActivity) {
                binding.navDrawer.trendingSubredditsLoader.visibility = if (it) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            error.observe(this@MainActivity, { error ->
                Util.handleGenericResponseErrors(binding.mainParentLayout, error.error, error.throwable)
            })
        }

        binding.mainParentLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                trendingSubredditsViewModel.trendingSubreddits.value?.let {
                    setTrendingSubredditsLastUpdated(it)
                }
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Not implemented
            }

            override fun onDrawerClosed(drawerView: View) {
                // Not implemented
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Not implemented
            }
        })

        with(binding.navDrawer) {
            app = App.get()
            userInfo = App.get().currentUserInfo
            subredditSelected = this@MainActivity

            profilePicture.setOnClickListener { selectProfileNavBar() }
            username.setOnClickListener { selectProfileNavBar() }
            settingsClicker.setOnClickListener { selectSettingsNavBar() }
            darkModeClicker.setOnClickListener {
                PreferenceManager.getDefaultSharedPreferences(this@MainActivity).run {
                    val key = getString(R.string.prefs_key_theme)
                    val darkMode = getBoolean(key, resources.getBoolean(R.bool.prefs_default_theme))
                    edit().putBoolean(key, !darkMode).apply()
                    App.get().updateTheme()
                }
            }

            subredditsAdapter = SubredditsAdapter().apply {
                viewType = SubredditsAdapter.SubredditViewType.SIMPLE
                subredditSelected = this@MainActivity
                favoriteClicked = OnClickListener { subreddit -> subredditsViewModel?.favorite(subreddit) }
            }
            subredditsLayoutManager = LinearLayoutManager(this@MainActivity)
            subreddits.run {
                adapter = subredditsAdapter
                layoutManager = subredditsLayoutManager
            }

            trendingSubredditsLastUpdated.setOnClickListener { trendingSubredditsViewModel.loadSubreddits() }
            trendingSubredditsAdapter = TrendingSubredditsAdapter().apply {
                onSubredditSelected = {
                    Intent(this@MainActivity, SubredditActivity::class.java).apply {
                        putExtra(SubredditActivity.SUBREDDIT_KEY, it)
                        startActivity(this)
                    }
                }
            }
            trendingSubredditsLayoutManager = LinearLayoutManager(this@MainActivity)
            trendingSubreddits.run {
                adapter = trendingSubredditsAdapter
                layoutManager = trendingSubredditsLayoutManager
            }
        }

        loadSubreddits()
    }

    /**
     * Loads subreddits, either subscribed subreddits if there is a logged in user, or default
     * if there isn't (or there is one privately browsing)
     */
    private fun loadSubreddits() {
        val loadDefault = if (App.get().isUserLoggedIn()) {
            // If the user is logged in we want to load default subs if they're privately browsing
            App.get().isUserLoggedInPrivatelyBrowsing()
        } else {
            true
        }
        subredditsViewModel?.loadSubreddits(loadDefault)
    }

    /**
     * Sets the trending subreddits retrieved text in the nav drawer
     *
     * @param trendingSubreddits The trending subreddits object to use for the text
     */
    private fun setTrendingSubredditsLastUpdated(trendingSubreddits: TrendingSubreddits) {
        val created = Instant.ofEpochSecond(trendingSubreddits.retrieved)
        val now = Instant.now()
        val between = Duration.between(created, now)
        setAgeTextTrendingSubreddits(binding.navDrawer.trendingSubredditsLastUpdated, between)
    }

    /**
     * Custom BottomNavigationView item selection listener
     */
    inner class BottomNavigationViewListener : BottomNavigationView.OnNavigationItemSelectedListener {
        private var playAnimationForNextChange = true

        /**
         * The current index the nav bar is in (from left to right). Used to determine what transition
         * to play when changing the nav bar item
         */
        private var navBarPos = 0

        /**
         * Flag indicating if that the last time the profile was selected in the navbar, the profile
         * was shown, and not the inbox
         *
         * If this is null, then no previous fragment has been shown and should decide which fragment
         * should be shown depending on the normal execution branch
         */
        var profileLastShownIsProfile: Boolean? = null

        /**
         * Disables the animation for the next nav bar change. Note this only lasts for one change
         */
        fun disableAnimationForNextChange() {
            playAnimationForNextChange = false
        }

        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            val selected: Fragment
            val previousPos = navBarPos
            when (item.itemId) {
                R.id.navHome -> {
                    navBarPos = 1
                    if (standardSubFragment == null) {
                        standardSubFragment = StandardSubContainerFragment.newInstance()
                    }
                    selected = standardSubFragment as StandardSubContainerFragment
                }
                R.id.navSubreddit -> {
                    navBarPos = 2
                    selected = onNavBarSubreddit()
                }
                R.id.navProfile -> {
                    navBarPos = 3
                    selected = onNavBarProfile()
                }
                R.id.navSettings -> {
                    navBarPos = 4
                    if (settingsFragment == null) {
                        settingsFragment = SettingsFragment.newInstance().apply {
                            languageListener
                        }
                    }
                    settingsFragment!!.unreadMessagesBadgeSettingChanged = this@MainActivity
                    settingsFragment!!.languageListener = LanguageListener {
                        updateLanguage(it, recreate = true)
                    }
                    selected = settingsFragment!!
                }
                else -> return false
            }

            // Example: previous = 2, current = 3. We are going right
            val goingRight = previousPos < navBarPos
            if (!playAnimationForNextChange) {
                replaceNavBarFragmentNoAnimation(selected)
                playAnimationForNextChange = true
            } else {
                replaceNavBarFragment(selected, goingRight)
            }
            return true
        }

        /**
         * Replaces the fragment in [ActivityMainBinding.fragmentContainer] without any animation. This should
         * be used in accordance with the nav bar.
         *
         * @param fragment The new fragment to show
         */
        private fun replaceNavBarFragmentNoAnimation(fragment: Fragment) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    // Although we don't use the backstack to pop elements, it is needed to keep the state
                    // of the fragments (otherwise posts are reloaded when coming back)
                    // With the use of a local database I can easily restore the state without the back stack
                    // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                    // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                    .addToBackStack(null)
                    .commit()
        }

        /**
         * Replaces the fragment in [ActivityMainBinding.fragmentContainer]. This should
         * be used in accordance with the nav bar
         *
         * @param fragment The new fragment to show
         * @param goingRight True if going from right in the nav bar items, this changes the way
         * the animation slides so it makes sense based on the nav bar
         */
        private fun replaceNavBarFragment(fragment: Fragment, goingRight: Boolean) {
            supportFragmentManager.beginTransaction()
                    // TODO if there is an ongoing transition and the user selects another nav bar item, the app crashes (need to somehow cancel the ongoing transition or something)
                    .setCustomAnimations(if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left, if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, fragment) // Although we don't use the backstack to pop elements, it is needed to keep the state
                    // of the fragments (otherwise posts are reloaded when coming back)
                    // With the use of a local database I can easily restore the state without the back stack
                    // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                    // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                    .addToBackStack(null)
                    .commit()
        }

        /**
         * Retrieves the correct fragment for when subreddit is clicked in the nav bar
         *
         *
         * If this is the first time clicking the icon the subreddit list is shown.
         * If clicked when already in the subreddit nav bar the subreddit list is shown.
         * If clicked when not already in the subreddit, the previous subreddit fragment selected is shown
         *
         * @return The correct fragment to show
         */
        private fun onNavBarSubreddit(): Fragment {
            // No subreddit created (first time here), or we in a subreddit looking to get back
            return if (activeSubreddit == null) {
                if (selectSubredditFragment == null) {
                    selectSubredditFragment = SelectSubredditFragment.newInstance().also {
                        it.subredditSelected = this@MainActivity
                    }
                }
                selectSubredditFragment!!
            } else {
                activeSubreddit!!
            }
        }

        /**
         * Retrieves the correct fragment for profiles
         *
         * @return If a user is not logged in a [LogInFragment] is shown. If a user is logged in either
         * a [ProfileFragment] or [InboxFragment] is shown, depending on if there are unread messages or not
         */
        private fun onNavBarProfile(): Fragment {
            // If logged in, show profile, otherwise show log in page
            return if (App.get().isUserLoggedIn()) {
                // Go to inbox if there are unread messages
                // unreadMessages should maybe be synchronized? Dunno tbh
                if (unreadMessages != 0) {
                    profileLastShownIsProfile = false

                    if (inboxFragment == null) {
                        inboxFragment = InboxFragment.newInstance()
                    }
                    inboxFragment!!
                } else {
                    when (profileLastShownIsProfile) {
                        // Profile shown last, or first time showing (and no inbox messages), show profile
                        null, true -> {
                            if (profileFragment == null) {
                                profileFragment = ProfileFragment.newInstance()
                            }

                            profileFragment!!.onInboxClicked = this@MainActivity
                            profileFragment!!
                        }

                        // Inbox shown last, show inbox again
                        false -> {
                            if (inboxFragment == null) {
                                inboxFragment = InboxFragment.newInstance()
                            }
                            inboxFragment!!
                        }
                    }
                }
            } else {
                if (logInFragment == null) {
                    logInFragment = LogInFragment.newInstance()
                }
                logInFragment!!
            }
        }
    }
}