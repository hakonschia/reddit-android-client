package com.example.hakonsreader.viewmodels;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;

import java.util.List;

public class PostsViewModel extends ViewModel {
    private static final String TAG = "PostsViewModel";

    private MutableLiveData<List<RedditPost>> posts;
    private MutableLiveData<Boolean> loadingChange;

    /**
     * @return The observable for the posts
     */
    public LiveData<List<RedditPost>> getPosts() {
        if (posts == null) {
            posts = new MutableLiveData<>();
        }

        return posts;
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

        List<RedditPost> previousPosts = posts.getValue();

        if (previousPosts != null) {
            count = previousPosts.size();

            if (count > 0) {
                after =  previousPosts.get(count - 1).getFullname();
            }
        }

        loadingChange.setValue(true);
        App.get().getApi().getPosts(subreddit, after, count, newPosts -> {
            posts.setValue(newPosts);
            loadingChange.setValue(false);
        }, (code, t) -> {
            loadingChange.setValue(false);
            t.printStackTrace();

            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }

}
