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
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding
import com.example.hakonsreader.interfaces.OnClickListener
import com.example.hakonsreader.interfaces.OnSubredditSelected
import com.example.hakonsreader.misc.Util
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter
import com.example.hakonsreader.viewmodels.SearchForSubredditsViewModel
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModelK
import com.example.hakonsreader.viewmodels.factories.SelectSubredditsFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
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
class SelectSubredditFragmentK : Fragment() {
    private val TAG = "SelectSubredditFragment"

    private var binding: FragmentSelectSubredditBinding? = null
    private val api = App.get().api
    private val database = AppDatabase.getInstance(context)

    private val saveState: Bundle = Bundle()

    private var subredditsAdapter: SubredditsAdapter? = null
    private var subredditsLayoutManager: LinearLayoutManager? = null
    private var subredditsViewModel: SelectSubredditsViewModelK? = null

    private var searchSubredditsAdapter: SubredditsAdapter? = null
    private var searchSubredditsLayoutManager: LinearLayoutManager? = null
    private var searchSubredditsViewModel: SearchForSubredditsViewModel? = null

    private var searchTimerTask: TimerTask? = null

    /**
     * The listener for when a subreddit has been clicked
     */
    var subredditSelected: OnSubredditSelected? = null

    /**
     * Updates the favorite for a subreddit. Calls the Reddit API and based on the response
     * updates the favorite status and list accordingly
     */
    private fun favoriteClicked(subreddit: Subreddit) {
        val favorite = !subreddit.isFavorited
        api.subreddit(subreddit.name).favorite(favorite, {
            run {
                subreddit.isFavorited = favorite
                subredditsAdapter?.onFavorite(subreddit)

                // If the top is visible make sure the top is also visible after the item has moved
                if (subredditsLayoutManager?.findFirstCompletelyVisibleItemPosition() == 0) {
                    subredditsLayoutManager?.scrollToPosition(0)
                }

                CoroutineScope(IO).launch {
                    database.subreddits().update(subreddit)
                }
            }
        }, { error, throwable ->
            run {
                Util.handleGenericResponseErrors(view, error, throwable)
            }
        })
    }

    /**
     * Initializes [subredditsViewModel] and observes all its values
     */
    private fun setupSubredditsViewModel() {
        subredditsViewModel = ViewModelProvider(this, SelectSubredditsFactory(context))
                .get(SelectSubredditsViewModelK::class.java)

        subredditsViewModel!!.getSubreddits().observe(viewLifecycleOwner, { subreddits ->
            subredditsAdapter?.submitList(subreddits as MutableList<Subreddit>, true)
            subredditsLayoutManager?.onRestoreInstanceState(saveState.getParcelable(LIST_STATE_KEY))
        })

        subredditsViewModel!!.getOnCountChange().observe(viewLifecycleOwner, { onCountChange ->
            binding?.loadingIcon?.onCountChange(onCountChange)
        })

        subredditsViewModel!!.getError().observe(viewLifecycleOwner, { error ->
            Util.handleGenericResponseErrors(view, error.error, error.throwable)
        })
    }

    /**
     * Initializes [searchSubredditsViewModel] and observes all its values
     */
    private fun setupSearchViewModel() {
        searchSubredditsViewModel = ViewModelProvider(this).get(SearchForSubredditsViewModel::class.java)

        searchSubredditsViewModel!!.searchResults.observe(viewLifecycleOwner, { subreddits ->
            binding?.searchedSubredditsCount = subreddits.size

            // TODO make some sort of animation for this
            if (subreddits.isEmpty()) {
                searchSubredditsAdapter?.clear()
            } else {
                searchSubredditsAdapter?.submitList(subreddits, false)
                searchSubredditsLayoutManager?.scrollToPosition(0)
            }

            searchSubredditsLayoutManager?.onRestoreInstanceState(saveState.getParcelable(SEARCH_LIST_STATE_KEY))
        })

        searchSubredditsViewModel!!.onCountChange.observe(viewLifecycleOwner, { onCountChange ->
            binding?.loadingIcon?.onCountChange(onCountChange)
        })

        searchSubredditsViewModel!!.error.observe(viewLifecycleOwner, { error ->
            Util.handleGenericResponseErrors(view, error.error, error.throwable)
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSelectSubredditBinding.inflate(layoutInflater)

        // Initialize the list of subreddits
        subredditsAdapter = SubredditsAdapter()
        subredditsAdapter?.subredditSelected = subredditSelected
        subredditsAdapter?.favoriteClicked = OnClickListener { subreddit -> favoriteClicked(subreddit) }
        subredditsLayoutManager = LinearLayoutManager(context)

        binding?.subreddits?.adapter = subredditsAdapter
        binding?.subreddits?.layoutManager = subredditsLayoutManager

        // Initialize the list for searched subreddits
        searchSubredditsAdapter = SubredditsAdapter()
        searchSubredditsAdapter?.subredditSelected = subredditSelected
        searchSubredditsLayoutManager = LinearLayoutManager(context)

        binding?.searchedSubreddits?.adapter = searchSubredditsAdapter
        binding?.searchedSubreddits?.layoutManager = searchSubredditsLayoutManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.setupSearchViewModel()
        this.setupSubredditsViewModel()

        // TODO this probably shouldn't be called every time, since it's not necessary to load subreddits
        //  every time the user opens to the fragment. Can load every 5 hours or something
        subredditsViewModel?.loadSubreddits()

        binding?.subredditSearch?.setOnEditorActionListener(actionDoneListener)
        binding?.subredditSearch?.addTextChangedListener(automaticSearchListener)

        return binding?.root
    }


    override fun onPause() {
        super.onPause()

        saveState.putParcelable(LIST_STATE_KEY, subredditsLayoutManager?.onSaveInstanceState())
        saveState.putParcelable(SEARCH_LIST_STATE_KEY, searchSubredditsLayoutManager?.onSaveInstanceState())
    }

    override fun onResume() {
        super.onResume()

        // If there's text in the input field, returning to the fragment should not trigger another search
        // TODO restore the list as well
        searchTimerTask?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }


    /**
     * Listener for when the edit text has done a "actionDone", ie. the user pressed "enter" and wants
     * to go to the subreddit.
     *
     * If the subreddit name input isn't in the range 3..21 then a Snackbar is shown, as this is the
     * length requirement for a subreddit
     */
    private val actionDoneListener: TextView.OnEditorActionListener = TextView.OnEditorActionListener { v, actionId, event ->
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