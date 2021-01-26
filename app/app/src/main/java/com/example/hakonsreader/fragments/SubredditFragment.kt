package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.ObservableField
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.PostActivity
import com.example.hakonsreader.activites.SubmitActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.databinding.*
import com.example.hakonsreader.dialogadapters.RedditFlairAdapter
import com.example.hakonsreader.interfaces.OnVideoFullscreenListener
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter
import com.example.hakonsreader.recyclerviewadapters.SubredditRulesAdapter
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener
import com.example.hakonsreader.viewmodels.PostsViewModel
import com.example.hakonsreader.viewmodels.factories.PostsFactory
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.util.ViewUtil
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.robinhood.ticker.TickerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubredditFragment : Fragment(), SortableWithTime, PrivateBrowsingObservable {

    companion object {
        private const val TAG = "SubredditFragment"

        /**
         * The key stored in [getArguments] saying the name the subreddit is for
         */
        private const val SUBREDDIT_NAME_KEY = "subredditName"

        private const val FIRST_VIEW_STATE_STORED_KEY = "first_view_state_stored"
        private const val LAST_VIEW_STATE_STORED_KEY = "last_view_state_stored"
        private const val VIEW_STATE_STORED_KEY = "view_state_stored"

        /**
         * The key used to store the post IDs the fragment is showing
         */
        private const val POST_IDS_KEY = "post_ids"

        /**
         * The key used to store the state of [postsLayoutManager]
         */
        private const val LAYOUT_STATE_KEY = "layout_state"


        /**
         * The key used in [getArguments] for how to sort the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [SortingMethods]
         */
        private const val SORT = "sort"

        /**
         * The key used in [getArguments] for the time sort for the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [PostTimeSort]
         */
        private const val TIME_SORT = "time_sort"

        /**
         * The key used to [getArguments] if the subreddit rules should automatically be shown
         * when entering the subreddit (does not apply for standard subs)
         *
         * The value with this key should be a [Boolean]
         */
        private const val SHOW_RULES = "show_rules"

        /**
         * Creates a new instance of the fragment
         *
         * @param subredditName The name of the subreddit to instantiate
         * @return The newly created fragment
         */
        fun newInstance(subredditName: String, sort: SortingMethods? = null, timeSort: PostTimeSort? = null, showRules: Boolean = false) = SubredditFragment().apply {
            arguments = Bundle().apply {
                putString(SUBREDDIT_NAME_KEY, subredditName)
                sort?.let { putString(SORT, it.value) }
                timeSort?.let { putString(TIME_SORT, it.value) }
                putBoolean(SHOW_RULES, showRules)
            }
        }
    }

    private val database = App.get().database
    private val api = App.get().api

    private var _binding: FragmentSubredditBinding? = null
    private val binding get() = _binding!!
    private var saveState: Bundle? = null
    private var postIds = ArrayList<String>()
    private var isDefaultSubreddit = false

    private var postsViewModel: PostsViewModel? = null
    private var postsAdapter: PostsAdapter? = null
    private var postsLayoutManager: LinearLayoutManager? = null
    private var postsScrollListener: PostScrollListener? = null

    private var rulesAdapter: SubredditRulesAdapter? = null
    private var rulesLayoutManager: LinearLayoutManager? = null
    /**
     * True if the rules for the subreddit has been loaded during this fragment
     */
    private var rulesLoaded = false

    /**
     * True if the user flairs for the subreddit has been loaded during this fragment
     */
    private var flairsLoaded = false


    /**
     * A DrawerListener for the drawer with subreddit info
     */
    var drawerListener: DrawerLayout.DrawerListener? = null


    private val subreddit: ObservableField<Subreddit> = object : ObservableField<Subreddit>() {
        override fun set(value: Subreddit) {
            // If there is no subscribers previously the ticker animation looks very weird
            // so disable it if it would like weird
            val old = this.get()
            val enableTickerAnimation = old != null && old.subscribers != 0
            binding.subredditSubscribers.animationDuration = (if (enableTickerAnimation) {
                resources.getInteger(R.integer.tickerAnimationDefault)
            } else {
                0
            }).toLong()

            // Probably not how ObservableField is supposed to be used? Works though
            super.set(value)

            ViewUtil.setSubredditIcon(binding.subredditIcon, value)
            binding.subreddit = value
            postsAdapter?.hideScoreTime = value.hideScoreTime
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()
        setupPostsList()
        setupSubredditObservable()
        setupSubmitPostFab()
        setupPostsViewModel()

        if (!isDefaultSubreddit) {
            setupRulesList()
            observeRules()
            observeUserFlairs()
            automaticallyOpenDrawerIfSet()
        }


        App.get().registerPrivateBrowsingObservable(this)

        // TODO if you go to settings, rotate and change theme, then the IDs wont be saved. The subreddit will be notified about
        //  the first change, save the state, but the state isn't restored again for the second save since it's restored here
        //  so there's nothing to restore. saveState() should use the bundle "saveState"
        saveState?.let {
            val ids = it.getStringArrayList(saveKey(POST_IDS_KEY))
            if (ids != null) {
                postIds = ids
                postsViewModel?.postIds = ids
            }
        }

        return binding.root
    }

    /**
     * Checks if there are posts already loaded. If there are no posts loaded [postsViewModel]
     * is notified to load posts automatically
     */
    override fun onResume() {
        super.onResume()

        // If the fragment is selected without any posts load posts automatically
        if (postsAdapter?.itemCount == 0) {
            // Starting from scratch
            if (postIds.isEmpty()) {
                val sort = arguments?.getString(SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
                val timeSort = arguments?.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }

                postsViewModel?.loadPosts(sort, timeSort)
            } else {
                // Post IDs restored, set those
                postsViewModel?.postIds = postIds
            }
        }

        restoreViewHolderStates()
    }

    override fun onPause() {
        super.onPause()
        saveViewHolderStates()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        App.get().unregisterPrivateBrowsingObservable(this)

        postsAdapter?.let {
            // Ensure that all videos are cleaned up
            for (i in 0 until it.itemCount) {
                val viewHolder = binding.posts.findViewHolderForLayoutPosition(i) as PostsAdapter.ViewHolder?
                viewHolder?.destroy()
            }

            it.lifecycleOwner = null
        }

        _binding = null
    }

    /**
     * Saves the state of the visible ViewHolders to [saveState]
     *
     * @see restoreViewHolderStates
     */
    private fun saveViewHolderStates() {
        if (saveState == null) {
            saveState = Bundle()
        }

        // TODO this should make use of the states stored by the adapter as that will have state
        //  for all previous posts, not just the visible ones (although we still have to call onUnselected to pause videos etc)
        saveState?.let { saveBundle ->
            // It's probably not necessary to loop through all, but ViewHolders are still active even when not visible
            // so just getting firstVisible and lastVisible probably won't be enough
            for (i in 0 until postsAdapter?.itemCount!!) {
                val viewHolder = binding.posts.findViewHolderForLayoutPosition(i) as PostsAdapter.ViewHolder?

                if (viewHolder != null) {
                    val extras = viewHolder.getExtras()
                    saveBundle.putBundle(saveKey(VIEW_STATE_STORED_KEY + i), extras)
                    viewHolder.onUnselected()
                }
            }

            postsLayoutManager?.let {
                val firstVisible = it.findFirstVisibleItemPosition()
                val lastVisible = it.findLastVisibleItemPosition()

                saveBundle.putInt(saveKey(FIRST_VIEW_STATE_STORED_KEY), firstVisible)
                saveBundle.putInt(saveKey(LAST_VIEW_STATE_STORED_KEY), lastVisible)
            }
        }
    }

    /**
     * Restores the state of the visible ViewHolders based on [saveState]
     *
     * @see saveViewHolderStates
     */
    private fun restoreViewHolderStates() {
        saveState?.let {
            val firstVisible = it.getInt(saveKey(FIRST_VIEW_STATE_STORED_KEY))
            val lastVisible = it.getInt(saveKey(LAST_VIEW_STATE_STORED_KEY))

            for (i in firstVisible..lastVisible) {
                val viewHolder = binding.posts.findViewHolderForLayoutPosition(i) as PostsAdapter.ViewHolder?

                if (viewHolder != null) {
                    // If the view has been destroyed the ViewHolders haven't been created yet
                    val extras = it.getBundle(saveKey(VIEW_STATE_STORED_KEY + i))
                    if (extras != null) {
                        viewHolder.setExtras(extras)
                    }
                }
            }
        }
    }

    /**
     * @return The name of the subreddit the fragment is for, or null if no subreddit is set
     */
    fun getSubredditName() : String? {
        return subreddit.get()?.name
    }

    /**
     * Converts a base key into a unique key for this subreddit, so that the subreddit state can be
     * stored in a global bundle holding states for multiple subreddits
     *
     * @param baseKey The base key to use
     * @return A key unique to this subreddit
     */
    private fun saveKey(baseKey: String) : String {
        return baseKey + "_" + getSubredditName()
    }

    /**
     * Saves the state of the fragment to a bundle. Restore the state with [restoreState]
     *
     * @param saveState The bundle to store the state to
     * @see restoreState
     */
    fun saveState(saveState: Bundle) {
        // If no items in the list it's no point in saving any state
        if (postIds.isEmpty()) {
            return
        }
        saveState.putStringArrayList(saveKey(POST_IDS_KEY), postIds)
        postsLayoutManager?.let { saveState.putParcelable(saveKey(LAYOUT_STATE_KEY), it.onSaveInstanceState()) }
    }

    /**
     * Restores the state stored for when the activity holding the fragment has been recreated in a
     * way that doesn't permit the fragment to store its own state
     *
     * @param state The bundle holding the stored state
     * @see saveState
     */
    fun restoreState(state: Bundle?) {
        // Might be asking for trouble by doing overriding saveState like this? This function
        // is meant to only be called when there is no saved state by the fragment
        if (state != null) {
            postIds = state.getStringArrayList(saveKey(POST_IDS_KEY)) ?: ArrayList()
        }
        saveState = state
    }


    /**
     * Inflates and sets up [binding]
     */
    private fun setupBinding() {
        _binding = FragmentSubredditBinding.inflate(layoutInflater)

        with (binding) {
            subredditRefresh.setOnClickListener { refreshPosts() }
            subscribe.setOnClickListener { subscribeOnclick() }
            subredditInfo.subscribe.setOnClickListener { subscribeOnclick() }

            subredditSubscribers.setCharacterLists(TickerUtils.provideNumberList())

            postsRefreshLayout.setOnRefreshListener {
                refreshPosts()

                // The refreshing will be visible with our own progress bar
                postsRefreshLayout.isRefreshing = false
            }
            postsRefreshLayout.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(requireContext(), R.color.colorAccent)
            )

            openDrawer.setOnClickListener { drawer.openDrawer(GravityCompat.END) }
            drawerListener?.let { drawer.addDrawerListener(it) }
            drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerOpened(drawerView: View) {
                    getSubredditName()?.let {
                        if (!rulesLoaded) {
                            retrieveSubredditRules(it)
                        }
                        if (!flairsLoaded) {
                            getSubmissionFlairs(it)
                        }
                    }
                }
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) { /* Not implemented */ }
                override fun onDrawerClosed(drawerView: View) { /* Not implemented */ }
                override fun onDrawerStateChanged(newState: Int) { /* Not implemented */ }
            })
        }

    }

    /**
     * Sets up [FragmentSubredditBinding.posts] and [postsAdapter]/[postsLayoutManager]
     */
    private fun setupPostsList() {
        postsAdapter = PostsAdapter().apply {
            lifecycleOwner = viewLifecycleOwner

            binding.posts.adapter = this
            onVideoManuallyPaused = OnVideoManuallyPaused { contentVideo ->
                // Ignore post when scrolling if manually paused
                postsScrollListener?.setPostToIgnore(contentVideo.redditPost?.id)
            }

            onVideoFullscreenListener = OnVideoFullscreenListener { contentVideo ->
                // Ignore post when scrolling if it has been fullscreened
                postsScrollListener?.setPostToIgnore(contentVideo.redditPost?.id)
            }

            onPostClicked = PostsAdapter.OnPostClicked { post ->
                // Ignore the post when scrolling, so that when we return and scroll a bit it doesn't
                // autoplay the video
                val redditPost = post.redditPost
                postsScrollListener?.setPostToIgnore(redditPost?.id)

                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(PostActivity.POST_KEY, Gson().toJson(redditPost))
                    putExtra(Content.EXTRAS, post.extras)
                    putExtra(PostActivity.HIDE_SCORE_KEY, post.hideScore)
                }

                // Only really applicable for videos, as they should be paused
                post.viewUnselected()

                val activity = requireActivity()
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *post.transitionViews.toTypedArray())
                activity.startActivity(intent, options.toBundle())
            }
        }

        postsLayoutManager = LinearLayoutManager(context).apply { binding.posts.layoutManager = this }

        postsScrollListener = PostScrollListener(binding.posts) { postsViewModel?.loadPosts() }
        binding.posts.setOnScrollChangeListener(postsScrollListener)
    }

    private fun setupRulesList() {
        rulesAdapter = SubredditRulesAdapter().apply { binding.subredditInfo.rules.adapter = this }
        rulesLayoutManager = LinearLayoutManager(context).apply { binding.subredditInfo.rules.layoutManager = this }
    }

    /**
     * Sets [subreddit] based on the subreddit name found in [getArguments]. If the subreddit
     * is not a standard subreddit [retrieveSubredditInfo] is called automatically.
     *
     * If no subreddit is found and empty string (ie. Front page) is used as a default.
     *
     * [binding] will be called for various bindings throughout this call
     */
    private fun setupSubredditObservable() {
        val args = arguments
        val subredditName = if (args != null) {
            args.getString(SUBREDDIT_NAME_KEY, "")
        } else {
            ""
        }
        isDefaultSubreddit = RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())

        val sub = Subreddit()
        sub.name = subredditName
        subreddit.set(sub)

        binding.standardSub = isDefaultSubreddit

        // Not a standard sub, get info from local database if previously stored
        if (!isDefaultSubreddit) {
            database.subreddits().get(subredditName).observe(viewLifecycleOwner) {
                // If the subreddit hasn't been previously loaded it will be null
                if (it != null) {
                    subreddit.set(it)
                }
            }

            retrieveSubredditInfo(subredditName)
        }
    }

    /**
     * Sets up [FragmentSubredditBinding.submitPostFab]. If the current fragment isn't a standard sub,
     * then a scroll listener is added to [FragmentSubredditBinding.posts] to automatically show/hide the FAB
     * when scrolling, and an onClickListener is set to the fab to open a [SubmitActivity]
     */
    private fun setupSubmitPostFab() {
        if (!RedditApi.STANDARD_SUBS.contains(getSubredditName()?.toLowerCase())) {
            binding.let {
                it.posts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0) {
                            it.submitPostFab.hide()
                        } else {
                            it.submitPostFab.show()
                        }
                    }
                })

                it.submitPostFab.setOnClickListener {
                    val intent = Intent(context, SubmitActivity::class.java)
                    intent.putExtra(SubmitActivity.SUBREDDIT_KEY, getSubredditName())
                    startActivity(intent)
                }
            }
        }
    }

    /**
     * Sets up [postsViewModel]
     *
     * This requires [subreddit] to be set with a name
     */
    private fun setupPostsViewModel() {
        postsViewModel = ViewModelProvider(this, PostsFactory(
                getSubredditName(),
                false
        )).get(PostsViewModel::class.java).apply {
            getPosts().observe(viewLifecycleOwner, { posts ->
                Log.d(TAG, "setupPostsViewModel: observing posts")
                // Store the updated post IDs right away
                this@SubredditFragment.postIds = postIds as ArrayList<String>

                // Posts have been cleared, clear the adapter and clear the layout manager state
                if (posts.isEmpty()) {
                    postsAdapter?.clearPosts()
                    postsLayoutManager = LinearLayoutManager(context)
                    return@observe
                }

                val previousSize = postsAdapter?.itemCount
                postsAdapter?.submitList(filterPosts(posts))

                if (saveState != null && previousSize == 0) {
                    val layoutState: Parcelable? = saveState?.getParcelable(saveKey(LAYOUT_STATE_KEY))

                    if (layoutState != null) {
                        postsLayoutManager?.onRestoreInstanceState(layoutState)

                        // If we're at this point we probably don't want the toolbar expanded
                        // We get here when the fragment/activity holding the fragment has been restarted
                        // so it usually looks odd if the toolbar now shows
                        binding.subredditAppBarLayout.setExpanded(false, false)
                    }
                }
            })

            onLoadingCountChange().observe(viewLifecycleOwner, { up -> _binding?.loadingIcon?.onCountChange(up) })
            getError().observe(viewLifecycleOwner, { error ->
                run {
                    // Error loading posts, reset onEndOfList so it tries again when scrolled
                    postsScrollListener?.resetOnEndOfList()
                    handleErrors(error.error, error.throwable)
                }
            })
        }
    }


    /**
     * Refreshes the posts in the fragment
     *
     * To ensure that no list state is saved and restored, [saveState] is cleared
     */
    private fun refreshPosts() {
        // If the user had previously gone out of the fragment and gone back, refreshing would
        // restore the list state that was saved at that point, making the list scroll to that point
        saveState?.clear()
        postsViewModel?.restart()
    }

    /**
     * Click listener for the "+ Subscribe/- Unsubscribe" button.
     *
     * Sends an API request to Reddit to subscribe/unsubscribe
     */
    private fun subscribeOnclick() {
        val sub = subreddit.get()

        sub?.let {
            // Assume success
            val newSubscription = !it.isSubscribed

            CoroutineScope(IO).launch {
                sub.isSubscribed = newSubscription
                sub.subscribers += if (newSubscription) 1 else -1

                database.subreddits().update(sub)

                val response = api.subreddit(sub.name).subscribe(newSubscription)

                withContext(Main) {
                    when (response) {
                        is ApiResponse.Success -> { }
                        is ApiResponse.Error -> {
                            // Revert back
                            sub.isSubscribed = !newSubscription
                            sub.subscribers += if (!newSubscription) 1 else -1
                            database.subreddits().update(sub)

                            Util.handleGenericResponseErrors(
                                    binding.parentLayout,
                                    response.error,
                                    response.throwable
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves information for a subreddit from the Reddit API
     *
     * [subreddit] is updated with the information retrieved from the API and is inserted into
     * the local DB (if it is not a NSFW sub)
     *
     * @param subredditName The name of the subreddit to get information for
     */
    private fun retrieveSubredditInfo(subredditName: String) {
        CoroutineScope(IO).launch {
            when (val response = api.subreddit(subredditName).info()) {
                is ApiResponse.Success -> {
                    database.subreddits().insert(response.value)
                }
                is ApiResponse.Error -> {
                    withContext(Main) {
                        handleErrors(response.error, response.throwable)
                    }
                }
            }
        }
    }

    /**
     * Retrieves rules for a subreddit
     *
     * @param subredditName The name of the subreddit to get rules for
     */
    private fun retrieveSubredditRules(subredditName: String) {
        binding.subredditInfo.rulesloadingIcon.onCountChange(true)
        CoroutineScope(IO).launch {
            when (val response = api.subreddit(subredditName).rules()) {
                is ApiResponse.Success -> {
                    rulesLoaded = true

                    database.rules().insertAll(response.value)

                    withContext(Main) {
                        binding.subredditInfo.rulesloadingIcon.onCountChange(false)
                    }
                }
                is ApiResponse.Error -> {
                    withContext(Main) {
                        binding.subredditInfo.rulesloadingIcon.onCountChange(false)
                        handleErrors(response.error, response.throwable)
                    }
                }
            }
        }
    }

    /**
     * Observes the subreddits rules from the local database and updates [rulesAdapter] on changes
     */
    private fun observeRules() {
        database.rules().getAllRules(getSubredditName()).observe(viewLifecycleOwner) {
            rulesAdapter?.submitList(it)
        }
    }

    /**
     * Observes the subreddits user flairs from the local database and updates the spinner in the
     * subreddit info
     */
    private fun observeUserFlairs() {
        database.flairs().getFlairsBySubredditAndType(getSubredditName(), FlairType.USER.name).observe(viewLifecycleOwner) {
            if (it == null) {
                return@observe
            }

            val adapter = RedditFlairAdapter(requireContext(), android.R.layout.simple_spinner_item, it as ArrayList<RedditFlair>).apply {
                onFlairClicked = RedditFlairAdapter.OnFlairClicked { flair ->
                    updateUserFlair(flair)
                }
            }

            binding.subredditInfo.selectFlairSpinner.adapter = adapter
        }
    }

    /**
     * Calls the API to get the submission flairs for this subreddit
     */
    private fun getSubmissionFlairs(subredditName: String) {
        binding.subredditInfo.selectFlairLoadingIcon.visibility = View.VISIBLE
        CoroutineScope(IO).launch {
            val response = api.subreddit(subredditName).userFlairs()
            withContext(Main) {
                when (response) {
                    is ApiResponse.Success -> {
                        flairsLoaded = true
                        onUserFlairResponse(response.value)
                    }
                    is ApiResponse.Error -> {
                        // If the sub doesn't allow flairs, a 403 is returned
                        binding.subredditInfo.selectFlairLoadingIcon.visibility = View.GONE
                        binding.subredditInfo.selectFlairSpinner.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Handles successful responses for submission flairs. The loading icon is removed and
     * [ActivitySubmitBinding.flairSpinner] is updated with the flairs.
     *
     * If no flairs are returned, then the spinner view is removed
     *
     * @param flairs The flairs retrieved
     */
    private fun onUserFlairResponse(flairs: List<RedditFlair>) {
        flairs as ArrayList<RedditFlair>

        if (flairs.isEmpty()) {
            binding.subredditInfo.selectFlairSpinner.visibility = View.GONE
            return
        }

        binding.subredditInfo.selectFlairLoadingIcon.visibility = View.GONE

        CoroutineScope(IO).launch {
            database.flairs().insert(flairs)
        }
    }

    /**
     * Updates the users flair on the subreddit
     *
     * If the subreddit name ([getSubredditName]) or the username ([App.storedUser]) is `null` then
     * this will return
     *
     * @param flair The flair to update, or `null` to disable the flair on the subreddit
     */
    private fun updateUserFlair(flair: RedditFlair?) {
        val subredditName = getSubredditName() ?: return
        val username = App.storedUser?.username ?: return

        if (flair != null) {
            CoroutineScope(IO).launch {
                when (val resp = api.subreddit(subredditName).selectFlair(username, flair.id)) {
                    is ApiResponse.Success -> {
                        _binding?.let {
                            withContext(Main) {
                                ViewUtil.setFlair(
                                        it.subredditInfo.userFlair,
                                        flair.richtextFlairs,
                                        flair.text,
                                        flair.textColor,
                                        flair.backgroundColor,
                                )
                            }

                            Snackbar.make(it.root, R.string.flairUpdated, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    is ApiResponse.Error -> {
                        if (_binding != null) {
                            handleErrors(resp.error, resp.throwable)
                        }
                    }
                }
            }
        } else {
            // TODO "dont show flair on subreddit"
        }
    }

    /**
     * Filters a list of posts based on [App.subredditsToFilterFromDefaultSubreddits]
     *
     * If [isDefaultSubreddit] is false, then the original list is returned
     *
     * @param posts The posts to filter
     * @return The filtered posts, or [posts] if this is not a default subreddit
     */
    private fun filterPosts(posts: List<RedditPost>) : List<RedditPost> {
        return if (isDefaultSubreddit) {
            val subsToFilter = App.get().subredditsToFilterFromDefaultSubreddits()
            posts.filter {
                // Keep the post if the subreddit it is in isn't found in subsToFilter
                !subsToFilter.contains(it.subreddit.toLowerCase())
            }
        } else {
            posts
        }
    }

    /**
     * Handles the errors received by the API
     *
     * @param error The error received
     * @param throwable The throwable received
     */
    private fun handleErrors(error: GenericError, throwable: Throwable) {
        val errorReason = error.reason
        throwable.printStackTrace()

        // Duplication of code here but idk how to generify the bindings?
        // These should also be in the center of the bottom parent/appbar or have margin to the bottom of the appbar
        // since now it might go over the appbar
        when (errorReason) {
            GenericError.SUBREDDIT_BANNED -> {
                SubredditBannedBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
                    subreddit = getSubredditName()
                    (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    root.requestLayout()
                }
            }

            GenericError.SUBREDDIT_PRIVATE -> {
                SubredditPrivateBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
                    subreddit = getSubredditName()
                    (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    root.requestLayout()
                }
            }

            // For instance accessing r/lounge without Reddit premium
            GenericError.REQUIRES_REDDIT_PREMIUM -> {
                SubredditRequiresPremiumBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
                    subreddit = getSubredditName()
                    (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    root.requestLayout()
                }
            }

            else -> {
                // NoSubredditInfoException is retrieved when trying to get info from front page, popular, or all
                // and we don't need to show anything of this to the user
                if (throwable is NoSubredditInfoException) {
                    return
                } else if (throwable is SubredditNotFoundException) {
                    val layout = SubredditNotFoundBinding.inflate(layoutInflater, binding.parentLayout, true)
                    layout.subreddit = getSubredditName()

                    (layout.root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    layout.root.requestLayout()
                    return
                }
                Util.handleGenericResponseErrors(binding.parentLayout, error, throwable)
            }
        }
    }

    /**
     * Closes the drawer if it is open
     *
     * @return True if the drawer was opened and is now closed, false if the drawer wasn't opened
     */
    fun closeDrawerIfOpen() : Boolean {
        return if (_binding?.drawer?.isDrawerOpen(GravityCompat.END) == true) {
            _binding?.drawer?.closeDrawer(GravityCompat.END)
            true
        } else {
            false
        }
    }

    /**
     * Automatically opens the drawer if [SHOW_RULES] in [getArguments] is set to `true`
     */
    private fun automaticallyOpenDrawerIfSet() {
        arguments?.getBoolean(SHOW_RULES)?.let {
            if (it) {
                binding.drawer.openDrawer(GravityCompat.END)
            }
        }
    }

    override fun newSort() {
        postsViewModel?.restart(SortingMethods.NEW)
    }

    override fun hot() {
        postsViewModel?.restart(SortingMethods.HOT)
    }

    override fun top(timeSort: PostTimeSort) {
        postsViewModel?.restart(SortingMethods.TOP, timeSort)
    }

    override fun controversial(timeSort: PostTimeSort) {
        postsViewModel?.restart(SortingMethods.CONTROVERSIAL, timeSort)
    }

    override fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
        binding.subredditIcon.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
    }
}