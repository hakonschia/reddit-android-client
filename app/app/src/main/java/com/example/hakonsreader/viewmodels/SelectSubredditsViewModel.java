package com.example.hakonsreader.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.Subreddit;
import com.example.hakonsreader.api.persistence.AppDatabase;
import com.example.hakonsreader.misc.SharedPreferencesManager;

import java.util.List;

public class SelectSubredditsViewModel extends ViewModel {

    /**
     * The key used to store the list of subreddit IDs the user is subscribed to
     */
    public static final String SUBSCRIBED_SUBREDDITS_KEY = "subscribedSubreddits";

    private final AppDatabase database;
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
     * <p>The list returned is not sorted</p>
     *
     * <p>The IDs are stored in SharedPreferences with the key {@link SelectSubredditsViewModel#SUBSCRIBED_SUBREDDITS_KEY}</p>
     */
    public void loadSubreddits() {
        App.get().getApi().subreddits().getSubreddits("", 0, subs -> {
            subreddits.setValue(subs);

            String[] ids = new String[subs.size()];
            for (int i = 0; i < subs.size(); i++) {
                ids[i] = subs.get(i).getId();
            }

            // Although NSFW subs might be inserted with this, it's fine as if the user
            // has subscribed to them it's fine (for non-logged in users, default subs don't include NSFW)
            new Thread(() -> database.subreddits().insertAll(subs)).start();

            SharedPreferencesManager.put(SUBSCRIBED_SUBREDDITS_KEY, ids);
        }, (e, t) -> {
            t.printStackTrace();
        });
    }
}
