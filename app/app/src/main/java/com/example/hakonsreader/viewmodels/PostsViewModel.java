package com.example.hakonsreader.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.SortingMethods;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.interfaces.OnFailure;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.persistence.AppDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel holding reddit posts
 */
public class PostsViewModel extends ViewModel {
    private static final String TAG = "PostsViewModel";


    // This is pretty weird I realise now, should probably just remove the database stuff if I bother at one point

    // The way this works is that the database holds all RedditPosts
    // We store the posts here as well, and have a function to get posts from the database by their ID
    // When our fragment/activity dies, we can get the posts back by storing the IDs


    private final AppDatabase database;
    private final RedditApi api = App.get().getApi();

    private List<String> postIds = new ArrayList<>();
    private final List<RedditPost> postsData = new ArrayList<>();
    private final MutableLiveData<List<RedditPost>> posts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingChange = new MutableLiveData<>();
    private final MutableLiveData<ErrorWrapper> error = new MutableLiveData<>();

    private String userOrSubreddit;
    private final boolean isUser;

    private SortingMethods sort;
    private PostTimeSort timeSort;

    /**
     * Handler for successful responses for getting posts
     */
    private final OnResponse<List<RedditPost>> onPostsResponse = this::onPostsRetrieved;
    /**
     * Handler for failed responses for getting posts
     */
    private final OnFailure onPostsFailure = (e, t) -> {
        loadingChange.setValue(false);
        error.setValue(new ErrorWrapper(e, t));
    };


    /**
     * @param context The context to use to create the database for the posts
     */
    public PostsViewModel(Context context, String userOrSubreddit, boolean isUser) {
        this.database = AppDatabase.getInstance(context);
        this.userOrSubreddit = userOrSubreddit;
        this.isUser = isUser;
    }


    /**
     * Update which which subreddit or user this ViewModel is for. Use this with caution, ensure
     * that if the ViewModel was for a user that the updated name is also a user and not a subreddit,
     * and vice versa.
     *
     * @param userOrSubreddit THe new user or subreddit
     */
    public void setUserOrSubreddit(String userOrSubreddit) {
        this.userOrSubreddit = userOrSubreddit;
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
        return posts;
    }

    /**
     * Retrieve the value used for listening to when something has started or finished loading
     *
     * @return If something has started loading the value in this LiveData will be set to true, and when
     * it has finished loading it will be set to false
     */
    public LiveData<Boolean> onLoadingChange() {
        // TODO this causes issues as if the observer is in a paused state they wont receive the update
        //  which can cause the loading icon to stay even if it should be gone
        //  using observeForever might work, is a poor solution as it relies on the user reading the documentation completely
        return loadingChange;
    }

    /**
     * Retrieve a LiveData object that will hold errors from the API
     *
     * @return An observable LiveData that will be updated when API calls return an error
     */
    public LiveData<ErrorWrapper> getError() {
        return error;
    }

    /**
     * Starts posts from the start. This will automatically call {@link PostsViewModel#loadPosts()}.
     * If {@link PostsViewModel#restart(SortingMethods, PostTimeSort)} has been called previously, the same
     * sorting is used this time
     */
    public void restart() {
        postsData.clear();
        postIds.clear();

        // Notify that the list is now empty
        posts.setValue(postsData);

        loadPosts();
    }

    /**
     * Starts posts from start and updates how to sort the posts
     *
     * @param sort How to sort the posts
     * @param timeSort How to sort the posts based on time. Note this isn't applicable for all types
     *                 of sorts. If not applicable this will be ignored (and can be null)
     */
    public void restart(SortingMethods sort, PostTimeSort timeSort) {
        this.sort = sort;
        this.timeSort = timeSort;

        restart();
    }

    /**
     * Retrieve posts from the user or subreddit. Calling this automatically resumes from
     * the previous posts loaded.
     *
     * <p>For users the default sort is {@link SortingMethods#NEW}, for subreddits the default is {@link SortingMethods#HOT}.
     * Use {@link PostsViewModel#restart(SortingMethods, PostTimeSort)} to change the sorting method</p>
     */
    public void loadPosts() {
        // Usernames can be null (if logged in user), subreddits cannot be null (TODO treat null as front page?)
        if (userOrSubreddit == null && !isUser) {
            return;
        }

        // Get the ID of the last post in the list
        String after = "";
        int count = postIds.size();

        if (count > 0) {
            after = Thing.POST.getValue() + "_" + postIds.get(count - 1);
        }

        loadingChange.setValue(true);

        if (isUser) {
            loadForUsers();
        } else {
            loadForSubreddits(after, count);
        }
    }

    /**
     * Loads posts for users
     */
    private void loadForUsers() {
        // TODO include after/count
        // Default for users is "new"

        if (sort == SortingMethods.HOT) {
            api.user(userOrSubreddit).posts().hot(onPostsResponse, onPostsFailure);
        } else if (sort == SortingMethods.TOP) {
            api.user(userOrSubreddit).posts().top(timeSort, onPostsResponse, onPostsFailure);
        } else if (sort == SortingMethods.CONTROVERSIAL) {
            api.user(userOrSubreddit).posts().controversial(timeSort, onPostsResponse, onPostsFailure);
        } else {
            api.user(userOrSubreddit).posts(onPostsResponse, onPostsFailure);
        }
    }

    /**
     * Loads posts for subreddits
     *
     * @param after The ID of the last post loaded (where to load new posts from)
     * @param count The amount of posts loaded
     */
    private void loadForSubreddits(String after, int count) {
        // Default for users is "hot"
        // Since the "else" is used for the default, this will ensure the default sort is always loaded

        if (sort == SortingMethods.NEW) {
            api.subreddit(userOrSubreddit).posts().newPosts(after, count, onPostsResponse, onPostsFailure);
        } else if (sort == SortingMethods.TOP) {
            api.subreddit(userOrSubreddit).posts().top(timeSort, after, count, onPostsResponse, onPostsFailure);
        } else if (sort == SortingMethods.CONTROVERSIAL) {
            api.subreddit(userOrSubreddit).posts().controversial(timeSort, after, count, onPostsResponse, onPostsFailure);
        } else {
            api.subreddit(userOrSubreddit).posts(after, count, onPostsResponse, onPostsFailure);
        }
    }

    /**
     * Function to deal with responses for new posts. The posts are inserted into the local database
     * and are notified to the observers of the LiveData
     *
     * @param newPosts The new posts retrieved
     */
    private void onPostsRetrieved(List<RedditPost> newPosts) {
        loadingChange.setValue(false);

        postsData.addAll(newPosts);
        posts.postValue(postsData);

        // Store (or update) the posts in the database
        new Thread(() -> {
            for (RedditPost post : newPosts) {
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
    }
}
