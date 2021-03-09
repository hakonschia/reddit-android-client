package com.example.hakonsreader.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding
import com.example.hakonsreader.interfaces.OnClickListener
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter
import com.example.hakonsreader.viewmodels.SearchForSubredditsViewModel
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * Fragment for selecting a subreddit. This fragment shows a list of subreddits (either the logged
 * in users subscribed subreddits or a list of default subreddits) and allows the user
 * to search for subreddits by a search field
 */
class SelectSubredditFragment : Fragment() {
    companion object {
        private const val TAG = "SelectSubredditFragment"

        /**
         * The amount of milliseconds to wait to search for subreddits after text has been
         * input to the search field
         */
        private const val SUBREDDIT_SEARCH_DELAY: Long = 500

        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = SelectSubredditFragment()
    }


    private var _binding: FragmentSelectSubredditBinding? = null
    private val binding get() = _binding!!

    private val subredditsViewModel: SelectSubredditsViewModel by activityViewModels()
    private val searchSubredditsViewModel: SearchForSubredditsViewModel by viewModels()

    private var searchTimerTask: TimerTask? = null


    /**
     * The listener for when a subreddit has been clicked
     */
    var subredditSelected: OnSubredditSelected? = null
        set(value) {
            field = value
            // When the device orientation changes and the fragment is recreated, the setter for this
            // will be called after the fragment has gone through onCreate/onCreateView, so the
            // value set on subredditsAdapter will be null
            (_binding?.subreddits?.adapter as SubredditsAdapter?)?.subredditSelected = value
            (_binding?.searchedSubreddits?.adapter as SubredditsAdapter?)?.subredditSelected = value
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupBinding(container)

        setupSubredditsList()
        setupSearchSubredditsList()

        setupSearchViewModel()
        setupSubredditsViewModel()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // If there's text in the input field, returning to the fragment should not trigger another search
        searchTimerTask?.cancel()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


    /**
     * Inflates and sets up [binding]
     */
    private fun setupBinding(container: ViewGroup?) {
        _binding = FragmentSelectSubredditBinding.inflate(layoutInflater, container, false)

        binding.subredditSearch.setOnEditorActionListener(actionDoneListener)
        binding.subredditSearch.addTextChangedListener(automaticSearchListener)
    }

    /**
     * Sets up the list of subreddits (the "default" list shown with subscribed/default subreddits)
     */
    private fun setupSubredditsList() {
        SubredditsAdapter().run {
            this.subredditSelected = this@SelectSubredditFragment.subredditSelected
            favoriteClicked = OnClickListener { subreddit -> subredditsViewModel.favorite(subreddit) }

            binding.subreddits.adapter = this
        }

        binding.subreddits.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Sets up the list of subreddits from search results
     */
    private fun setupSearchSubredditsList() {
        SubredditsAdapter().run {
            this.subredditSelected = this@SelectSubredditFragment.subredditSelected
            binding.searchedSubreddits.adapter = this
        }

        binding.searchedSubreddits.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Initializes [subredditsViewModel] and observes all its values
     */
    private fun setupSubredditsViewModel() {
        with (subredditsViewModel) {
            subreddits.observe(viewLifecycleOwner, { subreddits ->
                (binding.subreddits.adapter as SubredditsAdapter?)
                        ?.submitList(subreddits as MutableList<Subreddit>, true)
            })

            isLoading.observe(viewLifecycleOwner, { isLoading ->
                binding.loadingIcon.visibility = if (isLoading == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            })

            error.observe(viewLifecycleOwner, { error ->
                // View should never be null when an observer is fired but whatever
                view?.let {
                    handleGenericResponseErrors(it, error.error, error.throwable)
                }
            })
        }
    }

    /**
     * Initializes [searchSubredditsViewModel] and observes all its values
     */
    private fun setupSearchViewModel() {
        with(searchSubredditsViewModel) {
            searchResults.observe(viewLifecycleOwner, { subreddits ->
                binding.searchedSubredditsCount = subreddits.size
                val adapter = binding.searchedSubreddits.adapter as SubredditsAdapter? ?: return@observe

                if (subreddits.isEmpty()) {
                    adapter.clear()
                } else {
                    adapter.submitList(subreddits.toMutableList(), false)
                }
            })

            isLoading.observe(viewLifecycleOwner, { onCountChange ->
                binding.loadingIcon.visibility = if (onCountChange == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            })

            error.observe(viewLifecycleOwner, { error ->
                view?.let {
                    handleGenericResponseErrors(it, error.error, error.throwable)
                }
            })
        }
    }


    /**
     * Listener for when the edit text has done a "actionDone", ie. the user pressed "enter" and wants
     * to go to the subreddit.
     *
     * If the subreddit name input isn't in the range 3..21 then a Snackbar is shown, as this is the
     * length requirement for a subreddit
     */
    private val actionDoneListener = TextView.OnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val subredditName = v.text.toString().trim()

            if (subredditName.length in 3..21) {
                val activity = requireActivity()

                val imm: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)

                subredditSelected?.subredditSelected(subredditName)

                // Ensure the search task is cancelled as it is no longer needed
                searchTimerTask?.cancel()
            } else {
                Snackbar.make(requireView(), getString(R.string.subredditMustBeBetweenLength), Snackbar.LENGTH_LONG).show()
            }
        }

        // Return true = event consumed
        // It makes sense to only return true if the subreddit is valid, but returning false hides
        // the keyboard which is annoying when you got an error and want to try again
        return@OnEditorActionListener true
    }

    /**
     * TextWatcher that schedules [searchTimerTask] to run a task to search for subreddits. The task runs
     * when no text has been input for [SUBREDDIT_SEARCH_DELAY] milliseconds and is not canceled elsewhere.
     */
    private val automaticSearchListener = object : TextWatcher {
        private val timer = Timer()

        override fun afterTextChanged(s: Editable?) {
            // Cancel the previous task
            searchTimerTask?.cancel()

            searchTimerTask = object : TimerTask() {
                override fun run() {
                    val searchQuery = s?.toString()

                    if (searchQuery?.isNotBlank() == true) {
                        searchSubredditsViewModel?.search(searchQuery)
                    } else {
                        searchSubredditsViewModel?.clearSearchResults()
                    }
                }
            }

            timer.schedule(searchTimerTask, SUBREDDIT_SEARCH_DELAY)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not implemented
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Not implemented
        }
    }
}