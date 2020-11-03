package com.example.hakonsreader.activites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.interfaces.SlidrActivity;
import com.example.hakonsreader.views.LoadingIcon;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
public class SubredditActivity extends AppCompatActivity implements SlidrActivity {
    private static final String TAG = "SubredditActivity";

    /**
     * The key used to save the subreddit fagment
     */
    private static final String SAVED_SUBREDDIT = "subredditFragment";

    /**
     * The key used to transfer data about which subreddit the activity is for
     */
    public static final String SUBREDDIT_KEY = "subreddit";


    private LoadingIcon loadingIcon;
    private SubredditFragment fragment;
    private SlidrInterface slidrInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        slidrInterface = Slidr.attach(this);

        this.loadingIcon = findViewById(R.id.loadingIcon);

        // Restore the fragment if possible
        if (savedInstanceState != null) {
            fragment = (SubredditFragment) getSupportFragmentManager().findFragmentByTag(SAVED_SUBREDDIT);
        } else {
            String subreddit = "";

            Intent intent = getIntent();
            Uri uri = intent.getData();

            // Activity started from URL intent
            if (uri != null) {

                // First path segment is "/r/", second is the subreddit
                subreddit = uri.getPathSegments().get(1);
            } else {
                // Activity started from manual intent in app
                Bundle data = intent.getExtras();
                if (data != null) {
                    subreddit = data.getString(SUBREDDIT_KEY);
                }
            }

            // For testing purposes hardcode a subreddit
            //subreddit = "sports";

            fragment = SubredditFragment.newInstance(subreddit);
            getSupportFragmentManager().beginTransaction().add(R.id.subredditActivityFragment, fragment, SAVED_SUBREDDIT).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        getSupportFragmentManager().putFragment(outState, SAVED_SUBREDDIT, fragment);
    }

    @Override
    public void finish() {
        super.finish();

        // Slide the activity out
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void lock(boolean lock) {
        if (lock) {
            slidrInterface.lock();
        } else {
            slidrInterface.unlock();
        }
    }

    /**
     * @return The name of the subreddit the activity is displaying
     */
    public String getSubredditName() {
        return fragment.getSubredditName();
    }
}
