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
import com.example.hakonsreader.databinding.FragmentSubredditBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.SubredditsAdapter;

public class SelectSubredditFragment extends Fragment {
    private static final String TAG = "SelectSubredditFragment";

    private RedditApi redditApi = App.getApi();

    private SubredditsAdapter subredditsAdapter;
    private LinearLayoutManager layoutManager;

    private FragmentSubredditBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentSubredditBinding.inflate(inflater);
        View view = this.binding.getRoot();


        subredditsAdapter = new SubredditsAdapter();
        layoutManager = new LinearLayoutManager(getContext());

        binding.posts.setAdapter(subredditsAdapter);
        binding.posts.setLayoutManager(layoutManager);

        redditApi.getSubscribedSubreddits("", 0, subreddits -> {
            subredditsAdapter.setSubreddits(subreddits);
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });

        return view;
    }
}
