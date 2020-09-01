package com.example.hakonsreader.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.recyclerviewadapters.SubredditRecyclerViewAdapter;

import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SubredditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "SubredditFragment";
    
    private List<RedditPost> posts;

    private SubredditRecyclerViewAdapter adapter;
    private RecyclerView postsList;


    public SubredditFragment() {
        // Required empty public constructor
    }

    public void setPosts(List<RedditPost> posts) {
        this.posts = posts;
        this.adapter.setPosts(this.posts);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SubredditFragment.
     */
    public static SubredditFragment newInstance() {
        SubredditFragment fragment = new SubredditFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_subreddit, container, false);

        this.adapter = new SubredditRecyclerViewAdapter();

        this.postsList = (RecyclerView) root.findViewById(R.id.lstPostsFrontPage);
        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inflate the layout for this fragment

        return root;
    }
}