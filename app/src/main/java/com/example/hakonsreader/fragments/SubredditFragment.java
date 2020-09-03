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
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.AccessToken;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditPostResponse;
import com.example.hakonsreader.constants.SharedPreferencesConstants;
import com.example.hakonsreader.recyclerviewadapters.PostsAdapter;
import com.google.gson.Gson;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private Callback<RedditPostResponse> onPostResponse = new Callback<RedditPostResponse>() {
        @Override
        public void onResponse(Call<RedditPostResponse> call, Response<RedditPostResponse> response) {
            if (!response.isSuccessful() || response.body() == null) {
                return;
            }

            List<RedditPost> posts = response.body().getPosts();

            adapter.addPosts(posts);
        }

        @Override
        public void onFailure(Call<RedditPostResponse> call, Throwable t) {

        }
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
    }


    /**
     * Called when the fragment has been selected.
     * <p>If this is the first time the fragment is selected, posts are loaded</p>
     */
    public void onFragmentSelected() {
        // TODO If no posts are loaded, load from start. Otherwise do nothing really I guess

        // Starting from scratch
        if (this.adapter.getPosts().isEmpty()) {
            this.loadPosts();
        }
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

        this.redditApi.getSubredditPosts(this.subreddit, after, count).enqueue(this.onPostResponse);
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args == null) {
            return;
        }

        Gson gson = new Gson();
        AccessToken accessToken = gson.fromJson(args.getString(SharedPreferencesConstants.ACCESS_TOKEN, ""), AccessToken.class);

        this.redditApi = new RedditApi(accessToken);

        // Load posts now that we have a valid API object
        if (this.wantsToLoad) {
            this.loadPosts();

            this.wantsToLoad = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subreddit, container, false);

        this.postsList = view.findViewById(R.id.posts);
        this.title = view.findViewById(R.id.subredditName);

        this.title.setText((this.subreddit.isEmpty() ? "Front page" : this.subreddit));

        this.layoutManager = new LinearLayoutManager(getActivity());

        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(this.layoutManager);
        this.postsList.setOnScrollChangeListener(this.scrollListener);

        return view;
    }
}
