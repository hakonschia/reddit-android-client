package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentProfileBinding
import com.example.hakonsreader.databinding.UserIsSuspendedBinding
import com.example.hakonsreader.interfaces.OnInboxClicked
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.viewmodels.RedditUserViewModel
import com.example.hakonsreader.viewmodels.factories.RedditUserFactory
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

/**
 * Fragment for displaying a Reddit user profile
 */
class ProfileFragment : Fragment() {

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

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val username by lazy { arguments?.getString(USERNAME_KEY) }

    /**
     * Flag to set if the fragment is for the logged in user or not
     */
    private val isLoggedInUser by lazy { arguments?.getBoolean(IS_LOGGED_IN_USER_KEY) ?: false }

    private val viewModel: RedditUserViewModel by viewModels {
        RedditUserFactory(username, isLoggedInUser)
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

        App.get().privatelyBrowsing.observe(viewLifecycleOwner) {
            privateBrowsingStateChanged(it)
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


    private fun setupViewModel() {
        with(viewModel!!) {
            user.observe(viewLifecycleOwner) {
                onUserResponse(it)
            }
            isLoading.observe(viewLifecycleOwner) {
                checkLoadingStatus()
            }
            error.observe(viewLifecycleOwner) {
                Util.handleGenericResponseErrors(binding.parentLayout, it.error, it.throwable)
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
            PostsFragment.newInstance(
                isForUser = true,
                name = name,
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
                                Util.handleGenericResponseErrors(it.root, error, throwable)
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
        binding.profilePicture.borderColor = ContextCompat.getColor(
                requireContext(),
                if (privatelyBrowsing) R.color.privatelyBrowsing else R.color.opposite_background
        )
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
                App.get().updateUserInfo(info = newUser)
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
        count += if (viewModel?.isLoading?.value == true) 1 else 0

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