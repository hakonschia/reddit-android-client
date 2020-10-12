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
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding;
import com.example.hakonsreader.interfaces.OnSubredditSelected;
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter;
import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel;
import com.example.hakonsreader.viewmodels.factories.SelectSubredditsFactory;


public class SelectSubredditFragment extends Fragment {
    private static final String TAG = "SelectSubredditFragment";

    private RedditApi redditApi = App.get().getApi();

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


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentSelectSubredditBinding.inflate(inflater);
        View view = this.binding.getRoot();

        subredditsAdapter = new SubredditsAdapter();
        subredditsAdapter.setSubredditSelected(subredditSelected);
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

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
