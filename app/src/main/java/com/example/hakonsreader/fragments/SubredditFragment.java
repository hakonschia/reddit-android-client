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
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "PostsFragment";
    
    
    private List<RedditPost> posts;
    private PostsAdapter adapter;

    private RecyclerView postsList;
    private TextView title;
    private String subreddit;

    
    // Paging variables (where to load posts from etc)
    private int before = 0;
    private int after = 0;
    
    
    public SubredditFragment(String subreddit) {
        this.subreddit = subreddit;
        Log.d(TAG, "PostsFragment: creating PostFragment " + subreddit);
    }

    public void setPosts(List<RedditPost> posts) {
        this.posts = posts;
        this.adapter.setPosts(this.posts);
    }

    /**
     * Called when the fragment has been selected.
     * <p>If this is the first time the fragment is selected, posts are loaded</p>
     */
    public void onFragmentSelected() {
        Log.d(TAG, "onFragmentSelected: " + this.subreddit + " selected");
        // TODO If no posts are loaded, load from start. Otherwise do nothing really I guess
        this.loadPosts();
    }

    private void loadPosts() {
        // Load more posts :-d

        // Set posts on adapter etc etc
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: creating PostsFragment");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subreddit, container, false);

        this.postsList = view.findViewById(R.id.posts);
        this.title = view.findViewById(R.id.subredditName);

        this.title.setText(this.subreddit);

        this.adapter = new PostsAdapter();
        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        Log.d(TAG, "onCreateView: Creating posts fragment view");

        return view;
    }
}
