package com.example.hakonsreader.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.DispatcherActivity
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
import com.example.hakonsreader.interfaces.LockableSlidr
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.SubredditRulesAdapter
import com.example.hakonsreader.viewmodels.SubredditFlairsViewModel
import com.example.hakonsreader.viewmodels.SubredditRulesViewModel
import com.example.hakonsreader.viewmodels.SubredditViewModel
import com.example.hakonsreader.viewmodels.SubredditWikiViewModel
import com.example.hakonsreader.viewmodels.factories.SubredditFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFlairsFactory
import com.example.hakonsreader.viewmodels.factories.SubredditRulesFactory
import com.example.hakonsreader.viewmodels.factories.SubredditWikiFactory
import com.example.hakonsreader.views.util.ViewUtil
import com.example.hakonsreader.views.util.showPopupSortWithTime
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import kotlin.math.log


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

    val subredditName by lazy { arguments?.getString(SUBREDDIT_NAME_KEY) ?: "" }
    private val isDefaultSubreddit by lazy { RedditApi.STANDARD_SUBS.contains(subredditName.toLowerCase()) }

    private var subreddit: Subreddit? = null
    private var subredditViewModel: SubredditViewModel? = null

    private var rulesViewModel: SubredditRulesViewModel? = null
    private var rulesAdapter: SubredditRulesAdapter? = null
    private var rulesLayoutManager: LinearLayoutManager? = null

    private var flairsViewModel: SubredditFlairsViewModel? = null
    private var flairsAdapter: RedditFlairAdapter? = null

    // Not sure if storing the fragments like might cause a leak? Need to access them somehow though
    private var postsFragment: PostsFragment? = null
    private var wikiFragment: WikiFragment? = null

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
            setupViewPager(subredditName)
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
     * Sets up the ViewPager (containing the posts) with a given subreddit name
     *
     * @param name The name of the subreddit the subreddit is for
     */
    private fun setupViewPager(name: String) {
        // TODO unless you know the wiki it's not obvious since there arent tabs
        //  TabLayout could be here, but need to figure out how to hide that (along with the toolbar)
        //  since it's annoying to have it on screen the entire time
        binding.pager.adapter = Adapter(name, this@SubredditFragment)

        val act = activity
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val onFirstPage = position == 0

                // If the activity is a Slidr activity we have to lock it to avoid swiping away
                // when swiping back
                if (act is LockableSlidr) {
                    act.lock(!onFirstPage)
                }
            }
        })
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
                setupViewPager(it.name)
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

            isLoading.observe(viewLifecycleOwner) {
                checkLoadingStatus()
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

            isLoading.observe(viewLifecycleOwner) {
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
            isLoading.observe(viewLifecycleOwner) {
                binding.subredditInfo.selectFlairLoadingIcon.visibility = if (it) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
        }
    }

    /**
     * Sets up [FragmentSubredditBinding.submitPostFab]. If [isDefaultSubreddit] is false then a
     * scroll listener is added to [postsFragment] to automatically show/hide the FAB
     * when scrolling, and an onClickListener is set to the fab to open a [SubmitActivity]
     *
     * @param postsFragment The fragment to add the scroll listener to
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
     * Checks all the loading values on the fragment and enables to loading icon accordingly
     */
    @Synchronized
    private fun checkLoadingStatus() {
        var count = 0

        count += if (postsFragment?.isLoading() == true) 1 else 0
        count += if (wikiFragment?.isLoading() == true) 1 else 0
        count += if (subredditViewModel?.isLoading?.value == true) 1 else 0

        _binding?.loadingIcon?.visibility = if (count > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun getWikiFragment() : WikiFragment? {
        if (_binding == null) {
            return null
        }

        return if (binding.pager.childCount >= 2) {
            binding.pager[1] as WikiFragment
        } else null
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

    class WikiFragment : Fragment() {
        // TODO back button should probably go back to the previous wiki page. Unless the page has a button
        //  back it's impossible to get back. Can have a stack of the wiki names loaded or something
        //  (probably in the ViewModel with a (goToPrevious()) or something

        companion object {
            private const val WIKI_SUBREDDIT_NAME = "wikiSubredditName"

            fun newInstance(name: String) = WikiFragment().apply {
                arguments = Bundle().apply {
                    putString(WIKI_SUBREDDIT_NAME, name)
                }
            }
        }

        private var _binding: FragmentSubredditWikiBinding? = null
        private val binding get() = _binding!!

        private val name by lazy {
            arguments?.getString(WIKI_SUBREDDIT_NAME)
                    ?: throw RuntimeException("No subreddit name given to wiki")
        }

        private val wikiViewModel: SubredditWikiViewModel by viewModels { SubredditWikiFactory(App.get().api.subreddit(name)) }

        /**
         * A movement method to be used in the wiki that checks if the clicked link is another wiki page, and
         * loads that page internally instead of sending to [DispatcherActivity]
         */
        private val wikiLinkMovementMethod = InternalLinkMovementMethod { linkText ->
            // If linkText matches another wiki page, load that on the ViewModel, otherwise send to Dispatcher
            // Ensure we have a full URL (assume non-http links are to Reddit)
            val url = if (!linkText.matches("^http(s)?.*".toRegex())) {
                "https://reddit.com" + (if (linkText[0] == '/') "" else "/") + linkText
            } else linkText

            // Wiki pages might be in multiple paths (ie. "index/rules/whatever")
            // Must also match "/wiki", which should be treated as "wiki/index"
            if (url.matches("https://(.*)?reddit.com/r/$name/wiki.*".toRegex())) {
                // Get everything after "wiki"
                val wikiPage = url.split("/wiki").last()

                // Don't keep the first "/" if present. Reddit will handle it, but to be safe in the future
                if (wikiPage.isNotEmpty() && wikiPage.first() == '/') {
                    wikiViewModel.loadPage(wikiPage.substring(1))
                } else {
                    wikiViewModel.loadPage(wikiPage)
                }
            } else {
                Intent(requireContext(), DispatcherActivity::class.java).apply {
                    putExtra(DispatcherActivity.URL_KEY, linkText)
                    requireContext().startActivity(this)
                }
            }

            true
        }

        /**
         * Callback for errors when loading posts
         */
        var onError: ((GenericError, Throwable) -> Unit)? = null

        /**
         * Callback for when posts have started/finished loading
         *
         * @see isLoading
         */
        var onLoadingChange: ((Boolean) -> Unit)? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            _binding = FragmentSubredditWikiBinding.inflate(inflater)

            binding.wikiContent.movementMethod = wikiLinkMovementMethod

            setupViewModel()

            if (wikiViewModel.page.value == null) {
                // Load index if no page is already loaded
                wikiViewModel.loadPage()
            }

            return binding.root
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        private fun setupViewModel() {
            with (wikiViewModel) {
                page.observe(viewLifecycleOwner) {
                    // Ensure we're at the top when loading a new page (this might have to be stored in onSaveInstanceState)
                    binding.scroller.scrollTo(0, 0)
                    val content = App.get().adjuster.adjust(it.content)

                    App.get().markwon.setMarkdown(binding.wikiContent, content)
                }

                isLoading.observe(viewLifecycleOwner) {
                    onLoadingChange?.invoke(it)
                }

                error.observe(viewLifecycleOwner) {
                    // TODO Handle wiki specific errors such as not found
                }
            }
        }

        fun isLoading() = wikiViewModel.isLoading.value
    }

    private inner class Adapter(val name: String, fragment: Fragment) : FragmentStateAdapter(fragment) {
        // Kind of bad to hardcode the count I guess but whatever
        override fun getItemCount() = if (isDefaultSubreddit) 1 else 2
        override fun createFragment(position: Int) : Fragment {
            return when (position) {
                0 -> {
                    val sort = arguments?.getString(SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
                    val timeSort = arguments?.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }
                    PostsFragment.newInstance(
                            isForUser = false,
                            name = name,
                            sort = sort,
                            timeSort = timeSort
                    ).apply {
                        onError = { error, throwable ->
                            handleErrors(error, throwable)
                        }
                        onLoadingChange = {
                            checkLoadingStatus()
                        }

                        setupSubmitPostFab(this)

                        postsFragment = this
                    }
                }
                1 -> WikiFragment.newInstance(name).apply {
                    onLoadingChange = {
                        checkLoadingStatus()
                    }

                    wikiFragment = this
                }

                else -> throw IllegalStateException("Unexpected position: $position")
            }
        }
    }
}