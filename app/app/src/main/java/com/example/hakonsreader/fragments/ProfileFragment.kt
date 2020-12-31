package com.example.hakonsreader.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.PostActivity
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentProfileBinding
import com.example.hakonsreader.interfaces.OnInboxClicked
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter
import com.example.hakonsreader.recyclerviewadapters.listeners.PostScrollListener
import com.example.hakonsreader.viewmodels.PostsViewModel
import com.example.hakonsreader.viewmodels.factories.PostsFactory
import com.example.hakonsreader.views.Content
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class ProfileFragment : Fragment(), PrivateBrowsingObservable {

    companion object {
        private const val TAG = "ProfileFragment"

        private const val POST_IDS_KEY = "post_ids_profile"

        private const val LAYOUT_STATE_KEY = "layout_state_profile"

        /**
         * The key used to save the progress of the MotionLayout
         */
        private const val LAYOUT_ANIMATION_PROGRESS_KEY = "layout_progress"

        /**
         * The key set in the bundle with getArguments() that says the username the fragment is for
         */
        private const val USERNAME_KEY = "username"

        /**
         * The key set in the bundle with getArguments() that says if the fragment is for the logged in user
         */
        private const val IS_LOGGED_IN_USER_KEY = "isLoggedInUser"


        /**
         * Creates a new ProfileFragment for logged in users
         *
         * @return A new ProfileFragment for logged in users
         */
        fun newInstance() : ProfileFragment {
            val args = Bundle()
            args.putBoolean(IS_LOGGED_IN_USER_KEY, true)

            val fragment = ProfileFragment()
            fragment.arguments = args

            return fragment
        }

        /**
         * Create a new ProfileFragment for a user by their username
         *
         * @param username The username to create the fragment for. If this is equal to "me" or the username
         *                 stored in SharedPreferences, the fragment will be for the logged in user
         * @return A ProfileFragment for a user
         */
        fun newInstance(username: String) : ProfileFragment {
            val user = App.storedUser
            Log.d(TAG, "newInstance: Creating new ProfileFragment for $username")

            if (username == "me" || username.equals(user?.username, ignoreCase = true)) {
                return newInstance()
            }

            val args = Bundle()
            args.putString(USERNAME_KEY, username)

            val fragment = ProfileFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private val api = App.get().api
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    /**
     * The object representing the Reddit user the fragment is for
     */
    private var user: RedditUser? = null

    /**
     * Flag to set if the fragment is for the logged in user or not
     */
    private var isLoggedInUser = false

    var onInboxClicked: OnInboxClicked? = null

    private var username: String? = null

    private var saveState: Bundle? = null
    private var postIds = ArrayList<String>()
    private var postsViewModel: PostsViewModel? = null
    private var postsAdapter: PostsAdapter? = null
    private var postsLayoutManager: LinearLayoutManager? = null
    private var postsScrollListener: PostScrollListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            username = it.getString(USERNAME_KEY)
            isLoggedInUser = it.getBoolean(IS_LOGGED_IN_USER_KEY)
        }

        if (isLoggedInUser) {
            user = App.storedUser
            user?.let {
                if (it.username.isNotBlank()) {
                    username = it.username
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()
        setupPostsList()
        setupPostsViewModel()
        
        App.get().registerPrivateBrowsingObservable(this)

        // Retrieve user info if the fragment hasn't loaded any already
        if (savedInstanceState == null) {
            retrieveUserInfo()
        } else {
            binding.parentLayout.progress = savedInstanceState.getFloat(LAYOUT_ANIMATION_PROGRESS_KEY)
            postIds = savedInstanceState.getStringArrayList(POST_IDS_KEY) as ArrayList<String>
            postsViewModel?.postIds = postIds
        }

        if (saveState != null) {
            // getFloat() will return 0.0f if not found, and won't ever be null
            binding.parentLayout.progress = saveState?.getFloat(LAYOUT_ANIMATION_PROGRESS_KEY)!!

            postIds = saveState?.getStringArrayList(POST_IDS_KEY) as ArrayList<String>
            postsViewModel?.postIds = postIds
        }

        // If we're on a logged in user, or the fragment has been recreated, we might have some old info
        updateViews()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // If we have no username we can't get posts (we can't ask for posts for the logged in user without
        // their username). The posts are retrieved automatically when the user information loads
        if (postsAdapter?.posts?.isEmpty() == true && postIds.isEmpty() && username != null) {
            postsViewModel?.loadPosts()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // onSaveInstanceState is called for configuration changes (such as orientation)
        // so we need to store the animation state here and in saveState (for when the fragment has
        // been replaced but not destroyed)
        // TODO this should be fixed as this is called on occasions such as theme change, but the view
        //  has been destroyed so _binding is nulled
        _binding?.let {  outState.putFloat(LAYOUT_ANIMATION_PROGRESS_KEY, it.parentLayout.progress) }
        outState.putParcelable(LAYOUT_STATE_KEY, postsLayoutManager?.onSaveInstanceState())
        outState.putStringArrayList(POST_IDS_KEY, postsViewModel?.postIds as ArrayList<String>?)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        App.get().unregisterPrivateBrowsingObservable(this)
        
        if (saveState == null) {
            saveState = Bundle()
        }

        saveState?.putFloat(LAYOUT_ANIMATION_PROGRESS_KEY, binding.parentLayout.progress)
        saveState?.putParcelable(LAYOUT_STATE_KEY, postsLayoutManager?.onSaveInstanceState())
        saveState?.putStringArrayList(POST_IDS_KEY, postsViewModel?.postIds as ArrayList<String>?)

        _binding = null
    }

    /**
     * Updates [binding] with new user information, if [user] isn't *null*
     */
    private fun updateViews() {
        if (user != null) {
            binding.user = user
        }
    }

    /**
     * Inflates and sets up [binding]
     */
    private fun setupBinding() {
        _binding = FragmentProfileBinding.inflate(layoutInflater)

        // We might not have a username at this point (first time loading for logged in user)
        if (username != null) {
            binding.username.text = username
        }

        // Kinda weird to do this here, but even if we are privately browsing and on another users profile
        // it should indicate that we're privately browsing (as with your own profile and subreddits)
        binding.loggedInUser = isLoggedInUser

        binding.inbox.setOnClickListener {
            onInboxClicked?.onInboxClicked()
        }
    }

    /**
     * Sets up [postsViewModel] and observes the values it exposes
     *
     * If [username] is not set (is null or blank) then this won't do anything
     */
    private fun setupPostsViewModel() {
        if (username.isNullOrBlank()) {
            return
        }

        postsViewModel = ViewModelProvider(this, PostsFactory(
                requireContext(),
                username,
                true
        )).get(PostsViewModel::class.java)

        postsViewModel?.getPosts()?.observe(viewLifecycleOwner, { posts ->
            postsAdapter?.submitList(posts)

            // Restore state of layout manager if possible
            if (saveState != null) {
                val layoutState: Parcelable? = saveState?.getParcelable(LAYOUT_STATE_KEY)
                if (layoutState != null) {
                    postsLayoutManager?.onRestoreInstanceState(layoutState)
                }
            }
        })
        postsViewModel?.getError()?.observe(viewLifecycleOwner, { e ->
            Util.handleGenericResponseErrors(binding?.parentLayout, e.error, e.throwable)
        })
        postsViewModel?.onLoadingCountChange()?.observe(viewLifecycleOwner, { up -> binding?.loadingIcon?.onCountChange(up) })
    }

    /**
     * Sets up [FragmentProfileBinding.posts] and related variables it uses
     */
    private fun setupPostsList() {
        postsAdapter = PostsAdapter()
        postsLayoutManager = LinearLayoutManager(requireContext())

        binding.posts.adapter = postsAdapter
        binding.posts.layoutManager = postsLayoutManager

        postsScrollListener = PostScrollListener(binding.posts) { postsViewModel?.loadPosts() }
        binding.posts.setOnScrollChangeListener(postsScrollListener)

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

    override fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
        binding.profilePicture.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
    }


    private fun retrieveUserInfo() {
        binding.loadingIcon.onCountChange(true)

        CoroutineScope(IO).launch {
            val name = username
            // If we're privately browsing, attempting to get user info for a logged in user would
            // fail with a "You're currently privately browsing"
            // If the user is privately browsing but no name is previously set this would fail since name would be null
            // But that should never happen? A logged in user should always have a user object with name stored
            val userResponse = if ((isLoggedInUser && !App.get().isUserLoggedInPrivatelyBrowsing()) || name == null) {
                api.user().info()
            } else {
                api.user(name).info()
            }

            withContext(Main) {
                // In case the view has been destroyed before we get a response
                if (_binding != null) {
                    binding.loadingIcon.onCountChange(false)

                    when (userResponse) {
                        is ApiResponse.Success -> onUserResponse(userResponse.value)
                        is ApiResponse.Error -> {
                            Util.handleGenericResponseErrors(binding.parentLayout, userResponse.error, userResponse.throwable)
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the fragments user information and loads posts automatically
     *
     * @param newUser The new user information
     */
    private fun onUserResponse(newUser: RedditUser) {
        user = newUser
        username = newUser.username

        // Store the updated user information if this profile is for the logged in user
        if (isLoggedInUser) {
            App.storeUserInfo(newUser)
        }

        if (postsViewModel == null) {
            setupPostsViewModel()
        }

        postsViewModel?.loadPosts()
        updateViews()
    }
}

/**
 * Binding adapter for loading a profile picture
 *
 * @param imageView The view to load the picture into
 * @param url The URL to the picture
 */
@BindingAdapter("profilePicture")
fun setProfilePicture(imageView: ImageView, url: String) {
    // Load the users profile picture
    if (url.isNotBlank()) {
        Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_baseline_person_100)
                .error(R.drawable.ic_baseline_person_100)
                .into(imageView)
    }
}

/**
 * Binding adapter to set the profile age text. The text is formatted as "d. MMMM y",
 * 5. September 2012"
 *
 * @param textView The TextView to set the text on
 * @param createdAt The timestamp, in seconds, the profile was created. If this is negative, nothing is done
 */
@BindingAdapter("profileAge")
fun setProfileAge(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        // Format date as "5. September 2012"
        val dateFormat = SimpleDateFormat("d. MMMM y", Locale.getDefault())
        val date = Date.from(Instant.ofEpochSecond(createdAt))

        textView.text = String.format(textView.resources.getString(R.string.profileAge), dateFormat.format(date))
    }
}