package com.example.hakonsreader.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter;
import com.example.hakonsreader.viewmodels.SearchForSubredditsViewModel;
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel;
import com.example.hakonsreader.viewmodels.factories.SelectSubredditsFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.Timer;
import java.util.TimerTask;


public class SelectSubredditFragment extends Fragment {
    private static final String TAG = "SelectSubredditFragment";

    /**
     * The key used to store the state of the subreddits list
     */
    private static final String LIST_STATE_KEY = "listState";

    /**
     * The key used to store the state of the searched subreddits list
     */
    private static final String SEARCH_LIST_STATE_KEY = "searchListState";


    /**
     * The amount of milliseconds to wait to search for subreddits after text has been input
     * to the search field
     */
    private static final int SUBREDDIT_SEARCH_DELAY = 500;

    private FragmentSelectSubredditBinding binding;
    private final RedditApi api = App.get().getApi();
    private AppDatabase database;

    private Bundle saveState;
    private SubredditsAdapter subredditsAdapter;
    private SubredditsAdapter searchSubredditsAdapter;
    private LinearLayoutManager layoutManager;
    private LinearLayoutManager searchLayoutManager;
    private SelectSubredditsViewModel viewModel;
    private SearchForSubredditsViewModel searchViewModel;
    private OnSubredditSelected subredditSelected;
    private TimerTask searchTimerTask;


    /**
     * Sets the listener for when a subreddit has been selected
     *
     * @param subredditSelected The listener to call
     */
    public void setSubredditSelected(OnSubredditSelected subredditSelected) {
        this.subredditSelected = subredditSelected;
    }

    public void favoriteClicked(Subreddit subreddit) {
        api.subreddit(subreddit.getName()).favorite(!subreddit.isFavorited(), ignored -> {
            subreddit.setFavorited(!subreddit.isFavorited());
            subredditsAdapter.onFavorite(subreddit);
            new Thread(() -> database.subreddits().update(subreddit)).start();

            // If the top is visible make sure the top is also visible after the item has moved
            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                layoutManager.scrollToPosition(0);
            }
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(getView(), code, t);
        });
    }

    /**
     * Sets up {@link SelectSubredditFragment#viewModel}
     */
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new SelectSubredditsFactory(
                getContext()
        )).get(SelectSubredditsViewModel.class);
        viewModel.getSubreddits().observe(getViewLifecycleOwner(), subreddits -> {
            subredditsAdapter.setSubreddits(subreddits);
            if (saveState != null) {
                layoutManager.onRestoreInstanceState(saveState.getParcelable(LIST_STATE_KEY));
            }
        });
    }

    /**
     * Sets up {@link SelectSubredditFragment#searchViewModel}
     */
    private void setupSearchViewModel() {
        searchViewModel = new ViewModelProvider(this).get(SearchForSubredditsViewModel.class);
        searchViewModel.getSearchResults().observe(getViewLifecycleOwner(), subreddits -> {
            binding.setSearchedSubredditsCount(subreddits.size());

            // TODO make some sort of animation for this
            if (subreddits.isEmpty()) {
                searchSubredditsAdapter.clear();
            } else {
                searchSubredditsAdapter.setSubreddits(subreddits);
            }
            if (saveState != null) {
                searchLayoutManager.onRestoreInstanceState(saveState.getParcelable(SEARCH_LIST_STATE_KEY));
            }
        });
        searchViewModel.getOnCountChange().observe(getViewLifecycleOwner(), onCountChange -> {
            binding.loadingIcon.onCountChange(onCountChange);
        });
        searchViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            Util.handleGenericResponseErrors(binding.parentLayout, error.getError(), error.getThrowable());
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentSelectSubredditBinding.inflate(getLayoutInflater());

        database = AppDatabase.getInstance(getContext());

        // Initialize the default list of subreddits
        subredditsAdapter = new SubredditsAdapter();
        subredditsAdapter.setSubredditSelected(subredditSelected);
        subredditsAdapter.setFavoriteClicked(this::favoriteClicked);
        layoutManager = new LinearLayoutManager(getContext());

        binding.subreddits.setAdapter(subredditsAdapter);
        binding.subreddits.setLayoutManager(layoutManager);

        // Initalize the list for searched subreddits
        searchSubredditsAdapter = new SubredditsAdapter();
        searchSubredditsAdapter.setSubredditSelected(subredditSelected);
        searchLayoutManager = new LinearLayoutManager(getContext());

        binding.searchedSubreddits.setAdapter(searchSubredditsAdapter);
        binding.searchedSubreddits.setLayoutManager(searchLayoutManager);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.setupViewModel();
        viewModel.loadSubreddits();

        this.setupSearchViewModel();

        binding.subredditSearch.setOnEditorActionListener(actionDoneListener);
        binding.subredditSearch.addTextChangedListener(subredditAutomaticSearchListener);

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (saveState == null) {
            saveState = new Bundle();
        }

        saveState.putParcelable(LIST_STATE_KEY, layoutManager.onSaveInstanceState());
        saveState.putParcelable(SEARCH_LIST_STATE_KEY, searchLayoutManager.onSaveInstanceState());
    }

    @Override
    public void onResume() {
        super.onResume();

        // If there's text in the input field, returning to the fragment should not trigger another search
        // TODO restore the list as well
        if (searchTimerTask != null) {
            searchTimerTask.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }


    /**
     * Listener for when the edit text has done a "actionDone", ie. the user is finished typing
     * and wants to go to the subreddit
     */
    private final TextView.OnEditorActionListener actionDoneListener = (v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String subredditName = v.getText().toString().trim();

            if (subredditName.length() >= 3 && subredditName.length() <= 21) {
                // Hide the keyboard. This isn't strictly needed as it will get hidden automatically
                // but that happens with a slight delay which means it's possible for the user to press
                // multiple times before it disappears
                Activity activity = requireActivity();
                View view = activity.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                subredditSelected.subredditSelected(subredditName);

                // Ensure the task is canceled (this should never be null, but you never know)
                if (searchTimerTask != null) {
                    searchTimerTask.cancel();
                }
            } else {
                Snackbar.make(binding.getRoot(), getString(R.string.subredditMustBeBetweenLength), Snackbar.LENGTH_LONG).show();
            }
        }

        // Return true = event consumed
        // It makes sense to only return true if the subreddit is valid, but returning false hides
        // the keyboard which is annoying when you got an error and want to try again
        return true;
    };

    /**
     * TextWatcher that schedules {@link SelectSubredditFragment#searchTimerTask} to run a task
     * to search for subreddits. The task runs when no text has been input for
     * {@link SelectSubredditFragment#SUBREDDIT_SEARCH_DELAY} milliseconds and is not canceled elsewhere.
     */
    private final TextWatcher subredditAutomaticSearchListener = new TextWatcher() {
        private final Timer timer = new Timer();

        @Override
        public void afterTextChanged(Editable s) {
            // After a delay of no text added, search for the subreddits in the field

            // Cancel the previous task to avoid it running
            if (searchTimerTask != null) {
                searchTimerTask.cancel();
            }

            searchTimerTask = new TimerTask() {
                @Override
                public void run() {
                    // Tell ViewModel to search for subreddits
                    String searchQuery = s.toString();

                    if (!searchQuery.trim().isEmpty()) {
                        searchViewModel.search(searchQuery);
                    } else {
                        // Remove search results when text is cleared
                        searchViewModel.clearSearchResults();
                    }
                }
            };

            timer.schedule(searchTimerTask, SUBREDDIT_SEARCH_DELAY);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not implemented
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Not implemented
        }
    };
}
