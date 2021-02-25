package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.MainActivity
import com.example.hakonsreader.activities.SubmitActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.exceptions.NoSubredditInfoException
import com.example.hakonsreader.api.exceptions.SubredditNotFoundException
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.databinding.*
import com.example.hakonsreader.dialogadapters.RedditFlairAdapter
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.SubredditRulesAdapter
import com.example.hakonsreader.viewmodels.SubredditFlairsViewModel
import com.example.hakonsreader.viewmodels.SubredditRulesViewModel
import com.example.hakonsreader.viewmodels.SubredditViewModel
import com.example.hakonsreader.viewmodels.factories.SubredditFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFlairsFactory
import com.example.hakonsreader.viewmodels.factories.SubredditRulesFactory
import com.example.hakonsreader.views.util.ViewUtil
import com.example.hakonsreader.views.util.showPopupSortWithTime
import com.robinhood.ticker.TickerUtils
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


class SubredditFragment : Fragment(), PrivateBrowsingObservable {

    companion object {
        private const val TAG = "SubredditFragment"

        /**
         * The key stored in [getArguments] saying the name the subreddit is for
         */
        private const val SUBREDDIT_NAME_KEY = "subredditName"

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

        private const val POSTS_TAG = "posts_subreddit"

        /**
         * The key used to [getArguments] if the subreddit rules should automatically be shown
         * when entering the subreddit (does not apply for standard subs)
         *
         * The value with this key should be a [Boolean]
         */
        private const val SHOW_RULES = "show_rules"

        private const val SAVED_POSTS_FRAGMENT = "savedPostsFragment"

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
    private var isDefaultSubreddit = false

    val subredditName by lazy {
        arguments?.getString(SUBREDDIT_NAME_KEY) ?: ""
    }
    private var subreddit: Subreddit? = null
    private var subredditViewModel: SubredditViewModel? = null

    private var rulesViewModel: SubredditRulesViewModel? = null
    private var rulesAdapter: SubredditRulesAdapter? = null
    private var rulesLayoutManager: LinearLayoutManager? = null

    private var flairsViewModel: SubredditFlairsViewModel? = null
    private var flairsAdapter: RedditFlairAdapter? = null

    private var postsFragment: PostsFragment? = null

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

        if (savedInstanceState != null) {
            postsFragment = childFragmentManager.getFragment(savedInstanceState, SAVED_POSTS_FRAGMENT) as PostsFragment?
        }

        setupBinding()
        setupSubredditViewModel()

        // Default subs don't have rules/flairs/info in drawers (could potentially add a tiny description
        // of the different default subs in the info)
        if (!isDefaultSubreddit) {
            setupRulesList()
            automaticallyOpenDrawerIfSet()
        } else {
            // Is default subreddit, we can create the fragment now since it will never change
            // (if it is "random" it could change later, so initialize it later)
            createAndAddPostsFragment(subredditName)
        }

        App.get().registerPrivateBrowsingObservable(this)

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        postsFragment?.let { childFragmentManager.putFragment(outState, SAVED_POSTS_FRAGMENT, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        App.get().unregisterPrivateBrowsingObservable(this)

        _binding = null
    }

    /**
     * Gets the [PostsFragment] holding the posts for this subreddit
     *
     * @return The fragment holding the posts, or null if no fragment was found or if the [SubredditFragment]
     * isn't attached
     */
    private fun getPostsFragment() : PostsFragment? {
        // If the outer (this) fragment hasn't been added yet then the childFragmentManager will not be able to do anything
        if (!isAdded) {
            return null
        }

        val fragment = childFragmentManager.findFragmentByTag(POSTS_TAG)
        return if (fragment is PostsFragment) {
            fragment
        } else null
    }

    /**
     * Creates a [PostsFragment] and adds it to [getChildFragmentManager]
     *
     * @param name The name of the subreddit the posts are for
     */
    private fun createAndAddPostsFragment(name: String) {
        // TODO this will mess up configuration changes probably (if refreshed it wont use the current)
        //  and probably if the sort is changed and then scrolled further it would revert to the original when
        //  getting new posts
        val sort = arguments?.getString(SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
        val timeSort = arguments?.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }

        if (postsFragment == null) {
            postsFragment = PostsFragment.newInstance(
                    isForUser = false,
                    name = name,
                    sort = sort,
                    timeSort = timeSort
            ).apply {
                onError = { error, throwable ->
                    handleErrors(error, throwable)
                }
                onLoadingChange = {
                    _binding?.loadingIcon?.onCountChange(it)
                }
            }
        }

        childFragmentManager.beginTransaction()
                .replace(R.id.postsContainer, postsFragment!!, POSTS_TAG)
                .commit()

        setupSubmitPostFab(postsFragment!!)
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

            subredditRefresh.setOnClickListener { getPostsFragment()?.refreshPosts() }
            subredditSort.setOnClickListener { view ->
                getPostsFragment()?.let {
                    showPopupSortWithTime(it, view)
                }
            }

            subscribe.setOnClickListener { subscribeOnclick() }
            subredditInfo.subscribe.setOnClickListener { subscribeOnclick() }

            subredditSubscribers.setCharacterLists(TickerUtils.provideNumberList())

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

            // This is just to show a title until the info has been properly loaded, it might change
            // for instance if the capitalization is different, or for r/random
            this.subreddit = Subreddit().apply { name = subredditName }
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

                // TODO check if it is NSFW, if it is show a warning (this should also be a setting
                //  ie. "Warn about NSFW subreddits"
                createAndAddPostsFragment(it.name)
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
                //postsAdapter?.hideScoreTime = it.hideScoreTime
            }

            loading.observe(viewLifecycleOwner) {
                _binding?.loadingIcon?.onCountChange(it)
            }

            errors.observe(viewLifecycleOwner) {
                handleErrors(it.error, it.throwable)
            }
        }
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
    private fun setupSubmitPostFab(postsFragment: PostsFragment) {
        if (!isDefaultSubreddit) {
            binding.let {
                postsFragment.addScrollListener(object : RecyclerView.OnScrollListener() {
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
                if (App.get().loadSubredditBanners()) {
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
                }
            } else {
                imageView.visibility = View.GONE
            }
        }
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

    override fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
        binding.subredditIcon.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
    }
}