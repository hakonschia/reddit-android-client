package com.example.hakonsreader.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditPost;

import java.util.ArrayList;
import java.util.List;

public class PostsViewModel extends ViewModel {
    private static final String TAG = "PostsViewModel";
    
    private MutableLiveData<List<RedditPost>> posts;

    public LiveData<List<RedditPost>> getPosts() {
        if (posts == null) {
            posts = new MutableLiveData<>();
            posts.setValue(new ArrayList<>());
        }

        return posts;
    }

    public void loadPosts(String subreddit) {
        // Get the ID of the last post in the list
        String after = "";
        int count = 0;

        List<RedditPost> previousPosts = posts.getValue();

        if (previousPosts != null) {
            count = previousPosts.size();

            if (count > 0) {
                after = Thing.POST.getValue() + "_" + previousPosts.get(count - 1).getID();
            }
        }

        App.getApi().getPosts(subreddit, after, count, newPosts -> {
            if (previousPosts != null) {
                previousPosts.addAll(newPosts);
                posts.setValue(previousPosts);
            } else {
                posts.setValue(newPosts);
            }
        }, (code, t) -> {
            t.printStackTrace();
        });
    }

}
