package com.example.hakonsreader.activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditMulti
import com.example.hakonsreader.api.model.RedditUserInfo
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.TrendingSubreddits
import com.example.hakonsreader.api.persistence.RedditMessagesDao
import com.example.hakonsreader.api.persistence.RedditSubredditsDao
import com.example.hakonsreader.api.persistence.RedditUserInfoDao
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.broadcastreceivers.InboxWorkerStartReceiver
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_ID_INBOX_STATUS
import com.example.hakonsreader.constants.DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS
import com.example.hakonsreader.constants.NetworkConstants
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.databinding.ActivityMainBinding
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter
import com.example.hakonsreader.fragments.*
import com.example.hakonsreader.interfaces.*
import com.example.hakonsreader.misc.*
import com.example.hakonsreader.recyclerviewadapters.RedditMultiAdapter
import com.example.hakonsreader.states.LoggedInState
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter
import com.example.hakonsreader.recyclerviewadapters.TrendingSubredditsAdapter
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.states.OAuthState
import com.example.hakonsreader.viewmodels.RedditMultiViewModel
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel
import com.example.hakonsreader.viewmodels.TrendingSubredditsViewModel
import com.example.hakonsreader.viewmodels.assistedViewModel
import com.example.hakonsreader.views.util.goneIf
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity(), OnSubredditSelected, OnInboxClicked, OnUnreadMessagesBadgeSettingChanged {

    companion object {
        @Suppress("UNUSED")
        private const val TAG = "MainActivity"


        /**
         * The key used in [onSaveInstanceState] to save the state of the standard sub container fragment
         */
        private const val SAVED_STANDARD_SUB_CONTAINER_FRAGMENT = "saved_standardSubContainerFragment"

        /**
         * The key used in [onSaveInstanceState] to save the state of the active subreddit fragment
         */
        private const val SAVED_ACTIVE_SUBREDDIT_FRAGMENT = "saved_activeSubredditFragment"

        /**
         * The key used in [onSaveInstanceState] to save the state of the active multi fragment
         */
        private const val SAVED_ACTIVE_MULTI_FRAGMENT = "saved_activeMultiFragment"

        /**
         * The key used in [onSaveInstanceState] to save the state of the select subreddit fragment
         */
        private const val SAVED_SELECT_SUBREDDIT_FRAGMENT = "saved_selectSubredditFragment"

        /**
         * The key used in [onSaveInstanceState] to save the state of the profile fragment
         */
        private const val SAVED_PROFILE_FRAGMENT = "saved_profileFragment"

        /**
         * The key used in [onSaveInstanceState] to save the state of the settings fragment
         */
        private const val SAVED_SETTINGS_FRAGMENT = "saved_settingsFragment"

        /**
         * The saved position of the bottom navigation bars active item position
         */
        private const val SAVED_ACTIVE_NAV_ITEM = "saved_activeNavItem"

        /**
         * The key used to store the name of the subreddit represented in [MainActivity.activeSubreddit]
         *
         * When this key holds a value it does not necessarily mean the subreddit was active (shown on screen)
         * at the time the instance was saved, but means that when clicking on the subreddit navbar, this should
         * be shown again instead of the list of subreddits
         */
        private const val ACTIVE_SUBREDDIT_NAME = "saved_main_activity_active_subreddit_name"

        /**
         * The key used in [onSaveInstanceState] to store if the activity recreate is because the
         * application is changing to a new user
         */
        private const val SAVED_RECREATED_AS_NEW_USER = "saved_recreatedAsNewUser"

        /**
         * When creating this activity, set this on the extras to select the subreddit to show by default
         *
         * The value with this key should be a [String]
         */
        const val EXTRAS_START_SUBREDDIT = "extras_MainActivity_startSubreddit"
    }

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var subredditsDao: RedditSubredditsDao

    @Inject
    lateinit var messagesDao: RedditMessagesDao

    @Inject
    lateinit var userInfoDao: RedditUserInfoDao

    private lateinit var binding: ActivityMainBinding

    /**
     * The amount of unread messages in the inbox
     */
    private var unreadMessages = 0

    /**
     * If set to true the first call to [onSaveInstanceState] will ignore the fragment states and only
     * store the current active nav bar position
     */
    private var recreateAsNewUser = false

    // The fragments to show in the nav bar
    private var standardSubFragment: StandardSubContainerFragment? = null
    private var activeSubreddit: SubredditFragment? = null
    private var activeMulti: MultiFragment? = null
    private var selectSubredditFragment: SelectSubredditFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var inboxFragment: InboxFragment? = null
    private var settingsFragment: SettingsFragment? = null
    private var lastShownFragment: Fragment? = null
    private val navigationViewListener = BottomNavigationViewListener()

    @Inject
    lateinit var selectSubredditsViewModelFactory: SelectSubredditsViewModel.Factory
    private val subredditsViewModel: SelectSubredditsViewModel by assistedViewModel {
        // Currently we don't care about the saved state handle here
        selectSubredditsViewModelFactory.create(AppState.loggedInState.value is LoggedInState.LoggedIn)
    }

    @Inject
    lateinit var redditMultiViewModelFactory: RedditMultiViewModel.Factory
    private val redditMultiViewModel: RedditMultiViewModel by assistedViewModel {
        // Currently we don't care about the saved state handle here

        val loggedInState = AppState.loggedInState.value

        val username = if (loggedInState is LoggedInState.LoggedIn) {
            loggedInState.userInfo.userInfo?.username
        } else {
            null
        }
        redditMultiViewModelFactory.create(username)
    }

    private val trendingSubredditsViewModel: TrendingSubredditsViewModel by viewModels()

    /**
     * The counter for toggling developer mode
     */
    private var devModeCounter = 0

    /**
     * The job responsible for resetting [devModeCounter]
     */
    private var devModeJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch back to the app theme (from the launcher theme)
        // This has to be here and not in the manifest, otherwise the launcher theme won't be active
        // during the startup (for the splash screen)
        setTheme(R.style.AppTheme)
        val prefs = getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, MODE_PRIVATE)
        SharedPreferencesManager.create(prefs)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        // For testing purposes hardcode going into a subreddit/post etc.
        // TODO there are some issues with links, if a markdown link has superscript inside of it, markwon doesnt recognize it (also spaces in links causes issues)
        //  https://www.reddit.com/r/SpeedyDrawings/comments/jgg06k/this_gave_me_a_mild_heart_attack/
        Intent(this, DispatcherActivity::class.java).run {
            // Reddit gallery with video: https://www.reddit.com/r/GlobalOffensive/comments/lvv9dm/hey_hey_new_clear_polymer_skin_made_its_way_in/
            putExtra(DispatcherActivity.EXTRAS_URL_KEY, "https://reddit.com/r/hakonschia/comments/k22ft8/dw4423/")
            //startActivity(this)
        }

        val uri = intent.data
        if (uri != null) {
            // Resumed from OAuth authorization
            if (uri.toString().startsWith(NetworkConstants.CALLBACK_URL)) {
                handleOAuthResume(uri)
            } else {
                performSetup(savedInstanceState)
            }
        } else {
            performSetup(savedInstanceState)
        }
    }

    /**
     * Performs setup of the activity
     *
     * @param savedInstanceState The saved instance state
     */
    private fun performSetup(savedInstanceState: Bundle?) {
        attachFragmentChangeListener()
        setupNavBar()

        val recreatedAsNewUser = savedInstanceState?.getBoolean(SAVED_RECREATED_AS_NEW_USER, false)

        if (savedInstanceState != null) {
            // recreatedAsNewUser will never be null if we get here
            // Restart any potential workers to update the inbox for the new user
            if (recreatedAsNewUser!!) {
                InboxWorkerStartReceiver.startInboxWorker(this, Settings.inboxUpdateFrequency(), replace = true)
            }

            restoreFragmentStates(savedInstanceState, recreatedAsNewUser)
            restoreNavBar(savedInstanceState)
        } else {
            // Use empty string as default (ie. front page)
            val startSubreddit = intent.extras?.getString(EXTRAS_START_SUBREDDIT) ?: ""
            // Only setup the start fragment if we have no state to restore (as this is then a new activity)
            setupStartFragment(startSubreddit)

            // Trending subreddits aren't user specific so they don't have to be retrieved again
            trendingSubredditsViewModel.loadSubreddits()
        }

        observeUserState()
        observeUnreadMessages()

        val forLoggedInUser = AppState.loggedInState.value is LoggedInState.LoggedIn

        subredditsViewModel.isForLoggedInUser = forLoggedInUser
        subredditsViewModel.loadSubreddits(force = recreatedAsNewUser == true)

        if (forLoggedInUser) {
            // Could have been recreated as a new user, so ensure the username is updated
            val loggedInState = AppState.loggedInState.value

            redditMultiViewModel.username = (loggedInState as LoggedInState.LoggedIn).userInfo.userInfo?.username
            redditMultiViewModel.loadMultis(force = recreatedAsNewUser == true)
        }

        setupNavDrawer()
        checkAccessTokenScopes()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Store state of nav bar
        // TODO this will not store nested items (like subreddits) and therefore wont restore correctly
        //  when recreating as a new user
        outState.putInt(SAVED_ACTIVE_NAV_ITEM, binding.bottomNav.selectedItemId)
        outState.putBoolean(SAVED_RECREATED_AS_NEW_USER, recreateAsNewUser)

        settingsFragment?.let {
            // The fragments will sometimes fail to save their state because they aren't added,
            // but by using it.isAdded the state of a fragment will ONLY be saved in configuration changes
            // if the fragment is the one on the screen (ie. only one fragment will ever save its state)
            // These crashes only seem to occur when the app is in the background, but those crashes
            // might cause the app to have to restart more often after being closed (but not killed)
            try {
                supportFragmentManager.putFragment(outState, SAVED_SETTINGS_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }

        if (recreateAsNewUser) {
            recreateAsNewUser = false
            return
        }

        standardSubFragment?.let {
            try {
                supportFragmentManager.putFragment(outState, SAVED_STANDARD_SUB_CONTAINER_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }

        activeSubreddit?.let {
            // If there is an active subreddit it won't be null, store the state of it even if it isn't
            // currently added (shown on screen)
            outState.putString(ACTIVE_SUBREDDIT_NAME, it.subredditName)
            try {
                supportFragmentManager.putFragment(outState, SAVED_ACTIVE_SUBREDDIT_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }

        activeMulti?.let {
            try {
                supportFragmentManager.putFragment(outState, SAVED_ACTIVE_MULTI_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }

        profileFragment?.let {
            try {
                supportFragmentManager.putFragment(outState, SAVED_PROFILE_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }

        selectSubredditFragment?.let {
            try {
                supportFragmentManager.putFragment(outState, SAVED_SELECT_SUBREDDIT_FRAGMENT, it)
            } catch (e: IllegalStateException) { }
        }
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

                // Since we are in a way going back in the same nav bar item, use the close transition
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

        // Enable the icon on the toolbar as a menu icon, as it opens the nav drawer
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
     * Called when a subreddit has been selected. An instance of [SubredditFragment] will be shown.
     * If the selected subreddit is a default subreddit (not including "mod") then the subreddit will
     * be selected in the home nav bar (the default sub container). Otherwise, the subreddit will be
     * shown in the subreddit nav bar, and if [activeSubreddit] points to the same name as given here
     * it will be reused instead of creating a new one.
     *
     * This will hide the keyboard, if shown
     *
     * @param subredditName The subreddit selected
     */
    override fun subredditSelected(subredditName: String) {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        val lowerCased = subredditName.toLowerCase(Locale.ROOT)

        // If default sub, use the home nav bar instead
        // Currently, we don't show "mod" in the default sub container
        if (RedditApi.STANDARD_SUBS.contains(lowerCased) && lowerCased != "mod") {
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
            if (subredditName.equals("random", ignoreCase = true) || activeSubreddit?.subredditName != subredditName) {
                activeSubreddit = SubredditFragment.newInstance(subredditName)
            }

            // Already in the subreddit nav bar, open subreddit with opening transition
            if (binding.bottomNav.selectedItemId == R.id.navSubreddit) {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, activeSubreddit!!)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(null)
                        .commit()
            } else {
                // Otherwise select the subreddit nav bar, which will use activeSubreddit and deal with animations
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

        // Should maybe be sure that the profile nav bar is clicked? Dunno
        // Change the nav bar name to be "Inbox" maybe?
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

    private fun multiSelected(multi: RedditMulti) {
        activeMulti = MultiFragment.newInstance(multi)

        // Already in the subreddit nav bar, open Multi with opening transition
        if (binding.bottomNav.selectedItemId == R.id.navSubreddit) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, activeMulti!!)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit()
        } else {
            // Otherwise select the subreddit nav bar, which will use activeSubreddit and deal with animations
            binding.bottomNav.selectedItemId = R.id.navSubreddit
        }

        // If this was called from a drawer it should close (otherwise the drawer already is closed)
        binding.mainParentLayout.closeDrawer(GravityCompat.START)
    }

    private fun observeUserState() {
        AppState.loggedInState.observe(this) {
            // PrivatelyBrowsing and LoggedIn are both treated as having a user
            binding.navDrawer.hasUser = it !is LoggedInState.LoggedOut

            when (it) {
                is LoggedInState.LoggedIn -> asLoggedInUser(it.userInfo)
                is LoggedInState.PrivatelyBrowsing -> asPrivatelyBrowsingUser(it.userInfo)
                LoggedInState.LoggedOut -> asLoggedOutUser()
            }
        }
    }

    private fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.navDrawer.privatelyBrowsing = privatelyBrowsing
    }

    /**
     * Sets various things when the activity is in a context
     */
    private fun asLoggedInUser(userInfo: RedditUserInfo) {
        privateBrowsingStateChanged(false)
        binding.navDrawer.userInfo = userInfo

        val user = userInfo.userInfo
        if (user != null) {
            binding.bottomNav.menu.findItem(R.id.navProfile).title = user.username
        }
    }

    /**
     * Sets various things when the activity is in a private browsing context
     */
    private fun asPrivatelyBrowsingUser(userInfo: RedditUserInfo) {
        privateBrowsingStateChanged(true)
        binding.navDrawer.userInfo = userInfo

        val user = userInfo.userInfo
        if (user != null) {
            binding.bottomNav.menu.findItem(R.id.navProfile).title = user.username
        }
    }

    /**
     * Sets various things when the activity is in a context of a logger out user
     */
    private fun asLoggedOutUser() {
        privateBrowsingStateChanged(false)

        binding.navDrawer.userInfo = null
        binding.bottomNav.menu.findItem(R.id.navProfile).title = getString(R.string.navbarProfile)

        // No need to keep this at this point
        profileFragment = null
    }

    /**
     * Recreates the activity as a new user
     */
    fun recreateAsNewUser() {
        recreateAsNewUser = true

        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach {
            transaction.remove(it)
        }
        transaction.commit()

        recreate()
    }

    /**
     * Handles when the activity resumes from an OAuth intent. If the intent is a successful
     * login attempt, then the user info is retrieved
     */
    private fun handleOAuthResume(uri: Uri) {
        val state = uri.getQueryParameter("state")

        // Not a match from the state we generated, something weird is happening
        if (state == null || state != OAuthState.state) {
            showErrorLoggingInSnackbar(binding.mainParentLayout, binding.bottomNav)
            performSetup(null)
            return
        }

        val code = uri.getQueryParameter("code")
        if (code == null) {
            showErrorLoggingInSnackbar(binding.mainParentLayout, binding.bottomNav)
            performSetup(null)
            return
        }

        OAuthState.clear()

        // This might be bad, but onCreate is called with the same intent when the activity
        // is recreated which would cause this to run again, so we have to clear the intent
        intent.replaceExtras(Bundle())
        intent.action = ""
        intent.data = null
        intent.flags = 0

        CoroutineScope(IO).launch {
            when (val resp = api.accessToken().get(code)) {
                is ApiResponse.Success -> {
                    AppState.addNewUser(resp.value)
                    getUserInfo()

                    withContext(Main) {
                        Snackbar.make(binding.mainParentLayout, R.string.loggedIn, BaseTransientBottomBar.LENGTH_SHORT)
                                .setAnchorView(binding.bottomNav)
                                .show()
                    }
                }

                is ApiResponse.Error -> {
                    handleGenericResponseErrors(binding.mainParentLayout, resp.error, resp.throwable, binding.bottomNav)
                }
            }
        }
    }

    /**
     * Gets user info, if this fails then a snackbar is shown to let the user know and attempt again
     */
    private fun getUserInfo() {
        CoroutineScope(IO).launch {
            when (val userInfo = api.user().info()) {
                is ApiResponse.Success -> {
                    AppState.updateUserInfo(info = userInfo.value)
                    withContext(Main) {
                        performSetup(null)
                    }
                }
                is ApiResponse.Error -> {
                    // Seeing as this is called when the access token was just retrieved, it is very
                    // unlikely to fail, but just in case
                    Snackbar.make(binding.root, R.string.userInfoFailedToGet, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.userInfoFailedToGetTryAgain) {
                                getUserInfo()
                            }
                            .setAnchorView(binding.bottomNav)
                            .show()

                    // This should always happen
                    withContext(Main) {
                        performSetup(null)
                    }
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
     * Observes the unread messages in the local database and updates the profile navbar accordingly
     */
    private fun observeUnreadMessages() {
        val unread = messagesDao.getUnreadMessages()
        unread.observe(this, { m ->
            unreadMessages = m.size

            if (Settings.showUnreadMessagesBadge()) {
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
     * Attaches a listener to [getSupportFragmentManager] that stores fragments when detached
     * and saves it to [lastShownFragment]
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
                // Also don't save bottom sheets, as 1) they will leak (until something else is set)
                // and 2) they aren't relevant
                if (f !is InboxGroupFragment && f !is BottomSheetDialogFragment) {
                    lastShownFragment = f
                }
            }
        }, false)
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
     * @param onlyRestoreUserLess If true only fragments that are user less will be restored
     */
    private fun restoreFragmentStates(restoredState: Bundle, onlyRestoreUserLess: Boolean) {
        settingsFragment = supportFragmentManager.getFragment(restoredState, SAVED_SETTINGS_FRAGMENT) as SettingsFragment?

        if (onlyRestoreUserLess) {
            return
        }
        standardSubFragment = supportFragmentManager.getFragment(restoredState, SAVED_STANDARD_SUB_CONTAINER_FRAGMENT) as StandardSubContainerFragment?
        activeSubreddit = supportFragmentManager.getFragment(restoredState, SAVED_ACTIVE_SUBREDDIT_FRAGMENT) as SubredditFragment?
        activeMulti = supportFragmentManager.getFragment(restoredState, SAVED_ACTIVE_MULTI_FRAGMENT) as MultiFragment?
        selectSubredditFragment = supportFragmentManager.getFragment(restoredState, SAVED_SELECT_SUBREDDIT_FRAGMENT) as SelectSubredditFragment?
        profileFragment = supportFragmentManager.getFragment(restoredState, SAVED_PROFILE_FRAGMENT) as ProfileFragment?

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
        val active = restoredState.getInt(SAVED_ACTIVE_NAV_ITEM)

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
     * the [activeSubreddit] is set with the subreddit and is shown in the subreddit nav bar
     */
    private fun setupStartFragment(startSubreddit: String) {
        if (standardSubFragment == null) {
            standardSubFragment = StandardSubContainerFragment.newInstance()
        }

        val defaultSub = StandardSubContainerFragment.StandarSub.values().find { it.value == startSubreddit.toLowerCase(Locale.ROOT) }

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
                R.id.navHome -> {
                    // This is kinda hacky, but when the activity is recreated with a new user the
                    // fragment for some reason stays here even though this is nulled (unless we clear the fragment manager)
                    // and if it was called in the nav home this reselect will be called, so if it is null
                    // we just create a new one (if we later want some special functionality we can add it in the else)
                    // This is pretty much just the same functionality as when it is selected as normal
                    if (standardSubFragment == null) {
                        standardSubFragment = StandardSubContainerFragment.newInstance()

                        supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, standardSubFragment!!)
                                .addToBackStack(null)
                                .commit()
                    }
                }
                
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

                R.id.navSettings -> {
                    devModeJob?.cancel()
                    // Reset counter after 2.5 seconds
                    devModeJob = CoroutineScope(Main).launch {
                        delay(2500)
                        devModeCounter = 0
                    }

                    if (++devModeCounter == 5) {
                        if (AppState.toggleDeveloperMode()) {
                            Toast.makeText(this, R.string.developerModeEnabled, Toast.LENGTH_LONG).show()
                        } else {
                            NotificationManagerCompat.from(this)
                                    .cancel(DEVELOPER_NOTIFICATION_TAG_INBOX_STATUS, DEVELOPER_NOTIFICATION_ID_INBOX_STATUS)
                            Toast.makeText(this, R.string.developerModeDisabled, Toast.LENGTH_LONG).show()
                        }
                    }
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
     */
    private fun setupNavDrawer() {
        with(subredditsViewModel) {
            subreddits.observe(this@MainActivity) { subreddits ->
                (binding.navDrawer.subreddits.adapter as SubredditsAdapter)
                        .submitList(subreddits as MutableList<Subreddit>, true)

                // At least one sub where the user is a moderator
                binding.navDrawer.userIsModerator =  subreddits.find { sub -> sub.isModerator } != null
            }

            isLoading.observe(this@MainActivity) { loading ->
                binding.navDrawer.subredditsLoaderLayout.goneIf(!loading)
            }

            error.observe(this@MainActivity) { error ->
                handleGenericResponseErrors(binding.mainParentLayout, error.error, error.throwable, binding.bottomNav)
            }
        }

        with (trendingSubredditsViewModel) {
            trendingSubreddits.observe(this@MainActivity) { trending ->
                trending.subreddits?.let {
                    (binding.navDrawer.trendingSubreddits.adapter as TrendingSubredditsAdapter).submitList(it)
                }

                setTrendingSubredditsLastUpdated(trending)
            }

            isLoading.observe(this@MainActivity) { loading ->
                binding.navDrawer.trendingSubredditsLoaderLayout.goneIf(!loading)
            }

            error.observe(this@MainActivity) { error ->
                handleGenericResponseErrors(binding.mainParentLayout, error.error, error.throwable, binding.bottomNav)
            }
        }

        with (redditMultiViewModel) {
            multis.observe(this@MainActivity) { multis ->
                (binding.navDrawer.multis.adapter as RedditMultiAdapter)
                        .submitList(multis)

                binding.navDrawer.hasMultis = multis.isNotEmpty()
            }

            isLoading.observe(this@MainActivity) { loading ->
                binding.navDrawer.multisLoaderLayout.goneIf(!loading)
            }

            error.observe(this@MainActivity) { error ->
                handleGenericResponseErrors(binding.mainParentLayout, error.error, error.throwable, binding.bottomNav)
            }
        }

        with (binding.navDrawer) {
            appState = AppState
            api = this@MainActivity.api
            userInfoDao = this@MainActivity.userInfoDao
            isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

            profilePicture.setOnClickListener { selectProfileNavBar() }
            username.setOnClickListener { selectProfileNavBar() }
            settingsClicker.setOnClickListener { selectSettingsNavBar() }

            frontPageClicker.setOnClickListener { subredditSelected("") }
            popularClicker.setOnClickListener { subredditSelected("popular") }
            allClicker.setOnClickListener { subredditSelected("all") }
            modClicker.setOnClickListener { subredditSelected("mod") }
            randomClicker.setOnClickListener { subredditSelected("random") }

            darkModeIcon.setOnClickListener {
                PreferenceManager.getDefaultSharedPreferences(this@MainActivity).run {
                    val key = getString(R.string.prefs_key_theme)
                    val darkMode = getBoolean(key, resources.getBoolean(R.bool.prefs_default_theme))
                    edit().putBoolean(key, !darkMode).apply()

                    if (!darkMode) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }

            subreddits.adapter = SubredditsAdapter().apply {
                viewType = SubredditsAdapter.SubredditViewType.SIMPLE
                subredditSelected = this@MainActivity
                favoriteClicked = OnClickListener { subreddit -> subredditsViewModel.favorite(subreddit) }
            }
            subreddits.layoutManager = LinearLayoutManager(this@MainActivity)

            trendingSubredditsRefresh.setOnClickListener { trendingSubredditsViewModel.loadSubreddits() }
            trendingSubreddits.adapter = TrendingSubredditsAdapter().apply {
                subredditSelected = this@MainActivity
            }
            trendingSubreddits.layoutManager = LinearLayoutManager(this@MainActivity)

            trendingSubredditsLastUpdated.start()
            trendingSubredditsLastUpdated.setOnChronometerTickListener {
                trendingSubredditsViewModel.trendingSubreddits.value?.let { trending ->
                    setTrendingSubredditsLastUpdated(trending)
                }
            }

            multis.adapter = RedditMultiAdapter().apply {
                onMultiSelected = { multi ->
                    multiSelected(multi)
                }
            }
            multis.layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    /**
     * Sets the trending subreddits retrieved text in the nav drawer
     *
     * @param trendingSubreddits The trending subreddits object to use for the text
     */
    private fun setTrendingSubredditsLastUpdated(trendingSubreddits: TrendingSubreddits) {
        val now = System.currentTimeMillis() / 1000L
        val between = now - trendingSubreddits.retrieved
        setAgeTextTrendingSubreddits(binding.navDrawer.trendingSubredditsLastUpdated, between)
    }

    /**
     * Custom BottomNavigationView item selection listener
     *
     * The nav bar items work as following:
     * 1. "Home": Holds the standard sub container (front page, popular etc.)
     * 2. "Subreddit": [activeMulti] is chosen if not null, otherwise [activeSubreddit] is chosen, otherwise
     * [selectSubredditFragment] is chosen
     * 3. "Profile": If there are unread messages, an inbox fragment is shown. If the last fragment in the profile navbar
     * was shown, then it is shown again, otherwise the profile is shown, or a login fragment if no user is logged in
     * 4. "Settings": Only a settings fragment will be shown here
     */
    inner class BottomNavigationViewListener : BottomNavigationView.OnNavigationItemSelectedListener {
        private var playAnimationForNextChange = true

        /**
         * The current index the nav bar is in (from left to right). Used to determine what transition
         * to play when changing the nav bar item
         */
        private var navBarPos = 0

        /**
         * Flag indicating if that the last time the profile was selected in the nav bar, the profile
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
                    .setCustomAnimations(
                            if (goingRight) R.anim.slide_in_right else R.anim.slide_in_left,
                            if (goingRight) R.anim.slide_out_left else R.anim.slide_out_right)

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
         * If this is the first time clicking the icon the subreddit list is shown.
         * If clicked when already in the subreddit nav bar the subreddit list is shown.
         * If clicked when not already in the subreddit, the previous subreddit fragment selected is shown
         *
         * @return The correct fragment to show
         */
        private fun onNavBarSubreddit(): Fragment {
            return when {
                activeMulti != null -> activeMulti!!
                activeSubreddit != null -> activeSubreddit!!
                else -> {
                    if (selectSubredditFragment == null) {
                        selectSubredditFragment = SelectSubredditFragment.newInstance().also {
                            it.subredditSelected = this@MainActivity
                        }
                    }
                    selectSubredditFragment!!
                }
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
            return if (AppState.loggedInState.value is LoggedInState.LoggedOut) {
                LogInFragment.newInstance()
            } else {
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
            }
        }
    }
}