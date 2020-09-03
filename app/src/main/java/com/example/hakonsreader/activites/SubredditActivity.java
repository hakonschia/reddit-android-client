package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.SubredditFragment;

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
public class SubredditActivity extends AppCompatActivity {

    SubredditFragment fragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);

        Bundle data = getIntent().getExtras();
        String subreddit = data.getString("subreddit");

        this.fragment = new SubredditFragment(subreddit);
        this.fragment.setArguments(data);

        getSupportFragmentManager().beginTransaction().replace(R.id.subredditActivityFragment, this.fragment).commit();

        // The fragment needs to be manually called the first time (when no swipe has happened)
        this.fragment.onFragmentSelected();
    }
}
