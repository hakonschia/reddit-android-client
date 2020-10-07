package com.example.hakonsreader.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.PostActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.FragmentSubredditBinding;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.viewmodels.PostsViewModel;
import com.example.hakonsreader.views.ListDivider;
import com.example.hakonsreader.views.Post;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "SubredditFragment";

    /**
     * The amount of posts left in the list before attempting to load more posts automatically
     */
    private static final int NUM_REMAINING_POSTS_BEFORE_LOAD = 6;


    private FragmentSubredditBinding binding;

    private String subreddit;

    /**
     * The amount of items in the list at the last attempt at loading more posts
     */
    private int lastLoadAttemptCount;
    private int screenHeight = App.get().getScreenHeight();

    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    /**
     * Listener for scrolling in the posts list that automatically tries to load more posts
     */
    private View.OnScrollChangeListener scrollListener = (view, scrollX, scrollY, oldX, oldY) -> {
        // Find the positions of first and last visible items to find all visible items
        int posFirstItem = layoutManager.findFirstVisibleItemPosition();
        int posLastItem = layoutManager.findLastVisibleItemPosition();

        int listSize = adapter.getItemCount();

        // Load more posts before we reach the end to create an "infinite" list
        // Only load posts if there hasn't been an attempt at loading more posts
        if (posLastItem + NUM_REMAINING_POSTS_BEFORE_LOAD > listSize && lastLoadAttemptCount < listSize) {
            lastLoadAttemptCount = adapter.getPosts().size();
            postsViewModel.loadPosts(binding.parentLayout, subreddit);
        }

        this.checkSelectedPost(posFirstItem, posLastItem, oldY > 0);
    };

    /**
     * Creates a new instance of the fragment
     *
     * @param subreddit The subreddit to instantiate
     * @return The newly created fragment
     */
    public static SubredditFragment newInstance(String subreddit) {
        Bundle args = new Bundle();
        args.putString("subreddit", subreddit);

        SubredditFragment fragment = new SubredditFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Called when the refresh button has been clicked
     * <p>Clears the items in the list, scrolls to the top, and loads new posts</p>
     *
     * @param view Ignored
     */
    private void onRefreshPostsClicked(View view) {
        // Kinda weird to clear the posts here but works I guess?
        this.adapter.getPosts().clear();
        this.binding.posts.scrollToPosition(0);

        // TODO viewmodel load etc etc
        //this.loadPosts();
    }

    /**
     * Sets up {@link FragmentSubredditBinding#posts}
     */
    private void setupPostsList(View view) {
        layoutManager = new LinearLayoutManager(getActivity());

        binding.posts.setAdapter(adapter);
        binding.posts.setLayoutManager(layoutManager);
        binding.posts.setOnScrollChangeListener(scrollListener);

        ListDivider divider = new ListDivider(ContextCompat.getDrawable(getContext(), R.drawable.list_divider));
        binding.posts.addItemDecoration(divider);
    }

    /**
     * Goes through the selected range of posts and calls {@link PostsAdapter.ViewHolder#onSelected()}
     * and {@link PostsAdapter.ViewHolder#onUnSelected()} based on if a post has been "selected" (ie. is the main
     * item on the screen) or "unselected" (ie. no longer the main item)
     *
     * @param startPost The index of the post to start at (from {@link PostsAdapter#getPosts()} or {@link SubredditFragment#layoutManager}
     * @param endPost The index of the post to end at (from {@link PostsAdapter#getPosts()} or {@link SubredditFragment#layoutManager}
     * @param scrollingUp Whether or not we are scrolling up or down in the list
     */
    private void checkSelectedPost(int startPost, int endPost, boolean scrollingUp) {
        // The behavior is:
        // When scrolling UP:
        // 1. If the bottom of the content is under the screen, the view is UN SELECTED

        // When scrolling DOWN:
        // 1. If the top of the content is above the screen, the view is UN SELECTED
        // 2. If the top of the content is above 3/4th of the screen, the view is SELECTED


        // Go through all visible views and select/un select the view holder based on where on the screen they are
        for (int i = startPost; i <= endPost; i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

            // If we have no view holder there isn't anything we can do later
            if (viewHolder == null) {
                continue;
            }

            // (0, 0) is top left
            int y = viewHolder.getContentY();
            int viewBottom = viewHolder.getContentBottomY();

            if (scrollingUp) {
                if (viewBottom > screenHeight) {
                    viewHolder.onUnSelected();
                }
            } else {
                // If the view is above the screen (< 0) it is "unselected"
                // If the view is visible 3/4th the way up it is "selected"
                if (y < 0) {
                    viewHolder.onUnSelected();
                } else if (y < screenHeight / 4f) {
                    viewHolder.onSelected();
                }
            }

        }
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastLoadAttemptCount = 0;

        adapter = new PostsAdapter();

        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        postsViewModel.getPosts().observe(this, adapter::addPosts);
        postsViewModel.onLoadingChange().observe(this, up -> {
            binding.loadingIcon.onCountChange(up);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentSubredditBinding.inflate(inflater);
        View view = this.binding.getRoot();


        Bundle args = getArguments();
        if (args != null) {
            subreddit = args.getString("subreddit", "");

            // Set title in toolbar
            binding.subredditName.setText(subreddit.isEmpty() ? "Front page" : "r/" + subreddit);
        }

        // Bind the refresh button in the toolbar
        binding.subredditRefresh.setOnClickListener(this::onRefreshPostsClicked);

        // Setup the RecyclerView posts list
        this.setupPostsList(view);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the fragment is selected without any posts load posts automatically
        if (adapter.getPosts().isEmpty()) {
            postsViewModel.loadPosts(binding.parentLayout, subreddit);
        }
    }
}
