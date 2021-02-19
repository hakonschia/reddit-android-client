package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.*
import android.widget.Adapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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
import com.example.hakonsreader.viewmodels.SubredditFlairsViewModel
import com.example.hakonsreader.viewmodels.SubredditRulesViewModel
import com.example.hakonsreader.viewmodels.SubredditViewModel
import com.example.hakonsreader.viewmodels.factories.PostsFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFlairsFactory
import com.example.hakonsreader.viewmodels.factories.SubredditRulesFactory
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.util.ViewUtil
import com.google.gson.Gson
import com.robinhood.ticker.TickerUtils
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


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

    val subredditName by lazy {
        arguments?.getString(SUBREDDIT_NAME_KEY) ?: ""
    }
    private var subreddit: Subreddit? = null
    private var subredditViewModel: SubredditViewModel? = null

    private var postsViewModel: PostsViewModel? = null
    private var postsAdapter: PostsAdapter? = null
    private var postsLayoutManager: LinearLayoutManager? = null
    private var postsScrollListener: PostScrollListener? = null

    private var rulesViewModel: SubredditRulesViewModel? = null
    private var rulesAdapter: SubredditRulesAdapter? = null
    private var rulesLayoutManager: LinearLayoutManager? = null

    private var flairsViewModel: SubredditFlairsViewModel? = null
    private var flairsAdapter: RedditFlairAdapter? = null

    /**
     * A DrawerListener for the drawer with subreddit info
     */
    var drawerListener: DrawerLayout.DrawerListener? = null

    /**
     * If set to true, the fragments activity will call [AppCompatActivity.setSupportActionBar]
     * when the view is created
     */
    var setToolbarOnActivity = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isDefaultSubreddit = RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())

        setupBinding()
        setupPostsList()
        setupSubredditViewModel()
        setupSubmitPostFab()

        // Default subs don't have rules/flairs/info in drawers (could potentially add a tiny description
        // of the different default subs in the info)
        if (!isDefaultSubreddit) {
            setupRulesList()

            automaticallyOpenDrawerIfSet()
        } else {
            setupPostsViewModel(subredditName)
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
     * Converts a base key into a unique key for this subreddit, so that the subreddit state can be
     * stored in a global bundle holding states for multiple subreddits
     *
     * @param baseKey The base key to use
     * @return A key unique to this subreddit
     */
    private fun saveKey(baseKey: String) : String {
        return baseKey + "_" + subredditName
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

        with(binding) {
            if (isDefaultSubreddit) {
                setDefaultSubDescription()
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
            }

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

            drawerListener?.let { drawer.addDrawerListener(it) }
            drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerOpened(drawerView: View) {
                    this@SubredditFragment.subreddit?.let {
                        val rulesCount = rulesAdapter?.itemCount ?: 0
                        // No rules, or we have rules and data saving is not on
                        // Ie. only load rules again from API if we're not on data saving
                        if (rulesCount == 0 || (rulesCount != 0 && !App.get().dataSavingEnabled())) {
                            CoroutineScope(IO).launch {
                                rulesViewModel?.refresh()
                            }
                        }

                        // The adapter will always have one more (for the "Select flair")
                        val flairsCount = flairsAdapter?.count?.minus(1) ?: 0
                        if ((it.canAssignUserFlair || it.isModerator) && (flairsCount == 0 || (flairsCount != 0 && !App.get().dataSavingEnabled()))) {
                            CoroutineScope(IO).launch {
                                flairsViewModel?.refresh()
                            }
                        }
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

            (requireActivity() as AppCompatActivity).setSupportActionBar(subredditToolbar)
        }
    }

    /**
     * Sets up [subredditViewModel], assuming [isDefaultSubreddit] is false. If [isDefaultSubreddit]
     * is true, then a base subreddit is set on [binding] with [subredditName]
     */
    private fun setupSubredditViewModel() {
        binding.standardSub = isDefaultSubreddit
        if (isDefaultSubreddit) {
            binding.subreddit = Subreddit().apply {
                name = subredditName
            }

            return
        }

        subredditViewModel = ViewModelProvider(this, SubredditFactory(
                subredditName,
                api.subreddit(subredditName),
                database.subreddits(),
                database.posts()
        )).get(SubredditViewModel::class.java).apply {
            subreddit.observe(viewLifecycleOwner) {
                val old = this@SubredditFragment.subreddit
                this@SubredditFragment.subreddit = it

                // If this is null then it should probably be reflected on the subreddit field in the fragment?
                // Probably won't ever happen though
                if (it == null) {
                    return@observe
                }

                setupPostsViewModel(it.name)
                setupRulesViewModel(it.name)
                setupFlairsViewModel(it.name)

                // Kind of weird to call this I guess, but it's to now load load posts
                onResume()

                // If there is no subscribers previously the ticker animation looks very weird
                // so disable it if it would like weird
                val enableTickerAnimation = old != null && old.subscribers != 0
                binding.subredditSubscribers.animationDuration = (if (enableTickerAnimation) {
                    resources.getInteger(R.integer.tickerAnimationDefault)
                } else {
                    0
                }).toLong()

                ViewUtil.setSubredditIcon(binding.subredditIcon, it)
                setBannerImage()

                binding.subreddit = it
                postsAdapter?.hideScoreTime = it.hideScoreTime
            }
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
     * Sets up [rulesViewModel]
     *
     * @param name The name of the subreddit the ViewModel is for
     */
    private fun setupRulesViewModel(name: String) {
        rulesViewModel = ViewModelProvider(this, SubredditRulesFactory(
                name,
                api.subreddit(name),
                database.rules()
        )).get(SubredditRulesViewModel::class.java).apply {
            rules.observe(viewLifecycleOwner) {
                rulesAdapter?.submitList(it)
            }
            errors.observe(viewLifecycleOwner) {
                handleErrors(it.error, it.throwable)
            }
            // There won't be anything else causing this to loader to load so this is safe
            loading.observe(viewLifecycleOwner) {
                binding.subredditInfo.rulesloadingIcon.visibility = if (it) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
        }
    }

    /**
     * Sets up [flairsViewModel]
     *
     * @param name The name of the subreddit the ViewModel is for
     */
    private fun setupFlairsViewModel(name: String) {
        flairsViewModel = ViewModelProvider(this, SubredditFlairsFactory(
                name,
                FlairType.USER,
                api.subreddit(name),
                database.flairs()
        )).get(SubredditFlairsViewModel::class.java).apply {
            flairs.observe(viewLifecycleOwner) {
                flairsAdapter = RedditFlairAdapter(requireContext(), android.R.layout.simple_spinner_item, it as ArrayList<RedditFlair>).apply {
                    // If/when the flairs are updated the previous listener would trigger, which could
                    // remove the users flair
                    binding.subredditInfo.selectFlairSpinner.onItemSelectedListener = null

                    binding.subredditInfo.selectFlairSpinner.adapter = this
                    // Select the first item right away, as this would happen anyways, and would trigger the
                    // item selected listener, which would call updateUserFlair with null, disabling the users flair
                    binding.subredditInfo.selectFlairSpinner.setSelection(0, false)

                    binding.subredditInfo.selectFlairSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            updateUserFlair(flairsAdapter?.getFlairAt(position))
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Not implemented
                        }
                    }
                }
            }

            errors.observe(viewLifecycleOwner) {
                handleErrors(it.error, it.throwable)
            }
            // There won't be anything else causing this to loader to load so this is safe
            loading.observe(viewLifecycleOwner) {
                binding.subredditInfo.selectFlairLoadingIcon.visibility = if (it) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
        }
    }

    /**
     * Sets up [FragmentSubredditBinding.submitPostFab]. If the current fragment isn't a standard sub,
     * then a scroll listener is added to [FragmentSubredditBinding.posts] to automatically show/hide the FAB
     * when scrolling, and an onClickListener is set to the fab to open a [SubmitActivity]
     */
    private fun setupSubmitPostFab() {
        if (!RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase())) {
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
                    Intent(context, SubmitActivity::class.java).apply {
                        putExtra(SubmitActivity.SUBREDDIT_KEY, subredditName)
                        startActivity(this)
                    }
                }
            }
        }
    }

    /**
     * Sets up [postsViewModel]
     *
     * @param name The name of the subreddit the ViewModel is for
     */
    private fun setupPostsViewModel(name: String) {
        postsViewModel = ViewModelProvider(this, PostsFactory(
                name,
                false
        )).get(PostsViewModel::class.java).apply {
            getPosts().observe(viewLifecycleOwner, { posts ->
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
     * Sets the subreddits banner image. If no image is found, the banners visibility is set to [View.GONE].
     * When (if) the image loads then [FragmentSubredditBinding.setBannerLoaded] is called with `true`.
     * If data saving is enabled, then the image will only be loaded if it is already cached, and will
     * not load from the network
     */
    private fun setBannerImage() {
        subreddit?.let {
            val imageView = binding.banner
            val bannerURL = it.bannerBackgroundImage
            if (bannerURL.isNotEmpty()) {
                // Data saving on, only load if the image is already cached
                if (App.get().dataSavingEnabled()) {
                    Picasso.get()
                            .load(bannerURL)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(imageView, object : Callback {
                                override fun onSuccess() {
                                    imageView.visibility = View.VISIBLE
                                    binding.bannerLoaded = true
                                }

                                override fun onError(e: Exception) {
                                    imageView.visibility = View.GONE
                                    binding.bannerLoaded = false
                                }
                            })
                } else {
                    imageView.visibility = View.VISIBLE
                    Picasso.get()
                            .load(bannerURL)
                            .into(imageView, object : Callback {
                                override fun onSuccess() {
                                    binding.bannerLoaded = true
                                }

                                override fun onError(e: Exception) {
                                    binding.bannerLoaded = false
                                }
                            })
                }
            } else {
                imageView.visibility = View.GONE
            }
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
        CoroutineScope(IO).launch {
            subredditViewModel?.subscribe()
        }
    }

    /**
     * Updates the users flair on the subreddit
     *
     * If the username ([App.currentUserInfo]) is `null` then this will return
     *
     * @param flair The flair to update, or `null` to disable the flair on the subreddit
     */
    private fun updateUserFlair(flair: RedditFlair?) {
        val username = App.get().currentUserInfo?.userInfo?.username ?: return

        CoroutineScope(IO).launch {
            subredditViewModel?.updateFlair(username, flair)
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
                    subreddit = subredditName
                    (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    root.requestLayout()
                }
            }

            GenericError.SUBREDDIT_PRIVATE -> {
                SubredditPrivateBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
                    subreddit = subredditName
                    (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
                    root.requestLayout()
                }
            }

            // For instance accessing r/lounge without Reddit premium
            GenericError.REQUIRES_REDDIT_PREMIUM -> {
                SubredditRequiresPremiumBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
                    subreddit = subredditName
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
                    layout.subreddit = subredditName

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

    /**
     * Sets the subreddit description for default subs
     */
    private fun setDefaultSubDescription() {
        val stringRes = when (subredditName.toLowerCase()) {
            "" -> R.string.frontPageDescription
            "popular" -> R.string.popularDescription
            "all" -> R.string.allDescription
            else -> null
        } ?: return

        _binding?.standardSubDescription?.text = getString(stringRes)
    }

    /**
     * Retrieves this subreddits toolbar, if the view has been initialized
     *
     * @return The subreddits toolbar, or null if the view hasn't been created
     */
    fun getToolbar() : Toolbar? {
        return _binding?.subredditToolbar
    }

    override fun new() {
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

    override fun currentSort(): SortingMethods {
        return postsViewModel?.sort ?: SortingMethods.HOT
    }

    override fun currentTimeSort(): PostTimeSort? {
        return when (postsViewModel?.sort) {
            SortingMethods.TOP, SortingMethods.CONTROVERSIAL -> postsViewModel?.timeSort
            else -> null
        }
    }

    override fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
        binding.subredditIcon.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
    }
}