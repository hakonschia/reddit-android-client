package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.fragments.SubredditFragment;
import com.r0adkll.slidr.Slidr;

/**
 * Activity for a subreddit (used when a subreddit is clicked from a post)
 */
public class SubredditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);

        Bundle data = getIntent().getExtras();
        String subreddit = data.getString("subreddit");

        SubredditFragment fragment = new SubredditFragment(subreddit);

        getSupportFragmentManager().beginTransaction().replace(R.id.subredditActivityFragment, fragment).commit();

        Slidr.attach(this);
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
