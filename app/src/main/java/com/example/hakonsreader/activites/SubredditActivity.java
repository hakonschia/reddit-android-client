package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.example.hakonsreader.interfaces.ItemLoadingListener;
import com.example.hakonsreader.views.LoadingIcon;
import com.r0adkll.slidr.Slidr;

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
public class SubredditActivity extends AppCompatActivity implements ItemLoadingListener {
    private static final String TAG = "SubredditActivity";
    
    private LoadingIcon loadingIcon;
    private SubredditFragment fragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);

        this.loadingIcon = findViewById(R.id.loadingIcon);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: an instance was saved");
           // fragment = (SubredditFragment) getSupportFragmentManager().getFragment(savedInstanceState, "subredditFragment");
            fragment = (SubredditFragment) getSupportFragmentManager().findFragmentByTag("subredditFragment");
        } else {
            fragment = SubredditFragment.newInstance("GlobalOffensive");
            getSupportFragmentManager().beginTransaction().add(R.id.subredditActivityFragment, fragment, "subredditFragment").commit();
        }
        
        fragment.setLoadingListener(this);

        String subreddit;
        /*

        Bundle data = getIntent().getExtras();
        // Activity started from URL intent
        if (data == null) {
            Uri uri = getIntent().getData();

            // First path segment is "/r/", second is the subreddit
            subreddit = uri.getPathSegments().get(1);
        } else {
            // Activity started from manual intent in app
            subreddit = data.getString("subreddit");
        }
         */


        Slidr.attach(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: ");

        getSupportFragmentManager().putFragment(outState, "subredditFragment", fragment);
    }

    @Override
    public void finish() {
        super.finish();

        // Slide the activity out
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onCountChange(boolean up) {
        if (up) {
            this.loadingIcon.increaseLoadCount();
        } else {
            this.loadingIcon.decreaseLoadCount();
        }
    }
}
