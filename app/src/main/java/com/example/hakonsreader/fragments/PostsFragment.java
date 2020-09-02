package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;

import java.util.List;

public class PostsFragment extends Fragment {


    private List<RedditPost> posts;
    private PostsAdapter adapter;

    private RecyclerView postsList;
    private TextView title;
    private String titleText;

    public PostsFragment(String titleText) {
        this.titleText = titleText;
    }

    public void setPosts(List<RedditPost> posts) {
        this.posts = posts;
        this.adapter.setPosts(this.posts);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        this.postsList = view.findViewById(R.id.posts);

        this.adapter = new PostsAdapter();
        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }
}
