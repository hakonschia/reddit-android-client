package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentProfileBinding
import com.example.hakonsreader.interfaces.OnInboxClicked
import com.example.hakonsreader.interfaces.PrivateBrowsingObservable
import com.example.hakonsreader.misc.Util
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

        /**
         * The key set in [getArguments] that says the username the fragment is for
         */
        private const val USERNAME_KEY = "username"

        /**
         * The key set in [getArguments] that says if the fragment is for the logged in user
         */
        private const val IS_LOGGED_IN_USER_KEY = "isLoggedInUser"

        /**
         * The key for instance saves that says if info has been loaded about the user
         *
         * The value stored with this should be a `boolean`
         */
        private const val IS_INFO_LOADED = "isInfoLoaded"

        private const val POSTS_TAG = "posts_profile"

        private const val SAVED_POSTS_FRAGMENT = "savedPostsFragment"

        /**
         * Create a new ProfileFragment for a user by their username
         *
         * @param username The username to create the fragment for. If this is null, equal to "me", or the username
         * stored in SharedPreferences, the fragment will be for the logged in user. Default is `null` (ie. for
         * the logged in user)
         * @return A ProfileFragment
         */
        fun newInstance(username: String? = null) = ProfileFragment().apply {
            arguments = Bundle().apply {
                if (username == null || username == "me" || username.equals(App.get().currentUserInfo?.userInfo?.username, ignoreCase = true)) {
                    putBoolean(IS_LOGGED_IN_USER_KEY, true)
                }
                putString(USERNAME_KEY, username)
            }
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
    private var isInfoLoaded = false
    private var isInfoLoading = false

    private var postsFragment: PostsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            username = it.getString(USERNAME_KEY)
            isLoggedInUser = it.getBoolean(IS_LOGGED_IN_USER_KEY)
        }

        if (isLoggedInUser) {
            user = App.get().currentUserInfo?.userInfo
            user?.let {
                if (it.username.isNotBlank()) {
                    username = it.username
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding()

        App.get().registerPrivateBrowsingObservable(this)

        var infoLoaded = false
        // Retrieve user info if the fragment hasn't loaded any already
        if (savedInstanceState != null) {
            infoLoaded = savedInstanceState.getBoolean(IS_INFO_LOADED)
            postsFragment = childFragmentManager.getFragment(savedInstanceState, SAVED_POSTS_FRAGMENT) as PostsFragment?
        }

        if (!infoLoaded) {
            retrieveUserInfo()
        }

        // If we're on a logged in user, or the fragment has been recreated, we might have some old info
        updateViews()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // If we have no username we can't get posts (we can't ask for posts for the logged in user without
        // their username). The posts are retrieved automatically when the user information loads
        // TODO this might require some differences in PostsFragment
        //if (postsAdapter?.getPosts()?.isEmpty() == true && postIds.isEmpty() && username != null) {
        //    postsViewModel?.loadPosts()
        //}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // onSaveInstanceState is called for configuration changes (such as orientation)
        // so we need to store the animation state here and in saveState (for when the fragment has
        // been replaced but not destroyed)
        outState.putBoolean(IS_INFO_LOADED, isInfoLoaded)
        postsFragment?.let { childFragmentManager.putFragment(outState, SAVED_POSTS_FRAGMENT, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        App.get().unregisterPrivateBrowsingObservable(this)

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
            binding.collapsingToolbar.title = username
        }

        // Kinda weird to do this here, but even if we are privately browsing and on another users profile
        // it should indicate that we're privately browsing (as with your own profile and subreddits)
        binding.loggedInUser = isLoggedInUser

        binding.inbox.setOnClickListener {
            onInboxClicked?.onInboxClicked()
        }

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.profileToolbar)
    }

    /**
     * Creates a [PostsFragment] and adds it to [getChildFragmentManager]
     *
     * @param name The name of the profile the posts are for
     */
    private fun createAndAddPostsFragment(name: String) {
        if (postsFragment == null) {
            postsFragment = PostsFragment.newInstance(
                    isForUser = true,
                    name = name,
                    sort = SortingMethods.NEW
            ).apply {
                onError = { error, throwable ->
                    _binding?.let {
                        Util.handleGenericResponseErrors(it.root, error, throwable)
                    }
                }
                onLoadingChange = {
                    checkLoadingStatus()
                }
            }
        }

        childFragmentManager.beginTransaction()
                .replace(R.id.postsContainer, postsFragment!!, POSTS_TAG)
                .commit()
    }

    override fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
        binding.profilePicture.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
    }

    private fun retrieveUserInfo() {
        // TODO this should be in a new ViewModel for users (and isInfoLoading should be removed completely)
        isInfoLoading = true

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
                _binding?.let {
                    when (userResponse) {
                        is ApiResponse.Success -> onUserResponse(userResponse.value)
                        is ApiResponse.Error -> {
                            Util.handleGenericResponseErrors(it.parentLayout, userResponse.error, userResponse.throwable)
                        }
                    }
                }
            }
        }

        isInfoLoading = false
        checkLoadingStatus()
    }

    /**
     * Updates the fragments user information and loads posts automatically
     *
     * @param newUser The new user information
     */
    private fun onUserResponse(newUser: RedditUser) {
        isInfoLoaded = true
        user = newUser
        username = newUser.username

        // Store the updated user information if this profile is for the logged in user
        if (isLoggedInUser) {
            App.get().updateUserInfo(info = newUser)
        }

        createAndAddPostsFragment(newUser.username)

        updateViews()
    }

    /**
     * Checks all the loading values on the fragment and enables to loading icon accordingly
     */
    @Synchronized
    private fun checkLoadingStatus() {
        var count = 0

        count += if (postsFragment?.isLoading() == true) 1 else 0
        count += if (isInfoLoading) 1 else 0

        _binding?.loadingIcon?.visibility = if (count > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
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