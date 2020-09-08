package com.example.hakonsreader.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.PostActivity;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.google.gson.Gson;

import java.util.List;

/**
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "SubredditFragment";

    // The amount of posts left in the list before attempting to load more posts automatically
    private static final int NUM_REMAINING_POSTS_BEFORE_LOAD = 6;


    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private String subreddit;

    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView postsList;

    private ItemLoadingListener loadingListener;


    // The amount of items in the list at the last attempt at loading more posts
    private int lastLoadAttemptCount;


    // Listener for scrolling in the posts list that automatically tries to load more posts
    private RecyclerView.OnScrollChangeListener scrollListener = (view, i, i1, i2, i3) -> {
        // Get the last item visible in the current list
        int posLastItem = this.layoutManager.findLastVisibleItemPosition();
        int listSize = this.adapter.getItemCount();

        // Load more posts before we reach the end to create an "infinite" list
        if (posLastItem + NUM_REMAINING_POSTS_BEFORE_LOAD > listSize) {

            // Only load posts if there hasn't been an attempt at loading more posts
            if (this.lastLoadAttemptCount < listSize) {
                this.loadPosts();
            }
        }
    };

    // Response handler for loading posts
    private OnResponse<RedditPostResponse> onPostResponse = (call, response) -> {
        this.decreaseLoadingCount();

        if (!response.isSuccessful() || response.body() == null) {
            return;
        }

        List<RedditPost> posts = response.body().getPosts();

        adapter.addPosts(posts);
    };
    // Failure handler for loading posts
    private OnFailure<RedditPostResponse> onPostFailure = (call, t) -> {
        this.decreaseLoadingCount();
    };


    /**
     * Creates a new subreddit fragment
     *
     * @param subreddit The name of the subreddit. For front page use an empty string
     */
    public SubredditFragment(String subreddit) {
        this.subreddit = subreddit;
        this.adapter = new PostsAdapter();
        this.lastLoadAttemptCount = 0;

        // Open post with comments when clicked
        this.adapter.setOnClickListener(this::openPost);

        // Set long clicks to copy the post link
        this.adapter.setOnLongClickListener(this::copyLinkToClipboard);
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
        intent.putExtra("post", new Gson().toJson(view.getPost()));

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view.getPostTransitionViews());

        startActivity(intent, options.toBundle());
    }

    /**
     * For long clicks on a Reddit post, copy the post URL
     *
     * @param post The post clicked on
     */
    private void copyLinkToClipboard(RedditPost post) {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", post.getPermalink());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), R.string.linkCopied, Toast.LENGTH_SHORT).show();;
    }

    /**
     * Loads more posts. Retrieves posts continuing from the last item in the list
     */
    private void loadPosts() {
        if (this.redditApi == null) {
            return;
        }

        // Get the ID of the last post in the list
        String after = "";

        List<RedditPost> previousPosts = this.adapter.getPosts();
        int postsSize = previousPosts.size();

        if (postsSize > 0) {
            after = RedditApi.Thing.Post.getValue() + previousPosts.get(postsSize - 1).getId();
        }

        // Store the current attempt to load more posts to avoid attempting many times if it fails
        this.lastLoadAttemptCount = postsSize;

        this.increaseLoadingCount();
        this.redditApi.getSubredditPosts(this.subreddit, after, postsSize, this.onPostResponse, this.onPostFailure);
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
        this.postsList.scrollToPosition(0);

        this.loadPosts();
    }

    /**
     * Sets up {@link SubredditFragment#postsList}
     */
    private void setupPostsList(View view) {
        this.layoutManager = new LinearLayoutManager(getActivity());

        this.postsList = view.findViewById(R.id.posts);
        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(this.layoutManager);
        this.postsList.setOnScrollChangeListener(this.scrollListener);
    }

    private void increaseLoadingCount() {
        if (this.loadingListener != null) {
            this.loadingListener.onCountChange(true);
        }
    }

    private void decreaseLoadingCount() {
        if (this.loadingListener != null) {
            this.loadingListener.onCountChange(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subreddit, container, false);

        // Set title in toolbar
        TextView title = view.findViewById(R.id.subredditName);
        title.setText((this.subreddit.isEmpty() ? "Front page" : "r/" + this.subreddit));

        // Bind the refresh button in the toolbar
        view.findViewById(R.id.subredditRefresh).setOnClickListener(this::onRefreshPostsClicked);

        // Setup the RecyclerView posts list
        this.setupPostsList(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the fragment is selected without any posts load posts automatically
        if (this.adapter.getPosts().isEmpty()) {
            this.loadPosts();
        }
    }
}
