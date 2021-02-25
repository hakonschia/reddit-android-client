package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activities.PostActivity
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.GenericError
import com.example.hakonsreader.databinding.FragmentPostsBinding
import com.example.hakonsreader.interfaces.OnVideoFullscreenListener
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener
import com.example.hakonsreader.viewmodels.PostsViewModel
import com.example.hakonsreader.viewmodels.factories.PostsFactory
import com.example.hakonsreader.views.Content
import com.google.gson.Gson

class PostsFragment : Fragment(), SortableWithTime {

    companion object {

        /**
         * The key stored in [getArguments] saying the name the of the subreddit/user the posts are for
         */
        private const val NAME_KEY = "subredditName"

        /**
         * The key stored in [getArguments] saying is [name] is for a user or subreddit
         */
        private const val IS_FOR_USER = "isForUser"


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


        fun newInstance(isForUser: Boolean, name: String, sort: SortingMethods? = null, timeSort: PostTimeSort? = null) = PostsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(IS_FOR_USER, isForUser)
                putString(NAME_KEY, name)
                sort?.let { putString(SORT, it.value) }
                timeSort?.let { putString(TIME_SORT, it.value) }
            }
        }
    }

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    private var postsViewModel: PostsViewModel? = null
    private var postsAdapter: PostsAdapter? = null
    private var postsLayoutManager: LinearLayoutManager? = null
    private var postsScrollListener: PostScrollListener? = null

    /**
     * A list of scroll listeners that were set before the fragments view was created, and should
     * be set on the posts list when created
     */
    private val scrollListeners: MutableList<RecyclerView.OnScrollListener> = ArrayList()

    /**
     * The states/extras for the view holders saved when the fragment view was destroyed
     */
    // TODO this should also be saved in onSaveInstanceState
    private var savedViewHolderStates: HashMap<String, Bundle>? = null

    /**
     * Callback for errors when loading posts
     */
    var onError: ((GenericError, Throwable) -> Unit)? = null

    /**
     * Callback for when posts have started/finished loading
     */
    var onLoadingChange: ((Boolean) -> Unit)? = null

    val name: String by lazy { arguments?.getString(NAME_KEY) ?: "" }
    val isForUser: Boolean by lazy { arguments?.getBoolean(IS_FOR_USER) ?: false }

    private val isDefaultSubreddit = isForUser && RedditApi.STANDARD_SUBS.contains(name.toLowerCase())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostsBinding.inflate(inflater)

        setupBinding()
        setupPostsList()
        setupPostsViewModel(name, isForUser)

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
            val sort = arguments?.getString(SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
            val timeSort = arguments?.getString(TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }

            postsViewModel?.loadPosts(sort, timeSort)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        postsAdapter?.let {
            // Ensure that all videos are cleaned up
            for (i in 0 until it.itemCount) {
                val viewHolder = binding.posts.findViewHolderForLayoutPosition(i) as PostsAdapter.ViewHolder?
                if (viewHolder != null) {
                    // Ensure the extras for the view holder is saved
                    viewHolder.saveExtras()
                    viewHolder.destroy()
                }
            }

            savedViewHolderStates = it.postExtras

            it.lifecycleOwner = null
        }

        scrollListeners.clear()
        _binding = null
    }

    /**
     * Adds a scroll listener to the posts RecyclerView.
     * If the fragments view has not yet been created then this listener will be stored and added
     * when the view is created. The list of stored listeners will be cleared when the fragment view
     * is destroyed
     */
    fun addScrollListener(listener: RecyclerView.OnScrollListener) {
        if (_binding == null) {
            scrollListeners.add(listener)
        } else {
            binding.posts.addOnScrollListener(listener)
        }
    }

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
     * Sets up [postsAdapter]/[postsLayoutManager]
     */
    private fun setupPostsList() {
        postsAdapter = PostsAdapter().apply {
            savedViewHolderStates?.let {
                postExtras = it
            }

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

    /**
     * Sets up [postsViewModel]
     *
     * @param name The name of the subreddit the ViewModel is for
     */
    private fun setupPostsViewModel(name: String, isForUser: Boolean) {
        postsViewModel = ViewModelProvider(this, PostsFactory(
                name,
                isForUser
        )).get(PostsViewModel::class.java).apply {
            getPosts().observe(viewLifecycleOwner, { posts ->
                // Posts have been cleared, clear the adapter and clear the layout manager state
                if (posts.isEmpty()) {
                    postsAdapter?.clearPosts()
                    postsLayoutManager = LinearLayoutManager(context)
                    return@observe
                }

                postsAdapter?.submitList(filterPosts(posts))
            })

            onLoadingCountChange().observe(viewLifecycleOwner, { onLoadingChange?.invoke(it) })
            getError().observe(viewLifecycleOwner, { error ->
                // Error loading posts, reset onEndOfList so it tries again when scrolled
                postsScrollListener?.resetOnEndOfList()
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
     * Refreshes the posts in the fragment
     */
    fun refreshPosts() {
        postsViewModel?.restart()
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
           // This is only applicable for TOP/CONTROVERSIAL, but it might be set anyways from a previous
           // sort, so ensure it doesn't return the previous
           SortingMethods.TOP, SortingMethods.CONTROVERSIAL -> postsViewModel?.timeSort
           else -> null
       }
    }
}