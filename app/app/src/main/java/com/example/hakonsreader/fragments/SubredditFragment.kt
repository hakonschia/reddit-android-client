package com.example.hakonsreader.fragments

import android.animation.LayoutTransition
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
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
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.states.LoggedInState
import com.example.hakonsreader.recyclerviewadapters.SubredditRulesAdapter
import com.example.hakonsreader.viewmodels.*
import com.example.hakonsreader.viewmodels.factories.SubredditFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFlairsFactory
import com.example.hakonsreader.viewmodels.factories.SubredditRulesFactory
import com.example.hakonsreader.viewmodels.factories.SubredditWikiFactory
import com.example.hakonsreader.views.util.ViewUtil
import com.example.hakonsreader.views.util.showPopupSortWithTime
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.RuntimeException


/**
 * Fragment for displaying a subreddit. The posts/wiki/rules etc. will not be loaded until a valid
 * response for the information is retrieved, and therefore potential redirects for subreddit names
 * is supported
 */
class SubredditFragment : Fragment() {

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


        /**
         * The key used to [getArguments] if the subreddit rules should automatically be shown
         * when entering the subreddit (does not apply for standard subs)
         *
         * The value with this key should be a [Boolean]
         */
        private const val SHOW_RULES = "show_rules"

        /**
         * Key to save [nsfwWarningShown]
         */
        private const val NSFW_WARNING_SHOWN = "nsfwWarningShown"

        /**
         * Key to save [nsfwWarningDismissedWithSuccess]
         */
        private const val NSFW_WARNING_DISMISSED_WITH_SUCCESS = "nsfwWarningDismissedWithSuccess"

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
    private var flairsViewModel: SubredditFlairsViewModel? = null

    // Not sure if storing the fragments like might cause a leak? Need to access them somehow though
    private var postsFragment: PostsFragment? = null
    private var wikiFragment: WikiFragment? = null

    /**
     * Flag for if the NSFW warning for this sub has already been shown, and shouldn't be shown again
     */
    private var nsfwWarningShown = false

    /**
     * Flag for if the NSFW warning was dismissed with a success (ie. that the user "accepted" to load NSFW)
     */
    private var nsfwWarningDismissedWithSuccess = false


    /**
     * A DrawerListener for the drawer with subreddit info
     */
    var drawerListener: DrawerLayout.DrawerListener? = null

    /**
     * If set to true, the fragment will call [AppCompatActivity.setSupportActionBar] when the view is created
     */
    var setToolbarOnActivity = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) {
            nsfwWarningShown = savedInstanceState.getBoolean(NSFW_WARNING_SHOWN)
            nsfwWarningDismissedWithSuccess = savedInstanceState.getBoolean(NSFW_WARNING_DISMISSED_WITH_SUCCESS)
        }

        setupBinding()
        setupSubredditViewModel()
        addFragmentListener()

        // Default subs don't have rules/flairs/info in drawers (could potentially add a tiny description
        // of the different default subs in the info)
        if (!isDefaultSubreddit) {
            setupRulesList()
            automaticallyOpenDrawerIfSet()
        } else {
            // Is default subreddit, we can create the fragment now since it will never change
            // (if it is "random" it could change later, so initialize it later)
            setupViewPager(Subreddit().apply { name = subredditName })

            // Default subs don't have banners
            bannerLoaded(false)
        }

        App.get().loggedInState.observe(viewLifecycleOwner) {
            when (it) {
                is LoggedInState.LoggedIn -> privateBrowsingStateChanged(false)
                is LoggedInState.PrivatelyBrowsing -> privateBrowsingStateChanged(true)

                // This observer is only really for private browsing changes
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean(NSFW_WARNING_SHOWN, nsfwWarningShown)
            putBoolean(NSFW_WARNING_DISMISSED_WITH_SUCCESS, nsfwWarningDismissedWithSuccess)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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

            subredditRefresh.setOnClickListener { postsFragment?.refreshPosts() }
            subredditSort.setOnClickListener { view ->
                postsFragment?.let {
                    showPopupSortWithTime(it, view)
                }
            }

            subredditInfo.subscribe.setOnClickListener { subscribeOnclick() }

            drawerListener?.let { drawer.addDrawerListener(it) }
            drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerOpened(drawerView: View) {
                    this@SubredditFragment.subreddit?.let {
                        val rulesCount = binding.subredditInfo.rules.adapter?.itemCount ?: 0
                        // No rules, or we have rules and data saving is not on
                        // Ie. only load rules again from API if we're not on data saving
                        if (rulesCount == 0 || (rulesCount != 0 && !App.get().dataSavingEnabled())) {
                            rulesViewModel?.refresh()
                        }

                        val isLoggedIn = App.get().loggedInState.value is LoggedInState.LoggedIn
                        // The adapter will always have one more (for the "Select flair"), so the actual count is one less than the adapter count
                        val flairsCount = binding.subredditInfo.selectFlairSpinner.adapter?.count?.minus(1) ?: 0
                        if (isLoggedIn && (it.canAssignUserFlair || it.isModerator) && (flairsCount == 0 || (flairsCount != 0 && !App.get().dataSavingEnabled()))) {
                            flairsViewModel?.refresh()
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
     * Adds a fragment lifecycle listener to [getChildFragmentManager] that sets [PostsFragment] and
     * [WikiFragment] when their views are created, as well as setting listeners on
     * the fragment
     *
     * When their views are destroyed the references will be nulled.
     */
    private fun addFragmentListener() {
         childFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
             override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                 if (f is PostsFragment) {
                     postsFragment = f.apply {
                         onError = { error, throwable ->
                             handleErrors(error, throwable)
                         }
                         onLoadingChange = {
                             checkLoadingStatus()
                         }

                         hideScoreTime = subreddit?.hideScoreTime ?: 0

                         setupSubmitPostFab(this)

                         postsFragment = this
                     }
                 } else if (f is WikiFragment) {
                     wikiFragment = f.apply {
                         onLoadingChange = {
                             checkLoadingStatus()
                         }

                         onRulesLinkClicked = {
                             binding.drawer.openDrawer(GravityCompat.END)
                         }

                         wikiFragment = this
                     }
                 }
             }

             override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                 if (f is PostsFragment) {
                     postsFragment = null
                 } else if (f is WikiFragment) {
                     wikiFragment = null
                 }
             }
         }, false)
    }

    /**
     * Sets up the ViewPager (containing the posts) with a given subreddit name
     *
     * @param subreddit The the subreddit to display
     */
    private fun setupViewPager(subreddit: Subreddit) {
        // TODO unless you know the wiki it's not obvious since there arent tabs
        //  TabLayout could be here, but need to figure out how to hide that (along with the toolbar)
        //  since it's annoying to have it on screen the entire time
        binding.pager.adapter = Adapter(subreddit, this@SubredditFragment)
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

                // If there is no subscribers previously the ticker animation looks very weird
                // so disable it if it would like weird
                val enableTickerAnimation = old != null && old.subscribers != 0
                binding.subredditSubscribers.animationDuration = (if (enableTickerAnimation) {
                    resources.getInteger(R.integer.tickerAnimationDefault)
                } else {
                    0
                }).toLong()

                binding.subreddit = it

                if (it.isNsfw && !nsfwWarningDismissedWithSuccess && App.get().warnNsfwSubreddits()) {
                    // Only show warning once
                    if (!nsfwWarningShown) {
                        AlertDialog.Builder(requireContext())
                                .setTitle(R.string.subredditNsfwWarningHeader)
                                .setMessage(R.string.subredditNsfwWarningContent)
                                .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, _: Int ->
                                    nsfwWarningDismissedWithSuccess = true
                                    setSubredditAndLoadPosts(it)
                                    dialogInterface.dismiss()
                                }
                                .setNegativeButton(R.string.no) { dialogInterface: DialogInterface, _: Int ->
                                    // Do something else
                                    // We should probably create some sort of listener as this depends on
                                    // where the fragment is (like in SubredditActivity it should probably finish
                                    // and in MainActivity do something else like go back to the subreddits list or something)
                                    nsfwWarningDismissedWithSuccess = false
                                    dialogInterface.dismiss()
                                }
                                .show()
                    }
                    nsfwWarningShown = true
                } else {
                    setSubredditAndLoadPosts(it)
                }
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
        binding.subredditInfo.rules.adapter = SubredditRulesAdapter()
        binding.subredditInfo.rules.layoutManager = LinearLayoutManager(context)
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
                (binding.subredditInfo.rules.adapter as SubredditRulesAdapter?)?.submitList(it)
            }
            errors.observe(viewLifecycleOwner) {
                handleErrors(it.error, it.throwable)
            }

            isLoading.observe(viewLifecycleOwner) {
                binding.subredditInfo.rulesloadingIconLayout.visibility = if (it) {
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
                RedditFlairAdapter(requireContext(), android.R.layout.simple_spinner_item, it as ArrayList<RedditFlair>).run {
                    // If/when the flairs are updated the previous listener would trigger, which could
                    // remove the users flair
                    binding.subredditInfo.selectFlairSpinner.onItemSelectedListener = null

                    binding.subredditInfo.selectFlairSpinner.adapter = this
                    // Select the first item right away, as this would happen anyways, and would trigger the
                    // item selected listener, which would call updateUserFlair with null, disabling the users flair
                    binding.subredditInfo.selectFlairSpinner.setSelection(0, false)

                    binding.subredditInfo.selectFlairSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            updateUserFlair(getFlairAt(position))
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
                binding.subredditInfo.selectFlairLoadingIconLayout.visibility = if (it) {
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
            postsFragment.addScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // This *shouldn't* be called after onDestroyView, so _binding *shouldn't* be nulled
                    // but just to be 100 % to not cause a crash on this use null checks
                    if (dy > 0) {
                        _binding?.submitPostFab?.hide()
                    } else {
                        _binding?.submitPostFab?.show()
                    }
                }
            })

            binding.submitPostFab.setOnClickListener {
                Intent(context, SubmitActivity::class.java).apply {
                    putExtra(SubmitActivity.SUBREDDIT_KEY, subredditName)
                    startActivity(this)
                }
            }
        }
    }

    /**
     * Sets the icon and banner on the subreddit, as well as loading the fragments for the pagers and
     * initializing the rules/flairs ViewModels
     *
     * @param subreddit The subreddit to load. If the name of this subreddit matches the adapter found
     * in the ViewPager then the subreddit will only be changed on the binding
     */
    private fun setSubredditAndLoadPosts(subreddit: Subreddit) {
        // Chance of this having to change is small, but just to be sure
        ViewUtil.setSubredditIcon(binding.subredditIcon, subreddit)
        setBannerImage()

        val pagerAdapter = binding.pager.adapter
        if (pagerAdapter is Adapter && pagerAdapter.subreddit.name == subreddit.name) {
            return
        }
        
        setupViewPager(subreddit)
        setupRulesViewModel(subreddit.name)
        setupFlairsViewModel(subreddit.name)
    }

    /**
     * Sets the subreddits banner image. If no image is found, the banners visibility is set to [View.GONE].
     * When (if) the image loads then [FragmentSubredditBinding.setBannerLoaded] is called with `true`.
     * If data saving is enabled, then the image will only be loaded if it is already cached, and will
     * not load from the network
     */
    private fun setBannerImage() {
        if (isDefaultSubreddit) {
            bannerLoaded(false)
            return
        }

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
                                        bannerLoaded(true)
                                    }

                                    override fun onError(e: Exception) {
                                        bannerLoaded(false)
                                    }
                                })
                    } else {
                        Picasso.get()
                                .load(bannerURL)
                                .into(imageView, object : Callback {
                                    override fun onSuccess() {
                                        bannerLoaded(true)
                                    }

                                    override fun onError(e: Exception) {
                                        bannerLoaded(false)
                                    }
                                })
                    }
                } else {
                    bannerLoaded(false)
                }
            } else {
                bannerLoaded(false)
            }
        } ?: bannerLoaded(false)
    }

    /**
     * Sets if the banner is loaded on the subreddit
     */
    private fun bannerLoaded(loaded: Boolean) {
        binding.bannerLoaded = loaded
        if (loaded) {
            binding.banner.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarBannerLoaded).toInt()
            }
            binding.collapsingToolbar.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarWithBanner).toInt()
            }
            binding.subredditAppBarLayout.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarWithBanner).toInt()
            }
            binding.collapsingToolbar.scrimVisibleHeightTrigger = resources.getDimension(R.dimen.subredditToolbarScrimWithBanner).toInt()
        } else {
            val color = when {
                !subreddit?.primaryColor.isNullOrEmpty() -> {
                    Color.parseColor(subreddit!!.primaryColor)
                }

                !subreddit?.keyColor.isNullOrEmpty() -> {
                    Color.parseColor(subreddit!!.keyColor)
                }

                else -> {
                    ContextCompat.getColor(requireContext(), R.color.secondary_background)
                }
            }

            binding.banner.setBackgroundColor(color)

            binding.banner.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarBannerNotLoaded).toInt()
            }
            binding.collapsingToolbar.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarWithoutBanner).toInt()
            }
            binding.subredditAppBarLayout.updateLayoutParams {
                height = resources.getDimension(R.dimen.subredditToolbarWithoutBanner).toInt()
            }
            binding.collapsingToolbar.scrimVisibleHeightTrigger = resources.getDimension(R.dimen.subredditToolbarScrimWithoutBanner).toInt()
        }
    }

    /**
     * Click listener for the "+ Subscribe/- Unsubscribe" button.
     *
     * Sends an API request to Reddit to subscribe/unsubscribe
     */
    private fun subscribeOnclick() {
        subredditViewModel?.subscribe()
    }

    /**
     * Updates the users flair on the subreddit
     *
     * If the username ([App.getUserInfo]) is `null` then this will return
     *
     * @param flair The flair to update, or `null` to disable the flair on the subreddit
     */
    private fun updateUserFlair(flair: RedditFlair?) {
        val username = App.get().getUserInfo()?.userInfo?.username ?: return

        subredditViewModel?.updateFlair(username, flair)
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

        _binding?.progressBarLayout?.visibility = if (count > 0) {
            View.VISIBLE
        } else {
            View.GONE
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
                handleGenericResponseErrors(binding.parentLayout, error, throwable)
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

    private fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
    }

    class WikiFragment : Fragment() {
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
        private val wikiLinkMovementMethod = InternalLinkMovementMethod { linkText, context ->
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
            } else if (url.matches("https://(.*)?reddit.com/r/$name/about/rules/?".toRegex())) {
                onRulesLinkClicked?.invoke()
            } else {
                Intent(context, DispatcherActivity::class.java).apply {
                    putExtra(DispatcherActivity.URL_KEY, linkText)
                    requireContext().startActivity(this)
                }
            }

            true
        }

        /**
         * Callback for when posts have started/finished loading
         *
         * @see isLoading
         */
        var onLoadingChange: ((Boolean) -> Unit)? = null

        /**
         * Callback for when a rules link for (only) the given subreddit has been clicked
         */
        var onRulesLinkClicked: (() -> Unit)? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            _binding = FragmentSubredditWikiBinding.inflate(inflater)

            with (binding.wikiContainer.layoutTransition) {
                setAnimateParentHierarchy(false)
                enableTransitionType(LayoutTransition.CHANGING)
            }

            binding.wikiContent.movementMethod = wikiLinkMovementMethod
            binding.wikiGoBack.setOnClickListener {
                wikiViewModel.pop()
            }

            setupViewModel()

            if (wikiViewModel.page.value == null) {
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
                    binding.wikiGoBack.visibility = if (canGoBack()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    if (it.content.isBlank()) {
                        binding.wikiContent.text = getString(R.string.subredditWikiEmpty)
                        return@observe
                    }

                    // Ensure we're at the top when loading a new page (this might have to be stored in onSaveInstanceState)
                    binding.scroller.scrollTo(0, 0)
                    val content = App.get().adjuster.adjust(it.content)

                    App.get().markwon.setMarkdown(binding.wikiContent, content)
                }

                isLoading.observe(viewLifecycleOwner) {
                    onLoadingChange?.invoke(it)
                }

                error.observe(viewLifecycleOwner) {
                    handleErrors(it)
                }
            }
        }

        fun isLoading() = wikiViewModel.isLoading.value

        private fun handleErrors(error: ErrorWrapper) {
            when (error.error.reason) {
                // Disabled is basically if the subreddit hasn't created a wiki (or disabled later I suppose)
                GenericError.WIKI_DISABLED -> {
                    // Just use this I guess
                    _binding?.wikiContent?.text = getString(R.string.subredditWikiDisabled)
                }

                GenericError.WIKI_PAGE_NOT_FOUND -> {
                    _binding?.wikiContent?.text = getString(R.string.subredditWikiNotFound)
                }

                // Moderator accessing a page not created
                GenericError.WIKI_PAGE_NOT_CREATED -> {
                    _binding?.wikiContent?.text = getString(R.string.subredditWikiNotCreated)
                }

                GenericError.WIKI_MAY_NOT_VIEW -> {
                    _binding?.wikiContent?.text = getString(R.string.subredditWikiCannotView)
                }

                else -> {
                    handleGenericResponseErrors(binding.root, error.error, error.throwable)
                }
            }
        }
    }

    private inner class Adapter(val subreddit: Subreddit, fragment: Fragment) : FragmentStateAdapter(fragment) {
        // Kind of bad to hardcode the count I guess but whatever
        override fun getItemCount() = if (isDefaultSubreddit || !subreddit.wikiEnabled) 1 else 2
        override fun createFragment(position: Int) : Fragment {
            return when (position) {
                0 -> {
                    val sort = arguments?.getString(SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
                    val timeSort = arguments?.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }
                    PostsFragment.newInstance(
                            isForUser = false,
                            name = subreddit.name,
                            sort = sort,
                            timeSort = timeSort
                    )
                }
                1 -> WikiFragment.newInstance(subreddit.name)

                else -> throw IllegalStateException("Unexpected position: $position")
            }
        }
    }
}