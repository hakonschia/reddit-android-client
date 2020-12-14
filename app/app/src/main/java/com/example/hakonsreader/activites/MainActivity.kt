package com.example.hakonsreader.activites

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityMainBinding
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter
import com.example.hakonsreader.fragments.*
import com.example.hakonsreader.interfaces.OnInboxClicked
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.interfaces.OnUnreadMessagesBadgeSettingChanged
import com.example.hakonsreader.misc.TokenManager
import com.example.hakonsreader.misc.Util
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity(), OnSubredditSelected, OnInboxClicked, OnUnreadMessagesBadgeSettingChanged {

    companion object {
        private const val TAG = "MainActivity"

        private const val POSTS_FRAGMENT = "postsFragment"
        private const val ACTIVE_SUBREDDIT_FRAGMENT = "activeSubredditFragment"
        private const val SELECT_SUBREDDIT_FRAGMENT = "selectSubredditFragment"
        private const val PROFILE_FRAGMENT = "profileFragment"
        private const val ACTIVE_NAV_ITEM = "activeNavItem"

        /**
         * The key used to store the name of the subreddit represented in [MainActivity.activeSubreddit]
         *
         * When this key holds a value it does not necessarily mean the subreddit was active (shown on screen)
         * at the time the instance was saved, but means that when clicking on the subreddit navbar, this should
         * be shown again instead of the list of subreddits
         */
        private const val ACTIVE_SUBREDDIT_NAME = "active_subreddit_name"
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var savedState = Bundle()

    private val db = RedditDatabase.getInstance(this)

    /**
     * The amount of unread messages in the inbox
     */
    private var unreadMessages = 0

    // The fragments to show in the nav bar
    private var postsFragment: PostsContainerFragment? = null
    private var activeSubreddit: SubredditFragment? = null
    private var selectSubredditFragment: SelectSubredditFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var inboxFragment: InboxFragment? = null
    private var logInFragment: LogInFragment? = null
    private var settingsFragment: SettingsFragment? = null
    private var lastShownFragment: Fragment? = null
    private val navigationViewListener: BottomNavigationViewListener = BottomNavigationViewListener()


    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch back to the app theme (from the launcher theme)
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAccessTokenScopes()
        startInboxListener()
        attachFragmentChangeListener()

        // For testing purposes hardcode going into a subreddit/post etc.
        val intent = Intent(this, DispatcherActivity::class.java)
        // TODO there are some issues with links, if a markdown link has superscript inside of it, markwon doesnt recognize it (also spaces in links causes issues)
        //  https://www.reddit.com/r/SpeedyDrawings/comments/jgg06k/this_gave_me_a_mild_heart_attack/
        intent.putExtra(DispatcherActivity.URL_KEY, "https://www.reddit.com/r/hakonschia/comments/jtyz0e/cool_image/")
        //startActivity(intent)

        if (savedInstanceState != null) {
            savedState = savedInstanceState
            restoreFragmentStates(savedInstanceState)
        } else {
            // Only setup the start fragment if we have no state to restore (as this is then a new activity)
            setupStartFragment()
        }

        setupNavBar(savedInstanceState)
        App.get().registerReceivers()
    }

    override fun onResume() {
        super.onResume()
        App.get().setActiveActivity(this)

        val uri = intent.data ?: return

        // Resumed from OAuth authorization
        if (uri.toString().startsWith(NetworkConstants.CALLBACK_URL)) {
            handleOAuthResume(uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.get().unregisterReceivers()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // If fragments aren't null, save them
        // Save which fragment is the active one as well

        postsFragment?.let {
            it.saveState(outState)
            if (it.isAdded) {
                supportFragmentManager.putFragment(outState, POSTS_FRAGMENT, postsFragment!!)
            }
        }

        activeSubreddit?.let {
            // If there is an active subreddit it won't be null, store the state of it even if it isn't
            // currently added (shown on screen)
            outState.putString(ACTIVE_SUBREDDIT_NAME, it.getSubredditName())
            it.saveState(outState)
            if (it.isAdded) {
                supportFragmentManager.putFragment(outState, ACTIVE_SUBREDDIT_FRAGMENT, activeSubreddit!!)
            }
        }
        if (selectSubredditFragment != null && selectSubredditFragment!!.isAdded) {
            supportFragmentManager.putFragment(outState, SELECT_SUBREDDIT_FRAGMENT, selectSubredditFragment!!)
        }
        if (profileFragment != null && profileFragment!!.isAdded) {
            supportFragmentManager.putFragment(outState, PROFILE_FRAGMENT, profileFragment!!)
        }

        // TODO store state of settings so it knows where to scroll to? Can maybe do it inside the fragment
        // Login/settings fragments can just be recreated when needed as they don't store any specific state

        // Store state of navbar
        outState.putInt(ACTIVE_NAV_ITEM, binding.bottomNav.selectedItemId)
    }

    override fun onBackPressed() {
        val activeFragment = getActiveFragment()
        // In a subreddit, and the last item was the list, go back to the list
        if (activeFragment is SubredditFragment && lastShownFragment is SelectSubredditFragment) {
            activeSubreddit = null

            // If the fragment has been killed by the OS make a new one (after a while it might be killed)
            if (selectSubredditFragment == null) {
                selectSubredditFragment = SelectSubredditFragment()
            }

            // Since we are in a way going back in the same navbar item, use the close transition
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, selectSubredditFragment!!)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit()
        } else if (activeFragment is InboxFragment && lastShownFragment is ProfileFragment) {
            // In the inbox, and the last active was the profile, go back to the profile
            if (profileFragment == null) {
                profileFragment = ProfileFragment.newInstance()
            }

            navigationViewListener.profileLastShownIsProfile = true

            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, profileFragment!!)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit()
        } else {
            binding.bottomNav.selectedItemId = R.id.navHome
        }
    }

    /**
     * Called when a subreddit has been selected from a [SelectSubredditFragment] fragment
     *
     * A new instance of [SubredditFragment] is created and shown
     *
     * @param subredditName The subreddit selected
     */
    override fun subredditSelected(subredditName: String?) {
        // Create new subreddit fragment and replace
        activeSubreddit = SubredditFragment.newInstance(subredditName!!)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, activeSubreddit!!)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
    }

    override fun onInboxClicked() {
        if (inboxFragment == null) {
            inboxFragment = InboxFragment()
        }

        navigationViewListener.profileLastShownIsProfile = false

        // Should maybe be sure that the profile navbar is clicked? Dunno
        // Change the navbar name to be "Inbox" maybe?
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, inboxFragment!!)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
    }

    override fun showUnreadMessagesBadge(show: Boolean) {
        val badge = binding.bottomNav.getBadge(binding.bottomNav.menu.findItem(R.id.navProfile).itemId)
        badge?.isVisible = show
    }

    /**
     * Handles when the activity resumes from an OAuth intent. If the intent is a successful
     * login attempt, then the user info is retrieved
     */
    private fun handleOAuthResume(uri: Uri) {
        val state = uri.getQueryParameter("state")

        // Not a match from the state we generated, something weird is happening
        if (state == null || state != App.get().oauthState) {
            Util.showErrorLoggingInSnackbar(binding.parentLayout)
            return
        }

        val code = uri.getQueryParameter("code")
        if (code == null) {
            Util.showErrorLoggingInSnackbar(binding.parentLayout)
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
                    when (val userInfo = api.user().info()) {
                        // Get information about the user. If this fails it doesn't really matter, as it isn't
                        // strictly needed at this point
                        is ApiResponse.Success -> App.storeUserInfo(userInfo.value)
                        is ApiResponse.Error -> {}
                    }

                    // Re-create the start fragment as it now should load posts for the logged in user
                    // TODO this is kinda bad as it gets posts and then gets posts again for the logged in user
                    withContext(Main) {
                        postsFragment = null
                        setupStartFragment()
                        Snackbar.make(binding.parentLayout, R.string.loggedIn, BaseTransientBottomBar.LENGTH_SHORT).show()
                    }
                }

                is ApiResponse.Error -> {
                    Util.handleGenericResponseErrors(binding.parentLayout, resp.error, resp.throwable)
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
            val view = layoutInflater.inflate(R.layout.dialog_title_new_permissions, binding.parentLayout, false)

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

        // Run every 30 minutes
        fixedRateTimer("inboxTimer", false, 0L, 1 * 60 * 1000) {
            println("MainActivity: inboxTimer running")
            /*
            CoroutineScope(IO).launch {
                // Get all messages
                // We can get only the new messages, but if the user has viewed messages outside the application
                // the total inbox with read messages would go unsynced


                when (val response = api.messages().inbox()) {
                    is ApiResponse.Success -> db.messages().insertAll(response.value)
                    is ApiResponse.Error -> {}
                }
            }
             */
        }
    }

    /**
     * Observes the unread messages in the local database and updates the profile navbar accordingly
     */
    private fun observeUnreadMessages() {
        val unread = db.messages().unreadMessages
        unread.observe(this, { m ->
            unreadMessages = m.size

            // TODO add listener to settings fragment to update this when the setting changes
            if (App.get().showUnreadMessagesBadge()) {
                val profileItemId = binding.bottomNav.menu.findItem(R.id.navProfile).itemId

                // No unread messages, remove the badge
                if (unreadMessages == 0) {
                    binding.bottomNav.removeBadge(profileItemId)
                } else {
                    // Add or create a badge and update the number
                    val badge = binding.bottomNav.getOrCreateBadge(profileItemId)
                    badge.isVisible = true
                    badge.number = unreadMessages
                }
            }
        })
    }

    /**
     * Attaches a listener to [getSupportFragmentManager] that stores fragments when detached
     * and saves it to [lastShownFragment]
     */
    private fun attachFragmentChangeListener() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                super.onFragmentDetached(fm, f)
                // InboxGroupFragment is an "inner" fragment and not one we want to store directly
                if (f !is InboxGroupFragment) {
                    lastShownFragment = f
                }
            }
        }, false)
    }

    /**
     * Updates the language
     *
     * @param language The language to switch to. If this is `null` the language found in
     * SharedPreferences will be used
     */
    fun updateLanguage(language: String?) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        var lang = language ?: settings.getString(getString(R.string.prefs_key_language), getString(R.string.prefs_default_language))
        // TODO this
        //updateLocale(new Locale(language));
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
        postsFragment = supportFragmentManager.getFragment(restoredState, POSTS_FRAGMENT) as PostsContainerFragment?
        activeSubreddit = supportFragmentManager.getFragment(restoredState, ACTIVE_SUBREDDIT_FRAGMENT) as SubredditFragment?
        selectSubredditFragment = supportFragmentManager.getFragment(restoredState, SELECT_SUBREDDIT_FRAGMENT) as SelectSubredditFragment?
        profileFragment = supportFragmentManager.getFragment(restoredState, PROFILE_FRAGMENT) as ProfileFragment?
        if (postsFragment == null) {
            postsFragment = PostsContainerFragment()
            postsFragment!!.restoreState(restoredState)
        }

        // Active subreddit not restored directly, check if it should be restored manually
        if (activeSubreddit == null) {
            val activeSubredditName = restoredState.getString(ACTIVE_SUBREDDIT_NAME)
            if (activeSubredditName != null) {
                // The state of the fragment itself is restored when it is accessed again
                activeSubreddit = SubredditFragment.newInstance(activeSubredditName)
            }
        }

        // Restore the listeners as it won't be set automatically
        profileFragment?.onInboxClicked = this
        selectSubredditFragment?.subredditSelected = this
    }

    /**
     * Creates the posts fragment and replaces it in the container view
     */
    private fun setupStartFragment() {
        if (postsFragment == null) {
            postsFragment = PostsContainerFragment()
        }

        // Use an open transition since we're calling this when the app has been started
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, postsFragment!!)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
    }

    /**
     * Sets up the navbar to be able to switch between the fragments
     *
     * @param restoredState The restored state of the activity. If this isn't null (the activity is
     * restored from a previous point) the active nav bar item is set to what is
     * stored in the state
     */
    private fun setupNavBar(restoredState: Bundle?) {
        binding.bottomNav.setOnNavigationItemSelectedListener(navigationViewListener)

        // Set listener for when an item has been clicked when already selected
        binding.bottomNav.setOnNavigationItemReselectedListener { item ->
            when (item.itemId) {
                // When "Subreddit" is clicked when already in "subreddit" go back to the subreddit list
                R.id.navSubreddit -> {
                    activeSubreddit = null
                    if (selectSubredditFragment == null) {
                        selectSubredditFragment = SelectSubredditFragment()
                        selectSubredditFragment!!.subredditSelected = this
                    }

                    // Since we are in a way going back in the same navbar item, use the close transition
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, selectSubredditFragment!!)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            .commit()
                }

                // In the inbox, go back to profile
                R.id.navProfile -> {
                    if (profileFragment == null) {
                        profileFragment = ProfileFragment.newInstance()
                    }

                    navigationViewListener.profileLastShownIsProfile = true

                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, profileFragment!!)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            .commit()
                }
            }
        }

        if (restoredState != null) {
            val active = restoredState.getInt(ACTIVE_NAV_ITEM)

            // When we're restoring a state we don't want to play an animation, as the user hasn't manually
            // selected a change
            navigationViewListener.disableAnimationForNextChange()
            binding.bottomNav.selectedItemId = active
        }
    }


    /**
     * Selects the profile nav bar
     */
    fun selectProfileNavBar() {
        binding.bottomNav.selectedItemId = R.id.navProfile
    }


    /**
     * Custom BottomNavigationView item selection listener
     */
    inner class BottomNavigationViewListener : BottomNavigationView.OnNavigationItemSelectedListener {
        // TODO this class should probably be decoupled from MainActivity (need to pass the functions to call or something)
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
                    if (postsFragment == null) {
                        postsFragment = PostsContainerFragment()
                    }
                    selected = postsFragment as PostsContainerFragment
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
                        settingsFragment = SettingsFragment()
                    }
                    settingsFragment!!.setUnreadMessagesBadgeSettingChanged(this@MainActivity)
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
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            transaction
                    .replace(R.id.fragmentContainer, fragment) // Although we don't use the backstack to pop elements, it is needed to keep the state
                    // of the fragments (otherwise posts are reloaded when coming back)
                    // With the use of a local database I can easily restore the state without the back stack
                    // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                    // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                    //.addToBackStack(null)
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
            supportFragmentManager.beginTransaction() // TODO if there is an ongoing transition and the user selects another nav bar item, the app crashes (need to somehow cancel the ongoing transition or something)
                    .setCustomAnimations(if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left, if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right)
                    .replace(R.id.fragmentContainer, fragment) // Although we don't use the backstack to pop elements, it is needed to keep the state
                    // of the fragments (otherwise posts are reloaded when coming back)
                    // With the use of a local database I can easily restore the state without the back stack
                    // Not sure whats best to use, with addToBackStack it's smoother as it doesn't have to load
                    // from db etc. (it doesn't take a long time) but it probably uses more ram to hold everything in memory?
                    //.addToBackStack(null)
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
                    selectSubredditFragment = SelectSubredditFragment().also {
                        it.subredditSelected = this@MainActivity
                    }
                }
                selectSubredditFragment!!
            } else {
                activeSubreddit!!.restoreState(savedState)
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
                        inboxFragment = InboxFragment()
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
                                inboxFragment = InboxFragment()
                            }
                            inboxFragment!!
                        }
                    }
                }
            } else {
                if (logInFragment == null) {
                    logInFragment = LogInFragment()
                }
                logInFragment!!
            }
        }
    }
}