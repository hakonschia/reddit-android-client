package com.example.hakonsreader.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.databinding.FragmentSubredditBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.example.hakonsreader.viewmodels.PostsViewModel;
import com.example.hakonsreader.viewmodels.factories.PostsFactory;
import com.example.hakonsreader.views.ListDivider;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "SubredditFragment";

    /**
     * The amount of posts left in the list before attempting to load more posts automatically
     */
    private static final int NUM_REMAINING_POSTS_BEFORE_LOAD = 6;

    /**
     * The key representing how many list view states have been stored
     */

    private static final String FIRST_VIEW_STATE_STORED_KEY = "first_view_state_stored";
    private static final String LAST_VIEW_STATE_STORED_KEY = "last_view_state_stored";
    private static final String VIEW_STATE_STORED_KEY = "view_state_stored";

    /**
     * The key used to store the post IDs the fragment is showing
     */
    private static final String POST_IDS_KEY = "post_ids";

    /**
     * The key used to store the state of {@link SubredditFragment#layoutManager}
     */
    private static final String LAYOUT_STATE_KEY = "layout_state";



    private FragmentSubredditBinding binding;

    private AppDatabase database;
    private Bundle saveState;
    private String subredditName;
    private Subreddit subreddit;
    private List<String> postIds;

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
            postsViewModel.loadPosts(binding.parentLayout, subredditName);
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
    private void setupPostsList() {
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
                // TODO this might be a bit weird as scrolling up on the first item wont autplay
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


    /**
     * Restores the state of the visible ViewHolders based on {@link SubredditFragment#saveState}
     */
    private void restoreViewHolderStates() {
        if (saveState != null) {
            int firstVisible = saveState.getInt(FIRST_VIEW_STATE_STORED_KEY);
            int lastVisible = saveState.getInt(LAST_VIEW_STATE_STORED_KEY);

            for (int i = firstVisible; i <= lastVisible; i++) {
                PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

                // If the view has been destroyed the ViewHolders havent been created yet
                Bundle extras = saveState.getBundle(VIEW_STATE_STORED_KEY + i);
                if (extras != null && viewHolder != null) {
                    viewHolder.setExtras(extras);
                }
            }
        }
    }

    private void updateSubredditName() {
        binding.subredditName.setText(subredditName.isEmpty() ? "Front page" : "r/" + subredditName);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentSubredditBinding.inflate(getLayoutInflater());

        lastLoadAttemptCount = 0;
        adapter = new PostsAdapter();
        layoutManager = new LinearLayoutManager(getContext());
        postIds = new ArrayList<>();

        database = AppDatabase.getInstance(getContext());

        postsViewModel = new ViewModelProvider(this, new PostsFactory(
                getContext()
        )).get(PostsViewModel.class);
        postsViewModel.getPosts().observe(this, posts -> {
            int size = adapter.getItemCount();
            adapter.addPosts(posts);

            // Possible state to restore
            if (saveState != null && size == 0) {
                Parcelable state = saveState.getParcelable(LAYOUT_STATE_KEY);
                if (state != null) {
                    layoutManager.onRestoreInstanceState(saveState.getParcelable(LAYOUT_STATE_KEY));

                    // Resume videos etc etc
                    this.restoreViewHolderStates();
                }
            }
        });
        postsViewModel.onLoadingChange().observe(this, up -> {
            binding.loadingIcon.onCountChange(up);
        });

        Bundle args = getArguments();
        if (args != null) {
            subredditName = args.getString("subreddit", "");

            updateSubredditName();

            new Thread(() -> {
                subreddit = database.subreddits().get(subredditName);
                if (subreddit != null){
                    // In case the capitalization is different make sure it is updated
                    subredditName = subreddit.getName();
                    requireActivity().runOnUiThread(this::updateSubredditName);
                }
            }).start();

            App.get().getApi().getSubredditInfo(subredditName, sub -> {
                new Thread(() -> {
                    database.subreddits().insert(subreddit);
                    subredditName = sub.getName();
                    requireActivity().runOnUiThread(this::updateSubredditName);
                }).start();

                // Update UI when there is more UI to update
            }, (code, t) -> {
                Util.handleGenericResponseErrors(getView(), code, t);
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Bind the refresh button in the toolbar
        binding.subredditRefresh.setOnClickListener(this::onRefreshPostsClicked);

        // Setup the RecyclerView posts list
        this.setupPostsList();

        if (saveState != null) {
            postIds = saveState.getStringArrayList(POST_IDS_KEY);
            postsViewModel.setPostIds(postIds);
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // onDestroyView is called when a fragment is no longer visible. We store the list state of the fragment
        // here and when onCreateView is called again we restore the state (the fragment object itself is not
        // destroyed, so saveState will be the same)
        if (saveState == null) {
            saveState = new Bundle();
        }

        saveState.putParcelable(LAYOUT_STATE_KEY, layoutManager.onSaveInstanceState());
        saveState.putStringArrayList(POST_IDS_KEY, (ArrayList<String>) postsViewModel.getPostIds());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (saveState == null) {
            saveState = new Bundle();
        }

        // It's probably not necessary to loop through all, but ViewHolders are still active even when not visible
        // so just getting firstVisible and lastVisible probably won't be enough
        for (int i = 0; i < adapter.getItemCount(); i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

            if (viewHolder != null) {
                Bundle extras = viewHolder.getExtras();
                saveState.putBundle(VIEW_STATE_STORED_KEY + i, extras);

                viewHolder.pause();
            }
        }

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        saveState.putInt(FIRST_VIEW_STATE_STORED_KEY, firstVisible);
        saveState.putInt(LAST_VIEW_STATE_STORED_KEY, lastVisible);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Ensure that all videos are cleaned up
        for (int i = 0; i < adapter.getItemCount(); i++) {
            PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(i);

            if (viewHolder != null) {
                viewHolder.destroy();
            }
        }

        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the fragment is selected without any posts load posts automatically
        if (adapter.getPosts().isEmpty() && postIds.isEmpty()) {
            postsViewModel.loadPosts(binding.parentLayout, subredditName);
        }

        this.restoreViewHolderStates();
    }
}
