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

    private RedditApi redditApi;

    private PostsAdapter adapter;

    private RecyclerView postsList;
    private TextView title;
    private String subreddit;

    
    // Paging variables (where to load posts from etc)
    private int before = 0;
    private int after = 0;

    /**
     * Indicates that posts wants to be loaded, but the API object is not ready yet
     * <p>When the API is set, posts are automatically loaded</p>
     */
    private boolean wantsToLoad = false;


    /**
     * Creates a new subreddit fragment
     *
     * @param subreddit The name of the subreddit. For front page use an empty string
     */
    public SubredditFragment(String subreddit) {
        this.subreddit = subreddit;
        this.adapter = new PostsAdapter();
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

    private void loadPosts() {
        if (this.redditApi == null) {
            this.wantsToLoad = true;

            return;
        }
        Log.d(TAG, "loadPosts: " + this.redditApi);

        this.redditApi.getSubredditPosts(this.subreddit).enqueue(new Callback<RedditPostResponse>() {
            @Override
            public void onResponse(Call<RedditPostResponse> call, Response<RedditPostResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                List<RedditPost> posts = response.body().getPosts();

                adapter.setPosts(posts);
            }

            @Override
            public void onFailure(Call<RedditPostResponse> call, Throwable t) {

            }
        });
        // Load more posts :-d

        // Set posts on adapter etc etc
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        Log.d(TAG, "setArguments: xdxdxdxd");
        if (args == null) {
            return;
        }
        Log.d(TAG, "setArguments: xdxdxdxd2313123123");

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

        this.postsList.setAdapter(this.adapter);
        this.postsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }
}
