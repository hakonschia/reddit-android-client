package com.example.hakonsreader.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.misc.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectSubredditsViewModel extends ViewModel {

    /**
     * The key used to store the list of subreddit IDs the user is subscribed to
     */
    public static final String SUBSCRIBED_SUBREDDITS_KEY = "subscribedSubreddits";

    private AppDatabase database;
    private MutableLiveData<List<Subreddit>> subreddits;


    /**
     * @param context The context to use to create the database for the posts
     */
    public SelectSubredditsViewModel(Context context) {
        database = AppDatabase.getInstance(context);
    }

    /**
     * Retrieve the LiveData for the list of subreddits.
     *
     * <p>When this is called the first time the list of stored subreddits are set
     * (stored with the key {@link SelectSubredditsViewModel#SUBSCRIBED_SUBREDDITS_KEY}) in SharedPreferences</p>
     *
     * @return The subreddits LiveData
     */
    public LiveData<List<Subreddit>> getSubreddits() {
        if (subreddits == null) {
            subreddits = new MutableLiveData<>();

            // If we have a list of IDs we are tracking stored, load those instantly
            String[] ids = SharedPreferencesManager.get(SUBSCRIBED_SUBREDDITS_KEY, String[].class);
            if (ids != null) {
                new Thread(() -> {
                    subreddits.postValue(database.subreddits().getSubsById(ids));
                }).start();
            }
        }

        return subreddits;
    }

    /**
     * Load the subreddits. If a user is logged in their subscribed subreddits are loaded, otherwise
     * default subs are loaded.
     *
     * <p>The IDs are stored in SharedPreferences with the key {@link SelectSubredditsViewModel#SUBSCRIBED_SUBREDDITS_KEY}</p>
     *
     * <p>The order of the list will be, sorted alphabetically:
     * <ol>
     *     <li>Favorites (for logged in users)</li>
     *     <li>The rest of the subreddits</li>
     *     <li>Users the user is following</li>
     * </ol>
     * </p>
     */
    public void loadSubreddits() {
        App.get().getApi().getSubreddits("", 0, subs -> {
            List<Subreddit> sorted = subs.stream()
                    // Sort based on subreddit name
                    .sorted((first, second) -> first.getName().toLowerCase().compareTo(second.getName().toLowerCase()))
                    .collect(Collectors.toList());

            List<Subreddit> favorites = sorted.stream()
                    .filter(Subreddit::isFavorited)
                    .collect(Collectors.toList());

            List<Subreddit> users = sorted.stream()
                    .filter(subreddit -> subreddit.getSubredditType().equals("user"))
                    .collect(Collectors.toList());

            // Remove the favorites to not include twice
            sorted.removeAll(favorites);
            sorted.removeAll(users);

            List<Subreddit> combined = new ArrayList<>();
            combined.addAll(favorites);
            combined.addAll(sorted);
            combined.addAll(users);

            subreddits.setValue(combined);

            String[] ids = new String[subs.size()];
            for (int i = 0; i < subs.size(); i++) {
                ids[i] = subs.get(i).getId();
            }

            // Although NSFW subs might be inserted with this, it's fine as if the user
            // has subscribed to them it's fine (for non-logged in users, default subs don't include NSFW)
            new Thread(() -> database.subreddits().insertAll(combined)).start();

            SharedPreferencesManager.put(SUBSCRIBED_SUBREDDITS_KEY, ids);
        }, (code, t) -> {
            t.printStackTrace();
        });
    }
}
