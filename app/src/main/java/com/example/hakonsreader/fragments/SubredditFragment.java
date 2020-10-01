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
    private ItemLoadingListener loadingListener;

    private String subreddit;

    /**
     * The amount of items in the list at the last attempt at loading more posts
     */
    private int lastLoadAttemptCount;

    private PostsViewModel postsViewModel;
    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;


    /**
     * Listener for scrolling in the posts list that automatically tries to load more posts
     */
    private View.OnScrollChangeListener scrollListener = (view, i, i1, i2, i3) -> {
        // Get the last item visible in the current list
        int posLastItem = layoutManager.findLastVisibleItemPosition();
        int listSize = adapter.getItemCount();

        // Load more posts before we reach the end to create an "infinite" list
        if (posLastItem + NUM_REMAINING_POSTS_BEFORE_LOAD > listSize) {

            // Only load posts if there hasn't been an attempt at loading more posts
            if (lastLoadAttemptCount < listSize) {
                lastLoadAttemptCount = adapter.getPosts().size();
                postsViewModel.loadPosts(binding.parentLayout, subreddit);
            }
        }

        int posFirstItem = layoutManager.findFirstVisibleItemPosition();
        PostsAdapter.ViewHolder viewHolder = (PostsAdapter.ViewHolder)binding.posts.findViewHolderForLayoutPosition(posFirstItem);
        if (viewHolder != null) {
            viewHolder.onSelected();
        }
    };

    private RecyclerView.OnChildAttachStateChangeListener attachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
            // The video is started in the scroll listener, as that can find when the view is the first visible
            // instead of when it is attached. It is attached before it is completely visible, so it would start
            // playing before the user would see most if it
        }

        @Override
        public void onChildViewDetachedFromWindow(@NonNull View view) {
            Post post = view.findViewById(R.id.post);
            post.pauseVideo();
        }
    };


    /**
     * Creates a new instance of the fragment
     *
     * @param subreddit The subreddit to instantiate
     * @return The newly created fragment
     */
    public static SubredditFragment newInstance(String subreddit) {
        Bundle args = new Bundle();
        Log.d(TAG, "newInstance: " + subreddit);

        args.putString("subreddit", subreddit);

        SubredditFragment fragment = new SubredditFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Sets the listener to be notified for when this listener has started/finished loading something
     *
     * @param loadingListener The listener
     */
    public void setLoadingListener(ItemLoadingListener loadingListener) {
        this.loadingListener = loadingListener;
    }

    /**
     * Opens a Reddit post in a new activity to show comments
     *
     * @param view The view holder of the post clicked
     */
    private void openPost(PostsAdapter.ViewHolder view) {
        Intent intent = new Intent(requireActivity(), PostActivity.class);
        intent.putExtra(PostActivity.POST, new Gson().toJson(view.getRedditPost()));

        Bundle extras = view.getExtraPostInfo();
        intent.putExtra("extras", extras);

        view.pauseVideo();

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view.getPostTransitionViews());
        startActivity(intent, options.toBundle());
    }

    /**
     * For long clicks on a post, copy the post URL
     *
     * @param post The post clicked on
     */
    private void copyLinkToClipboard(RedditPost post) {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", post.getPermalink());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), R.string.linkCopied, Toast.LENGTH_SHORT).show();

        // DEBUG
        Log.d(TAG, "copyLinkToClipboard: " + new GsonBuilder().setPrettyPrinting().create().toJson(post));
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
        binding.posts.addOnChildAttachStateChangeListener(attachStateChangeListener);

        ListDivider divider = new ListDivider(ContextCompat.getDrawable(getContext(), R.drawable.list_divider));
        binding.posts.addItemDecoration(divider);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastLoadAttemptCount = 0;

        adapter = new PostsAdapter();

        // Open post with comments when clicked
        adapter.setOnClickListener(this::openPost);

        // Set long clicks to copy the post link
        adapter.setOnLongClickListener(this::copyLinkToClipboard);

        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        postsViewModel.getPosts().observe(this, adapter::addPosts);
        postsViewModel.onLoadingChange().observe(this, up -> {
            if (loadingListener == null) {
                return;
            }
            loadingListener.onCountChange(up);
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
