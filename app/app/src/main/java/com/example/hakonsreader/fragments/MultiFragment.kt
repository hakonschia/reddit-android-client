package com.example.hakonsreader.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.api.model.RedditMulti
import com.example.hakonsreader.databinding.FragmentMultiBinding
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.states.LoggedInState
import com.example.hakonsreader.views.util.goneIf
import com.example.hakonsreader.views.util.showPopupSortWithTime
import com.google.gson.Gson

/**
 * Fragment for displaying a Reddit Multi
 */
class MultiFragment : Fragment() {

    companion object {

        /**
         * The arguments key for the json serialized [RedditMulti]
         */
        private const val ARGS_MULTI = "args_multi"

        /**
         * The arguments key for the sort of the posts
         */
        private const val ARGS_SORT = "args_multiName"

        /**
         * The arguments key for the time sort of the posts
         */
        private const val ARGS_TIME_SORT = "args_timeSort"


        private const val SAVED_POSTS_FRAGMENT = "saved_postsFragment"


        /**
         * Creates a new fragment to display a Reddit Multi
         *
         * @param multi The Multi to load
         * @param sort How to sort the posts
         * @param timeSort How to time sort the posts
         */
        fun newInstance(
                multi: RedditMulti,
                sort: SortingMethods = SortingMethods.HOT,
                timeSort: PostTimeSort = PostTimeSort.DAY
        ) = MultiFragment().apply {
            arguments = bundleOf(
                    ARGS_MULTI to Gson().toJson(multi),
                    ARGS_SORT to sort.value,
                    ARGS_TIME_SORT to timeSort.value
            )
        }
    }

    private var _binding: FragmentMultiBinding? = null
    private val binding get() = _binding!!

    private var postsFragment: PostsFragment? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentMultiBinding.inflate(inflater).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            postsFragment = childFragmentManager.getFragment(savedInstanceState, SAVED_POSTS_FRAGMENT) as PostsFragment?
        }

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.multiToolbar)

        val multi = Gson().fromJson(requireArguments().getString(ARGS_MULTI)!!, RedditMulti::class.java)

        setupBinding(multi)

        addFragmentListener()
        createAndAddPostsFragment(multi)

        AppState.loggedInState.observe(viewLifecycleOwner) {
            when (it) {
                is LoggedInState.LoggedIn -> binding.privatelyBrowsing = false
                is LoggedInState.PrivatelyBrowsing -> binding.privatelyBrowsing = true

                // This observer is only really for private browsing changes
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        postsFragment?.let { childFragmentManager.putFragment(outState, SAVED_POSTS_FRAGMENT, it) }

        postsFragment = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBinding(multi: RedditMulti) {
        binding.multi = multi

        if (multi.keyColor.isNullOrEmpty()) {
            binding.banner.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_background))
        } else {
            binding.banner.setBackgroundColor(Color.parseColor(multi.keyColor))
        }

        binding.multiRefresh.setOnClickListener { postsFragment?.refreshPosts() }
        binding.multiSort.setOnClickListener { view ->
            postsFragment?.let {
                showPopupSortWithTime(it, view)
            }
        }
    }

    /**
     * Creates a [PostsFragment] and adds it to [getChildFragmentManager]. If [postsFragment] is not null
     * it is added directly
     */
    private fun createAndAddPostsFragment(multi: RedditMulti) {
        val fragment = if (postsFragment != null) {
            postsFragment!!
        } else {
            val args = requireArguments()

            val sort = args.getString(ARGS_SORT)?.let { s -> SortingMethods.values().find { it.value.equals(s, ignoreCase = true) } }
            val timeSort = args.getString(ARGS_TIME_SORT)?.let { s -> PostTimeSort.values().find { it.value.equals(s, ignoreCase = true) } }

            PostsFragment.newMulti(
                    username = multi.owner,
                    multiName = multi.name,
                    sort = sort,
                    timeSort = timeSort
            )
        }

        childFragmentManager.beginTransaction()
                .replace(R.id.postsContainer, fragment)
                .commit()
    }

    /**
     * Checks all the loading values on the fragment and enables to loading icon accordingly
     */
    @Synchronized
    private fun checkLoadingStatus() {
        var count = 0

        count += if (postsFragment?.isLoading() == true) 1 else 0

        _binding?.progressBarLayout?.goneIf(count <= 0)
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
        }, false)
    }
}