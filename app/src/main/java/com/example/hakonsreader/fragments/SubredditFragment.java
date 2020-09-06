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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.interfaces.OnFailure;
import com.example.hakonsreader.interfaces.OnResponse;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;

import java.util.List;

/**
 * Fragment containing a subreddit
 */
public class SubredditFragment extends Fragment {
    private static final String TAG = "PostsFragment";

    // The amount of posts left in the list before attempting to load more posts automatically
    private static final int NUM_REMAINING_POSTS_BEFORE_LOAD = 6;


    private RedditApi redditApi;

    private PostsAdapter adapter;
    private LinearLayoutManager layoutManager;

    private RecyclerView postsList;
    private TextView title;
    private String subreddit;

    // The amount of items in the list at the last attempt at loading more posts
    private int lastLoadAttemptCount;

    /**
     * Indicates that posts wants to be loaded, but the API object is not ready yet
     * <p>When the API is set, posts are automatically loaded</p>
     */
    private boolean wantsToLoad = false;


    // Listener for scrolling in the posts list that automatically tries to load more posts
    private View.OnScrollChangeListener scrollListener = (view, i, i1, i2, i3) -> {
        // Get the last item visible in the current list
        int posLastItem = this.layoutManager.findLastVisibleItemPosition();

        // Load more posts before we reach the end to create an "infinite" list
        if (posLastItem + NUM_REMAINING_POSTS_BEFORE_LOAD > adapter.getItemCount()) {

            // Only load posts if there hasn't been an attempt at loading more posts
            if (this.lastLoadAttemptCount < adapter.getItemCount()) {
                this.loadPosts();
            }
        }
    };

    // Response handler for loading posts
    private OnResponse<RedditPostResponse> onPostResponse = (call, response) -> {
        if (!response.isSuccessful() || response.body() == null) {
            return;
        }

        List<RedditPost> posts = response.body().getPosts();

        adapter.addPosts(posts);
    };
    // Failure handler for loading posts
    private OnFailure<RedditPostResponse> onPostFailure = (call, t) -> {

    };


    /**
     * Creates a new subreddit fragment
     *
     * @param subreddit The name of the subreddit. For front page use an empty string
     */
    public SubredditFragment(String subreddit) {
        this.redditApi = RedditApi.getInstance();

        this.subreddit = subreddit;
        this.adapter = new PostsAdapter();
        this.lastLoadAttemptCount = 0;

        this.adapter.setOnSubredditClickListener(this::openSubredditInActivity);
        this.adapter.setOnLongClickListener(this::onPostLongClickListener);
    }

    /**
     * For long clicks on a reddit post, copy the post URL
     *
     * @param post The post clicked on
     */
    private void onPostLongClickListener(RedditPost post) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("reddit post", post.getPermalink());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), R.string.linkCopied, Toast.LENGTH_SHORT).show();;
    }

    /**
     * Loads more posts. Retrieves posts continuing from the last item in the list
     */
    private void loadPosts() {
        if (this.redditApi == null) {
            this.wantsToLoad = true;

            return;
        }

        // Get the ID of the last post in the list (t3_ signifies to reddit it's posts)
        String after = "";

        List<RedditPost> previousPosts = this.adapter.getPosts();
        if (previousPosts.size() > 0) {
            after = "t3_" + this.adapter.getPosts().get(this.adapter.getItemCount() - 1).getId();
        }

        int count = this.adapter.getItemCount();

        // Store the current attempt to load more posts to avoid attempting many times if it fails
        this.lastLoadAttemptCount = count;

        Log.d(TAG, "Loading more posts, count = " + count + ", last ID = " + after);

        this.redditApi.getSubredditPosts(this.subreddit, after, count, this.onPostResponse, this.onPostFailure);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subreddit, container, false);

        this.postsList = view.findViewById(R.id.posts);
        this.title = view.findViewById(R.id.subredditName);

        this.title.setText((this.subreddit.isEmpty() ? "Front page" : this.subreddit));

        this.layoutManager = new LinearLayoutManager(getActivity());

        view.findViewById(R.id.subredditRefresh).setOnClickListener(v -> {
            // Kinda weird to clear the posts here but works I guess?
            this.adapter.getPosts().clear();

            this.loadPosts();
        });

        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(this.layoutManager);
        this.postsList.setOnScrollChangeListener(this.scrollListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Called when the fragment is visible, so when the fragment is first selected automatically load posts
        if (this.adapter.getPosts().isEmpty()) {
            this.loadPosts();
        }
    }

    /**
     * Opens an activity with the selected subreddit
     *
     * @param subreddit The subreddit to open
     */
    private void openSubredditInActivity(String subreddit) {
        // Don't open another activity if we are already in that subreddit (because honestly why would you)
        if (this.subreddit.equals(subreddit)) {
            return;
        }

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(getActivity(), SubredditActivity.class);
        intent.putExtra("subreddit", subreddit);

        startActivity(intent);

        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
