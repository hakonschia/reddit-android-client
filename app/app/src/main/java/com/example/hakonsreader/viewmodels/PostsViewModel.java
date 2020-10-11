package com.example.hakonsreader.viewmodels;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.api.persistence.AppDatabase;

import java.util.ArrayList;
import java.util.List;

public class PostsViewModel extends ViewModel {
    private static final String TAG = "PostsViewModel";


    // The way this works is that the database holds all RedditPosts
    // We store the IDs of the post this ViewModel keeps track of, and asks the database for those posts
    // When we retrieve posts we add them to the shared database for all PostsViewModel

    // TODO posts should be removed 1 day (or something) after they have been inserted, so not massive amounts of posts are
    //  stored that wont ever be used

    private AppDatabase database;

    private List<String> postIds = new ArrayList<>();
    private List<RedditPost> postsData = new ArrayList<>();
    private MutableLiveData<List<RedditPost>> posts;
    private MutableLiveData<Boolean> loadingChange;

    public PostsViewModel(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public List<String> getPostIds() {
        return postIds;
    }

    public void setPostIds(List<String> postIds) {
        this.postIds = postIds;
        // TODO get posts and make sure crossposts are set correctly
    }

    /**
     * @return The observable for the posts
     */
    public LiveData<List<RedditPost>> getPosts() {
        if(posts == null) {
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

        if (postIds != null) {
            count = postIds.size();

            if (count > 0) {
                after = Thing.POST.getValue() + "_" + postIds.get(count - 1);
            }
        }

        loadingChange.setValue(true);

        App.get().getApi().getPosts(subreddit, after, count, newPosts -> {
            loadingChange.setValue(false);

            // Store (or update) the posts in the database
            new Thread(() -> {
                for(RedditPost post : newPosts) {
                    // Store which IDs this ViewModel is tracking
                    postIds.add(post.getId());

                    List<RedditPost> crossposts = post.getCrossposts();
                    if (crossposts != null && !crossposts.isEmpty()) {
                        List<String> crosspostIds = new ArrayList<>();

                        // Insert crossposts as their own database record
                        for (RedditPost crosspost : crossposts) {
                            database.posts().insert(crosspost);
                            crosspostIds.add(crosspost.getId());
                        }

                        post.setCrosspostIds(crosspostIds);
                    }
                    database.posts().insert(post);
                }

                postsData.addAll(newPosts);
                posts.postValue(postsData);
            }).start();
        }, (code, t) -> {
            t.printStackTrace();
            loadingChange.setValue(false);
            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }
}
