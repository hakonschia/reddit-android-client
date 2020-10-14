package com.example.hakonsreader.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectSubredditsViewModel extends ViewModel {

    private AppDatabase database;
    private MutableLiveData<List<Subreddit>> subreddits;


    /**
     * @param context The context to use to create the database for the posts
     */
    public SelectSubredditsViewModel(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public LiveData<List<Subreddit>> getSubreddits() {
        if (subreddits == null) {
            subreddits = new MutableLiveData<>();
        }

        new Thread(() -> {
            subreddits.postValue(database.subreddits().getAll());
        }).start();

        return subreddits;
    }

    /**
     * Load the subreddits. If a user is logged in their subscribed subreddits are loaded, otherwise
     * default subs are loaded
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
        App.get().getApi().getSubreddits("", 0, loaded -> {
            List<Subreddit> sorted = loaded.stream()
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
            new Thread(() -> database.subreddits().insertAll(combined)).start();
        }, (code, t) -> {
            t.printStackTrace();
        });
    }
}
