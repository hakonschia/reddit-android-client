package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Fragment containing a list of posts
 */
public class PostsFragment extends Fragment {
    private static final String TAG = "PostsFragment";
    
    
    private List<RedditPost> posts;
    private PostsAdapter adapter;

    private RecyclerView postsList;
    private TextView title;
    private String titleText;

    
    // Paging variables (where to load posts from etc)
    private int before;
    private int after;
    
    
    public PostsFragment(String titleText) {
        this.titleText = titleText;
        Log.d(TAG, "PostsFragment: creating PostFragment " + titleText);
    }

    public void setPosts(List<RedditPost> posts) {
        this.posts = posts;
        this.adapter.setPosts(this.posts);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: creating PostsFragment");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        this.postsList = view.findViewById(R.id.posts);
        this.title = view.findViewById(R.id.subredditName);

        this.title.setText(this.titleText);

        this.adapter = new PostsAdapter();
        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        Log.d(TAG, "onCreateView: Creating posts fragment view");

        return view;
    }
}
