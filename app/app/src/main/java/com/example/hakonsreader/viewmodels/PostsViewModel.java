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
    // We store the posts here as well, and have a function to get posts from the database by their ID
    // When our fragment/activity dies, we can get the posts back by storing the IDs

    // TODO posts should be removed 1 day (or something) after they have been inserted, so not massive amounts of posts are
    //  stored that wont ever be used

    private AppDatabase database;

    private List<String> postIds = new ArrayList<>();
    private List<RedditPost> postsData = new ArrayList<>();
    private MutableLiveData<List<RedditPost>> posts;
    private MutableLiveData<Boolean> loadingChange;

    /**
     * @param context The context to use to create the database for the posts
     */
    public PostsViewModel(Context context) {
        database = AppDatabase.getInstance(context);
    }


    /**
     * @return The list of IDs the ViewModel is keeping track of
     */
    public List<String> getPostIds() {
        return postIds;
    }

    /**
     * Set the list of IDs the ViewModel should keep track of.
     *
     * <p>This function automatically retrieves the posts from the local database. Make sure
     * {@link PostsViewModel#getPosts()} is called and is observed before this is called.</p>
     *
     * @param postIds The IDs to track
     */
    public void setPostIds(List<String> postIds) {
        this.postIds = postIds;

        // Retrieve the posts
        new Thread(() -> {
            // The posts are not sorted in the database so they need to be added back in the way
            // they originally were (sorting by a "inserted" field wouldn't work as one post might be
            // in several ViewModels in a different order)
            List<RedditPost> postsFromDb = database.posts().getPostsById(postIds);

            for (String id : postIds) {
                RedditPost post = find(postsFromDb, id);

                if (post != null) {

                    // If the post had crosspost posts, restore them
                    List<String> crosspostIds = post.getCrosspostIds();
                    if (crosspostIds != null && !crosspostIds.isEmpty()) {
                        post.setCrossposts(database.posts().getPostsById(crosspostIds));
                    }
                    postsData.add(post);
                }
            }

            posts.postValue(postsData);
        }).start();
    }

    /**
     * Find a post from a list of posts by a given ID
     *
     * @param posts The posts to look in
     * @param id The ID to look for
     * @return The post, or null if not found
     */
    private RedditPost find(List<RedditPost> posts, String id) {
        for (RedditPost post : posts) {
            if (post.getId().equals(id)) {
                return post;
            }
        }

        return null;
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
        int count = postIds.size();

        if (count > 0) {
            after = Thing.POST.getValue() + "_" + postIds.get(count - 1);
        }

        loadingChange.setValue(true);

        App.get().getApi().getPosts(subreddit, after, count, newPosts -> {
            loadingChange.setValue(false);

            postsData.addAll(newPosts);
            posts.postValue(postsData);

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
            }).start();
        }, (code, t) -> {
            t.printStackTrace();
            loadingChange.setValue(false);
            Util.handleGenericResponseErrors(parentLayout, code, t);
        });
    }
}
