package com.example.hakonsreader.viewmodels;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.persistence.AppDatabase;

import java.util.ArrayList;
import java.util.List;

public class PostsViewModel extends ViewModel {
    private static final String TAG = "PostsViewModel";


    // The way this works is that the database holds all RedditPosts
    // We store the IDs of the post this ViewModel keeps track of, and asks the database for those posts
    // When we retrieve posts we add them to the shared database for all PostsViewModel

    private AppDatabase database;

    private List<String> postIds = new ArrayList<>();
    private MutableLiveData<Boolean> loadingChange;

    public PostsViewModel(Context context) {
        database = AppDatabase.getInstance(context);
    }

    /**
     * @return The observable for the posts
     */
    public LiveData<List<RedditPost>> getPosts() {
        return database.listingDao().getPostsById(postIds);
    }

    /**
     * Retrieve the value used for listening to when something has started or finished loading
     *
     * @return If something has started loading the value in this LiveData will be set to true, and when
     * it has finished loading it will be set to false
     */
    public LiveData<Boolean> onLoadingChange() {
        if (loadingChange == null) {
            loadingChange = new MutableLiveData<>();
        }

        return loadingChange;
    }


    public void loadPosts(View parentLayout, String subreddit) {
        // Get the ID of the last post in the list
        String after = "";
        int count = 0;

        if (postIds != null) {
            count = postIds.size();

            if (count > 0) {
                after = Thing.POST.getValue() + "_" + postIds.get(count - 1);
            }
        }

        loadingChange.setValue(true);
        // TODO this creates issues as the posts currently in the list gets refreshed, causing videos to restart
        App.get().getApi().getPosts(subreddit, after, count, newPosts -> {
            loadingChange.setValue(false);

            // Store which IDs this ViewModel is tracking
            newPosts.forEach(post -> postIds.add(post.getId()));

            // Store (or update) the posts in the database
            new Thread(() -> {
                database.listingDao().insertAll(newPosts);
            }).start();
        }, (code, t) -> {
            loadingChange.setValue(false);
            t.printStackTrace();

            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }

}
