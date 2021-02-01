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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding
import com.example.hakonsreader.interfaces.OnClickListener
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter
import com.example.hakonsreader.viewmodels.SearchForSubredditsViewModel
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/**
 * The key used to store the state of the subreddits list
 */
private const val LIST_STATE_KEY = "listState"

/**
 * The key used to store the state of the searched subreddits list
 */
private const val SEARCH_LIST_STATE_KEY = "searchListState"

/**
 * The amount of milliseconds to wait to search for subreddits after text has been
 * input to the search field
 */
private const val SUBREDDIT_SEARCH_DELAY: Long = 500


/**
 * Fragment for selecting a subreddit. This fragment shows a list of subreddits (either the logged
 * in users subscribed subreddits or a list of default subreddits) and allows the user
 * to search for subreddits by a search field
 */
class SelectSubredditFragment : Fragment() {
    private val TAG = "SelectSubredditFragment"

    private var _binding: FragmentSelectSubredditBinding? = null
    private val binding get() = _binding!!
    private val api = App.get().api
    private val database = App.get().database

    private val saveState: Bundle = Bundle()

    private var subredditsAdapter: SubredditsAdapter? = null
    private var subredditsLayoutManager: LinearLayoutManager? = null
    private var subredditsViewModel: SelectSubredditsViewModel? = null

    private var searchSubredditsAdapter: SubredditsAdapter? = null
    private var searchSubredditsLayoutManager: LinearLayoutManager? = null
    private var searchSubredditsViewModel: SearchForSubredditsViewModel? = null

    private var searchTimerTask: TimerTask? = null

    /**
     * Flag to check if subreddits have already been loaded, and shouldn't be loaded again when
     * the fragment view is recreated
     */
    private var subredditsLoaded = false


    /**
     * The listener for when a subreddit has been clicked
     */
    var subredditSelected: OnSubredditSelected? = null
        set(value) {
            field = value
            // When the device orientation changes and the fragment is recreated, the setter for this
            // will be called after the fragment has gone through onCreate/onCreateView, so the
            // value set on subredditsAdapter will be null
            subredditsAdapter?.subredditSelected = value
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.setupBinding(container)

        this.setupSubredditsList()
        this.setupSearchSubredditsList()

        this.setupSearchViewModel()
        this.setupSubredditsViewModel()

        if (!subredditsLoaded) {
            val loadDefault = if (App.get().isUserLoggedIn()) {
                // If the user is logged in we want to load default subs if they're privately browsing
                App.get().isUserLoggedInPrivatelyBrowsing()
            } else {
                true
            }
            subredditsViewModel?.loadSubreddits(loadDefault)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // If there's text in the input field, returning to the fragment should not trigger another search
        searchTimerTask?.cancel()
    }

    override fun onPause() {
        super.onPause()

        saveState.putParcelable(LIST_STATE_KEY, subredditsLayoutManager?.onSaveInstanceState())
        saveState.putParcelable(SEARCH_LIST_STATE_KEY, searchSubredditsLayoutManager?.onSaveInstanceState())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    /**
     * Infaltes and sets up [binding]
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
        subredditsAdapter = SubredditsAdapter().apply {
            this.subredditSelected = this@SelectSubredditFragment.subredditSelected
            favoriteClicked = OnClickListener { subreddit -> favoriteClicked(subreddit) }

            binding.subreddits.adapter = this
        }

        subredditsLayoutManager = LinearLayoutManager(context).apply { binding.subreddits.layoutManager = this }
    }

    /**
     * Sets up the list of subreddits from search results
     */
    private fun setupSearchSubredditsList() {
        searchSubredditsAdapter = SubredditsAdapter().apply {
            this.subredditSelected = this@SelectSubredditFragment.subredditSelected
            binding.searchedSubreddits.adapter = this
        }

        searchSubredditsLayoutManager = LinearLayoutManager(context).apply { binding.searchedSubreddits.layoutManager = this }
    }

    /**
     * Initializes [subredditsViewModel] and observes all its values
     */
    private fun setupSubredditsViewModel() {
        subredditsViewModel = ViewModelProvider(this).get(SelectSubredditsViewModel::class.java).apply {
            getSubreddits().observe(viewLifecycleOwner, { subreddits ->
                subredditsLoaded = true
                subredditsAdapter?.submitList(subreddits as MutableList<Subreddit>, true)
                subredditsLayoutManager?.onRestoreInstanceState(saveState.getParcelable(LIST_STATE_KEY))
            })

            getOnCountChange().observe(viewLifecycleOwner, { onCountChange ->
                binding.loadingIcon.onCountChange(onCountChange)
            })

            getError().observe(viewLifecycleOwner, { error ->
                Util.handleGenericResponseErrors(view, error.error, error.throwable)
            })
        }
    }

    /**
     * Initializes [searchSubredditsViewModel] and observes all its values
     */
    private fun setupSearchViewModel() {
        searchSubredditsViewModel = ViewModelProvider(this).get(SearchForSubredditsViewModel::class.java).apply {
            getSearchResults().observe(viewLifecycleOwner, { subreddits ->
                binding.searchedSubredditsCount = subreddits.size

                // TODO make some sort of animation for this
                if (subreddits.isEmpty()) {
                    searchSubredditsAdapter?.clear()
                } else {
                    searchSubredditsAdapter?.submitList(subreddits.toMutableList(), false)
                    searchSubredditsLayoutManager?.scrollToPosition(0)
                }

                searchSubredditsLayoutManager?.onRestoreInstanceState(saveState.getParcelable(SEARCH_LIST_STATE_KEY))
            })

            getOnCountChange().observe(viewLifecycleOwner, { onCountChange ->
                binding.loadingIcon.onCountChange(onCountChange)
            })

            getError().observe(viewLifecycleOwner, { error ->
                Util.handleGenericResponseErrors(view, error.error, error.throwable)
            })
        }
    }


    /**
     * Updates the favorite for a subreddit. Calls the Reddit API and based on the response
     * updates the favorite status and list accordingly
     */
    private fun favoriteClicked(subreddit: Subreddit) {
        val favorite = !subreddit.isFavorited

        CoroutineScope(IO).launch {
            when (val response = api.subreddit(subreddit.name).favorite(favorite)) {
                is ApiResponse.Success -> {
                    subreddit.isFavorited = favorite
                    database.subreddits().update(subreddit)

                    withContext(Main) {
                        subredditsAdapter?.onFavorite(subreddit)
                        // If the top is visible make sure the top is also visible after the item has moved
                        if (subredditsLayoutManager?.findFirstCompletelyVisibleItemPosition() == 0) {
                            subredditsLayoutManager?.scrollToPosition(0)
                        }
                    }
                }
                is ApiResponse.Error -> Util.handleGenericResponseErrors(view, response.error, response.throwable)
            }
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