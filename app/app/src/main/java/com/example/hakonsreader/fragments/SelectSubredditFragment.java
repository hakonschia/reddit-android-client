package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter;
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel;
import com.example.hakonsreader.viewmodels.factories.SelectSubredditsFactory;


public class SelectSubredditFragment extends Fragment {
    private static final String TAG = "SelectSubredditFragment";

    private RedditApi redditApi = App.get().getApi();
    private AppDatabase database;

    private SubredditsAdapter subredditsAdapter;
    private LinearLayoutManager layoutManager;

    private SelectSubredditsViewModel viewModel;

    private FragmentSelectSubredditBinding binding;
    private OnSubredditSelected subredditSelected;


    /**
     * Sets the listener for when a subreddit has been selected
     *
     * @param subredditSelected The listener to call
     */
    public void setSubredditSelected(OnSubredditSelected subredditSelected) {
        this.subredditSelected = subredditSelected;
    }

    public void favoriteClicked(Subreddit subreddit) {
        redditApi.favoriteSubreddit(subreddit.getName(), !subreddit.isFavorited(), ignored -> {
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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSelectSubredditBinding.inflate(inflater);
        database = AppDatabase.getInstance(getContext());

        subredditsAdapter = new SubredditsAdapter();
        subredditsAdapter.setSubredditSelected(subredditSelected);
        subredditsAdapter.setFavoriteClicked(this::favoriteClicked);
        layoutManager = new LinearLayoutManager(getContext());

        binding.subreddits.setAdapter(subredditsAdapter);
        binding.subreddits.setLayoutManager(layoutManager);

        viewModel = new ViewModelProvider(this, new SelectSubredditsFactory(
                getContext()
        )).get(SelectSubredditsViewModel.class);

        viewModel.getSubreddits().observe(getViewLifecycleOwner(), subreddits -> {
            subredditsAdapter.setSubreddits(subreddits);
        });

        viewModel.loadSubreddits();

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
