package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.databinding.FragmentSelectSubredditBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectSubredditFragment extends Fragment {
    private static final String TAG = "SelectSubredditFragment";

    private RedditApi redditApi = App.getApi();

    private SubredditsAdapter subredditsAdapter;
    private LinearLayoutManager layoutManager;

    private FragmentSelectSubredditBinding binding;

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
        layoutManager = new LinearLayoutManager(getContext());

        binding.subreddits.setAdapter(subredditsAdapter);
        binding.subreddits.setLayoutManager(layoutManager);

        redditApi.getSubscribedSubreddits("", 0, subreddits -> {
            List<Subreddit> favorites = subreddits.stream()
                    .filter(Subreddit::userHasFavorited)
                    .collect(Collectors.toList());

            List<Subreddit> sorted = subreddits.stream()
                    // Sort based on subreddit name
                    .sorted((first, second) -> first.getName().compareTo(second.getName()))
                    .collect(Collectors.toList());
            // Remove the favorites to not include twice
            sorted.removeAll(favorites);

            List<Subreddit> combined = new ArrayList<>();
            combined.addAll(favorites);
            combined.addAll(sorted);

            subredditsAdapter.setSubreddits(combined);
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });

        return view;
    }
}
