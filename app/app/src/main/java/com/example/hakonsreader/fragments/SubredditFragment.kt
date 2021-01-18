package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.PostActivity
import com.example.hakonsreader.activites.SubmitActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.databinding.*
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener
import com.example.hakonsreader.viewmodels.PostsViewModel
import com.example.hakonsreader.viewmodels.factories.PostsFactory
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentVideo
import com.example.hakonsreader.views.util.ViewUtil
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
         * The key used to send to this activity how to sort the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [SortingMethods]
         */
        const val SORT = "sort"

        /**
         * The key used to send to this activity the time sort for the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [PostTimeSort]
         */
        const val TIME_SORT = "time_sort"

        /**
         * Creates a new instance of the fragment
         *
         * @param subredditName The name of the subreddit to instantiate
         * @return The newly created fragment
         */
        fun newInstance(subredditName: String, sort: SortingMethods? = null, timeSort: PostTimeSort? = null) = SubredditFragment().apply {
            arguments = Bundle().apply {
                putString(SUBREDDIT_NAME_KEY, subredditName)
                sort?.let { putString(SORT, it.value) }
                timeSort?.let { putString(TIME_SORT, it.value) }
            }
        }
    }

    private val database = RedditDatabase.getInstance(context)
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
            postsAdapter?.setHideScoreTime(value.hideScoreTime)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()
        setupPostsList()
        setupSubredditObservable()
        setupSubmitPostFab()
        setupPostsViewModel()

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
                    val extras = viewHolder.extras
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
                        viewHolder.extras = extras
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

        binding.subredditRefresh.setOnClickListener { refreshPosts() }
        binding.subscribe.setOnClickListener { subscribeOnclick() }

        binding.subredditSubscribers.setCharacterLists(TickerUtils.provideNumberList())

        binding.postsRefreshLayout.setOnRefreshListener {
            refreshPosts()

            // The refreshing will be visible with our own progress bar
            binding.postsRefreshLayout.isRefreshing = false
        }
        binding.postsRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(requireContext(), R.color.colorAccent)
        )
    }

    /**
     * Sets up [FragmentSubredditBinding.posts] and [postsAdapter]/[postsLayoutManager]
     */
    private fun setupPostsList() {
        postsAdapter = PostsAdapter().apply { binding.posts.adapter = this }
        postsLayoutManager = LinearLayoutManager(context).apply { binding.posts.layoutManager = this }

        postsScrollListener = PostScrollListener(binding.posts) { postsViewModel?.loadPosts() }
        binding.posts.setOnScrollChangeListener(postsScrollListener)

        postsAdapter?.setOnVideoManuallyPaused { contentVideo ->
            // Ignore post when scrolling if manually paused
            postsScrollListener?.setPostToIgnore(contentVideo.redditPost.id)
        }
        postsAdapter?.setVideoFullscreenListener { contentVideo ->
            // Ignore post when scrolling if it has been fullscreened
            postsScrollListener?.setPostToIgnore(contentVideo.redditPost.id)
        }

        postsAdapter?.setOnPostClicked { post ->
            // Ignore the post when scrolling, so that when we return and scroll a bit it doesn't
            // autoplay the video
            val redditPost = post.redditPost
            postsScrollListener?.setPostToIgnore(redditPost.id)

            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra(PostActivity.POST_KEY, Gson().toJson(redditPost))
            intent.putExtra(Content.EXTRAS, post.extras)
            intent.putExtra(PostActivity.HIDE_SCORE_KEY, post.hideScore)

            // Only really applicable for videos, as they should be paused
            post.viewUnselected()

            val activity = requireActivity()
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *post.transitionViews.toTypedArray())
            activity.startActivity(intent, options.toBundle())
        }
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
            CoroutineScope(IO).launch {
                val s = database.subreddits().get(subredditName)
                if (s != null) {
                    withContext(Main) {
                        subreddit.set(s)
                    }
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
                context,
                getSubredditName(),
                false
        )).get(PostsViewModel::class.java)

        postsViewModel?.getPosts()?.observe(viewLifecycleOwner, { posts ->
            // Store the updated post IDs right away
            postIds = postsViewModel?.postIds as ArrayList<String>

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

        postsViewModel?.onLoadingCountChange()?.observe(viewLifecycleOwner, { up -> binding?.loadingIcon?.onCountChange(up) })
        postsViewModel?.getError()?.observe(viewLifecycleOwner, { error ->
            run {
                // Error loading posts, reset onEndOfList so it tries again when scrolled
                postsScrollListener?.resetOnEndOfList()
                handleErrors(error.error, error.throwable)
            }
        })
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

        // TODO should this also assume that it succeeds like with voting?
        sub?.let {
            val newSubscription = !it.isSubscribed

            CoroutineScope(IO).launch {
                val response = api.subreddit(sub.name).subscribe(newSubscription)

                withContext(Main) {
                    when (response) {
                        is ApiResponse.Success -> {
                            it.isSubscribed = newSubscription
                            subreddit.set(it)
                        }
                        is ApiResponse.Error -> Util.handleGenericResponseErrors(
                                binding.parentLayout,
                                response.error,
                                response.throwable
                        )
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
                    val sub = response.value

                    // Lets assume the user doesn't want to store NSFW. We could use the setting for caching
                    // images/videos but it's somewhat going beyond the intent of the setting
                    if (sub.isNsfw) {
                        database.subreddits().insert(sub)
                    }

                    withContext(Main) {
                        subreddit.set(sub)
                    }
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