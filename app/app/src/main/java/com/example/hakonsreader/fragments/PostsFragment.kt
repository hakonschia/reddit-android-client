package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.activities.VideoActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.persistence.RedditPostsDao
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.databinding.FragmentPostsBinding
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener
import com.example.hakonsreader.viewmodels.PostsViewModel
import com.example.hakonsreader.views.Content
import com.example.hakonsreader.views.ContentVideo
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Fragment for displaying posts from a user/subreddit
 */
@AndroidEntryPoint
class PostsFragment : Fragment(), SortableWithTime {

    companion object {
        private const val TAG = "PostsFragment"


        /**
         * The key stored in [getArguments] saying the name the of the subreddit/user the posts are for
         */
        private const val ARGS_NAME = "args_subredditName"

        /**
         * The key stored in [getArguments] saying is [name] is for a user or subreddit
         */
        private const val ARGS_IS_FOR_USER = "args_isForUser"


        /**
         * The key used in [getArguments] for how to sort the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [SortingMethods]
         */
        private const val ARGS_SORT = "args_sort"

        /**
         * The key used in [getArguments] for the time sort for the posts when loading this subreddit
         *
         * The value with this key should be the value of corresponding enum value from [PostTimeSort]
         */
        private const val ARGS_TIME_SORT = "args_timeSort"


        /**
         * The amount of milliseconds that should the used to wait to open more posts after a post has
         * been opened
         */
        // This value is fairly arbitrary. Its purpose is to avoid posts being opened multiple times
        // if clicked fast after each other. The time can be relatively large as when opening posts normally
        // you have to wait for the animation anyways, which takes a decent amount of time
        // For "normal" use of opening posts, reading some comments, and then going back it will never be noticeable
        // You would have to actively try to notice this, and even then it's hard
        private const val POST_OPEN_TIMEOUT = 1250L


        fun newInstance(isForUser: Boolean, name: String, sort: SortingMethods? = null, timeSort: PostTimeSort? = null) = PostsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARGS_IS_FOR_USER, isForUser)
                putString(ARGS_NAME, name)
                sort?.let { putString(ARGS_SORT, it.value) }
                timeSort?.let { putString(ARGS_TIME_SORT, it.value) }
            }
        }
    }

    /**
     * The name of the subreddit/user the posts are for
     */
    val name: String by lazy { arguments?.getString(ARGS_NAME) ?: "" }

    /**
     * If true, the posts are for a user, not a subreddit
     */
    private val isForUser: Boolean by lazy { arguments?.getBoolean(ARGS_IS_FOR_USER) ?: false }

    /**
     * If true, the subreddit is a default subreddit (eg. front page)
     */
    private var isDefaultSubreddit = false

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var postsDao: RedditPostsDao

    private val postsViewModel: PostsViewModel by viewModels { PostsViewModel.Factory(name, isForUser, api, postsDao) }
    private val postsScrollListener: PostScrollListener = PostScrollListener { postsViewModel.loadPosts() }

    /**
     * A list of scroll listeners that were set before the fragments view was created, and should
     * be set on the posts list when created
     */
    private val scrollListeners: MutableList<RecyclerView.OnScrollListener> = ArrayList()


    /**
     * The timestamp the last time a post was opened (or -1 if no post has been opened)
     */
    private var lastPostOpened = -1L

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

    /**
     * The time, in minutes, the score should be hidden on the posts shown
     */
    var hideScoreTime = 0
        set(value) {
            field = value
            (_binding?.posts?.adapter as PostsAdapter?)?.hideScoreTime = value
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Posts in the activity open new activities with a transition, which requires an activity context
        // the LayoutInflater we get with Hilt isn't with an activity context
        return FragmentPostsBinding.inflate(LayoutInflater.from(requireActivity())).apply {
            _binding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Must not be a user to be a default subreddit (eg. /u/popular is an actual user)
        isDefaultSubreddit = !isForUser && RedditApi.STANDARD_SUBS.contains(name.toLowerCase())

        setupBinding()
        setupPostsList()
        setupPostsViewModel()

        viewLifecycleOwner.lifecycle.addObserver(postsScrollListener)
    }

    override fun onPause() {
        super.onPause()

        // If the layout is refreshing when the fragment is paused it can cause a leak (at least it used to)
        binding.postsRefreshLayout.isEnabled = false

        (binding.posts.adapter as PostsAdapter?)?.let {
            // Tell all the view holders to save their extras and then deselect them (primarily to pause videos)
            it.viewHolders.forEach { viewHolder ->
                viewHolder.saveExtras()
                viewHolder.onUnselected()
            }
            postsViewModel.savedPostStates = it.postExtras
        }
    }

    /**
     * Checks if there are posts already loaded. If there are no posts loaded [postsViewModel]
     * is notified to load posts automatically
     */
    override fun onResume() {
        super.onResume()
        binding.postsRefreshLayout.isEnabled = true

        val adapter = binding.posts.adapter as PostsAdapter?

        // If the fragment is selected without any posts load posts automatically
        if (adapter?.itemCount == 0) {
            val sort = arguments?.getString(ARGS_SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
            val timeSort = arguments?.getString(ARGS_TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }

            postsViewModel.loadPosts(sort, timeSort)
        }
    }

    override fun onDestroyView() {
        (binding.posts.adapter as PostsAdapter?)?.let {
            postsViewModel.savedPostStates = it.postExtras
        }
        binding.posts.adapter = null

        _binding = null
        super.onDestroyView()
    }

    /**
     * Adds a scroll listener to the posts RecyclerView.
     *
     * The listener will be stored and added when the fragments view is created. If the fragment view
     * is created when this is called then the listener is added now, as well as being stored to be
     * automatically added again if the view is recreated
     */
    fun addScrollListener(listener: RecyclerView.OnScrollListener) {
        //scrollListeners.add(listener)
        if (_binding != null) {
            binding.posts.addOnScrollListener(listener)
        }
    }

    /**
     * @return True if the posts are currently loading, false otherwise
     */
    fun isLoading() = postsViewModel.onLoadingCountChange.value ?: false

    private fun setupBinding() {
        with (binding) {
            scrollListeners.forEach {
                posts.addOnScrollListener(it)
            }

            postsRefreshLayout.setOnRefreshListener {
                refreshPosts()

                // The refreshing will be visible with our own progress bar
                postsRefreshLayout.isRefreshing = false
            }
            postsRefreshLayout.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(requireContext(), R.color.colorAccent)
            )
        }
    }

    /**
     * Sets up the posts list. The adapter set is a [PostsAdapter], with a [LinearLayoutManager] as the
     * layout manager
     */
    private fun setupPostsList() {
        PostsAdapter().apply {
            hideScoreTime = this@PostsFragment.hideScoreTime
            lifecycleOwner = viewLifecycleOwner
            binding.posts.adapter = this

            onVideoManuallyPaused = { contentVideo ->
                // Ignore post when scrolling if manually paused
                postsScrollListener.setPostToIgnore(contentVideo.redditPost?.id)
            }

            onVideoFullscreenListener = { contentVideo ->
                // Ignore post when scrolling if it has been fullscreened
                postsScrollListener.setPostToIgnore(contentVideo.redditPost?.id)

                val intent = Intent(context, VideoActivity::class.java).apply {
                    putExtra(VideoActivity.EXTRAS_EXTRAS, contentVideo.extras)
                }

                // Pause the video here so it doesn't play both places
                contentVideo.viewUnselected()
                requireContext().run {
                    startActivity(intent)
                    if (this is AppCompatActivity) {
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                }
            }

            onPostClicked = PostsAdapter.OnPostClicked { post ->
                val currentTime = System.currentTimeMillis()
                synchronized(lastPostOpened) {
                    if (lastPostOpened + POST_OPEN_TIMEOUT > currentTime) {
                        return@OnPostClicked
                    }

                    lastPostOpened = currentTime
                }

                // Ignore the post when scrolling, so that when we return and scroll a bit it doesn't
                // autoplay the video
                val redditPost = post.redditPost
                postsScrollListener.setPostToIgnore(redditPost?.id)

                val content = post.getContent()
                if (content is ContentVideo) {
                    val currentFrame = content.getCurrentFrame()
                    PostActivity.VIDEO_THUMBNAIL_BITMAP = currentFrame
                }

                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(PostActivity.EXTRAS_POST_KEY, Gson().toJson(redditPost))
                    putExtra(Content.EXTRAS, post.extras)
                    putExtra(PostActivity.EXTRAS_HIDE_SCORE_KEY, post.hideScore)
                }

                // Only really applicable for videos, as they should be paused
                post.viewUnselected()

                val activity = requireActivity()
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *post.transitionViews.toTypedArray())
                activity.startActivity(intent, options.toBundle())
            }
        }

        LinearLayoutManager(context).apply { binding.posts.layoutManager = this }
        binding.posts.setOnScrollChangeListener(postsScrollListener)
    }

    /**
     * Observes values on [postsViewModel]
     */
    private fun setupPostsViewModel() {
        with (postsViewModel) {
            posts.observe(viewLifecycleOwner, { posts ->
                val adapter = binding.posts.adapter as PostsAdapter?
                postsViewModel.savedPostStates?.let {
                    adapter?.postExtras = it
                }

                // Posts have been cleared, clear the adapter and clear the layout manager state
                if (posts.isEmpty()) {
                    adapter?.clearPosts()
                    binding.posts.layoutManager = LinearLayoutManager(context)
                    return@observe
                }

                adapter?.submitList(filterPosts(posts))
            })

            onLoadingCountChange.observe(viewLifecycleOwner, { onLoadingChange?.invoke(it) })

            error.observe(viewLifecycleOwner, { error ->
                // Error loading posts, reset onEndOfList so it tries again when scrolled
                postsScrollListener.resetOnEndOfList()
                onError?.invoke(error.error, error.throwable)
            })
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
        // TODO this should probably be in the ViewModel
        return if (isDefaultSubreddit) {
            val subsToFilter = Settings.subredditsToFilterFromDefaultSubreddits()
            posts.filter {
                // Keep the post if the subreddit it is in isn't found in subsToFilter
                !subsToFilter.contains(it.subreddit.toLowerCase())
            }
        } else {
            posts
        }
    }

    /**
     * Refreshes the posts in the fragment
     */
    fun refreshPosts() {
        postsViewModel.restart()
    }

    override fun new() {
        postsViewModel.restart(SortingMethods.NEW)
    }

    override fun hot() {
        postsViewModel.restart(SortingMethods.HOT)
    }

    override fun top(timeSort: PostTimeSort) {
        postsViewModel.restart(SortingMethods.TOP, timeSort)
    }

    override fun controversial(timeSort: PostTimeSort) {
        postsViewModel.restart(SortingMethods.CONTROVERSIAL, timeSort)
    }

    override fun currentSort(): SortingMethods {
        return postsViewModel.sort
    }

    override fun currentTimeSort(): PostTimeSort? {
       return when (postsViewModel.sort) {
           // This is only applicable for TOP/CONTROVERSIAL, but it might be set anyways from a previous
           // sort, so ensure it doesn't return the previous
           SortingMethods.TOP, SortingMethods.CONTROVERSIAL -> postsViewModel.timeSort
           else -> null
       }
    }
}