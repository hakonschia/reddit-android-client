package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.persistence.RedditUserInfoDao
import com.example.hakonsreader.databinding.FragmentProfileBinding
import com.example.hakonsreader.databinding.UserIsSuspendedBinding
import com.example.hakonsreader.interfaces.OnInboxClicked
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.misc.dpToPixels
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.states.LoggedInState
import com.example.hakonsreader.viewmodels.RedditUserViewModel
import com.example.hakonsreader.viewmodels.assistedViewModel
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject

/**
 * Fragment for displaying a Reddit user profile
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"

        /**
         * The key set in [getArguments] that says the username the fragment is for
         *
         * The value for this key should be a [String]
         */
        private const val ARGS_USERNAME = "args_username"

        /**
         * The key set in [getArguments] that says if the fragment is for the logged in user
         *
         * The value for this key should be a [Boolean]
         */
        private const val ARGS_IS_LOGGED_IN_USER = "args_isLoggedInUser"


        /**
         * The key used in [onSaveInstanceState] to save the state of the posts fragment
         */
        private const val SAVED_POSTS_FRAGMENT = "saved_postsFragment"


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
                // Must be logged in (ie. not privately browsing)
                val state = AppState.loggedInState.value

                val isLoggedIn = state is LoggedInState.LoggedIn &&
                        (username.equals(AppState.getUserInfo()?.userInfo?.username, ignoreCase = true) ||
                        // No username given, or "me" should redirect to the logged in users profile
                        username == null || username == "me")

                putBoolean(ARGS_IS_LOGGED_IN_USER, isLoggedIn)

                // No username given and not a logged in user then we have to assume there is a username
                // stored previously that we can use as the "anonymous" user (this should happen when a user
                // browsing privately goes to their profile)
                if (!isLoggedIn && username == null) {
                    putString(ARGS_USERNAME, AppState.getUserInfo()?.userInfo?.username)
                } else {
                    putString(ARGS_USERNAME, username)
                }
            }
        }
    }

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var userInfoDao: RedditUserInfoDao

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val username by lazy { arguments?.getString(ARGS_USERNAME) }

    /**
     * Flag to set if the fragment is for the logged in user or not
     */
    private val isLoggedInUser by lazy { arguments?.getBoolean(ARGS_IS_LOGGED_IN_USER) ?: false }

    @Inject
    lateinit var userViewModelFactory: RedditUserViewModel.Factory

    private val viewModel: RedditUserViewModel by assistedViewModel {
        userViewModelFactory.create(username, isLoggedInUser, it)
    }

    var onInboxClicked: OnInboxClicked? = null

    private var postsFragment: PostsFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Retrieve user info if the fragment hasn't loaded any already
        if (savedInstanceState != null) {
            postsFragment = childFragmentManager.getFragment(savedInstanceState, SAVED_POSTS_FRAGMENT) as PostsFragment?
        }

        setupBinding()
        setupViewModel()
        addFragmentListener()

        AppState.loggedInState.observe(viewLifecycleOwner) {
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
        postsFragment?.let { childFragmentManager.putFragment(outState, SAVED_POSTS_FRAGMENT, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Inflates and sets up [binding]
     */
    private fun setupBinding() {
        // The inflate must be an activity as popup menus require an activity context
        _binding = FragmentProfileBinding.inflate(LayoutInflater.from(requireActivity()))

        binding.api = api
        binding.userInfoDao = userInfoDao

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


    private fun setupViewModel() {
        with(viewModel) {
            user.observe(viewLifecycleOwner) {
                onUserResponse(it)
            }
            isLoading.observe(viewLifecycleOwner) {
                checkLoadingStatus()
            }
            error.observe(viewLifecycleOwner) {
                handleGenericResponseErrors(binding.parentLayout, it.error, it.throwable)
            }

            load()
        }
    }

    /**
     * Creates a [PostsFragment] and adds it to [getChildFragmentManager]. If [postsFragment] is not null
     * it is added directly
     *
     * @param name The name of the profile the posts are for
     */
    private fun createAndAddPostsFragment(name: String) {
        val fragment = if (postsFragment != null) {
            postsFragment!!
        } else {
            PostsFragment.newUser(
                username = name,
                sort = SortingMethods.NEW
            )
        }

        childFragmentManager.beginTransaction()
                .replace(R.id.postsContainer, fragment)
                .commit()
    }

    /**
     * Adds a fragment lifecycle listener to [getChildFragmentManager] that sets [PostsFragment] and
     * as well as setting listeners on the fragment
     *
     * When their views are destroyed the references will be nulled.
     */
    private fun addFragmentListener() {
        childFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                if (f is PostsFragment) {
                    postsFragment = f.apply {
                        onError = { error, throwable ->
                            _binding?.let {
                                handleGenericResponseErrors(it.root, error, throwable)
                            }
                        }
                        onLoadingChange = {
                            checkLoadingStatus()
                        }
                        postsFragment = this
                    }
                }
            }

            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is PostsFragment) {
                    postsFragment = null
                }
            }
        }, false)
    }


    private fun privateBrowsingStateChanged(privatelyBrowsing: Boolean) {
        binding.privatelyBrowsing = privatelyBrowsing
    }

    /**
     * Updates the fragments user information and loads posts automatically
     *
     * @param newUser The new user information
     */
    private fun onUserResponse(newUser: RedditUser) {
        if (newUser.isSuspended) {
            userIsSuspended(newUser)
        } else {
            // Store the updated user information if this profile is for the logged in user
            if (isLoggedInUser) {
                CoroutineScope(IO).launch {
                    AppState.updateUserInfo(info = newUser)
                }
            }

            createAndAddPostsFragment(newUser.username)
            binding.user = newUser
        }
    }

    /**
     * Checks all the loading values on the fragment and enables to loading icon accordingly
     */
    @Synchronized
    private fun checkLoadingStatus() {
        var count = 0

        count += if (postsFragment?.isLoading() == true) 1 else 0
        count += if (viewModel.isLoading.value == true) 1 else 0

        _binding?.progressBarLayout?.visibility = if (count > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun userIsSuspended(user: RedditUser) {
        UserIsSuspendedBinding.inflate(layoutInflater, binding.parentLayout, true).apply {
            username = user.username
            (root.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.CENTER
            root.requestLayout()
        }
    }
}

/**
 * Binding adapter for loading a profile picture. This will prefer loading a snoovatar ([RedditUser.snoovatarImage])
 * if one is found
 *
 * @param imageView The view to load the picture into
 * @param user The user to load the picture for
 */
@BindingAdapter("profilePicture")
fun setProfilePicture(imageView: RoundedImageView, user: RedditUser?) {
    user ?: return

    val url = if (user.snoovatarImage.isNotEmpty()) {
        with(imageView) {
            borderWidth = 0f
            cornerRadius = 0f
            mutateBackground(false)
        }
        user.snoovatarImage
    } else {
        with(imageView) {
            borderWidth = dpToPixels(2f, imageView.resources).toFloat()
            borderColor = ContextCompat.getColor(imageView.context, R.color.opposite_background)
            cornerRadius = dpToPixels(30f, imageView.resources).toFloat()
            mutateBackground(true)
        }
        user.profilePicture
    }

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