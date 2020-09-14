package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;

import java.time.Duration;
import java.time.Instant;

/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private TextView subreddit;
    private TextView author;
    private TextView age;
    private TextView title;

    private RedditPost post;

    public PostInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.layout_post_info, this);

        this.subreddit = findViewById(R.id.post_info_subreddit);
        this.author = findViewById(R.id.post_info_author);
        this.age = findViewById(R.id.post_info_age);
        this.title = findViewById(R.id.post_info_title);
    }

    /**
     * Sets the post to use in this VoteBar and sets the initial state of the vote status
     *
     * @param post The post to set
     */
    public void setPost(@NonNull RedditPost post) {
        this.post = post;

        // Make sure the initial status is up to date
        this.updateInfo();
    }

    /**
     * Updates the information based on the post set
     */
    private void updateInfo() {
        Context context = getContext();

        Instant created = Instant.ofEpochSecond(post.getCreatedAt());
        Instant now = Instant.now();

        Duration between = Duration.between(created, now);

        String subredditText = String.format(context.getString(R.string.subredditPrefixed), post.getSubreddit());
        String authorText = String.format(context.getString(R.string.authorPrefixed), post.getAuthor());

        subreddit.setText(subredditText);
        author.setText(authorText);
        age.setText(Util.createAgeText(getResources(), between));
        title.setText(post.getTitle());

        // When the subreddit is clicked, open the selected subreddit in a new activity
        subreddit.setOnClickListener(view -> openSubredditInActivity(post.getSubreddit()));
    }

    /**
     * Opens an activity with the selected subreddit
     *
     * @param subreddit The subreddit to open
     */
    private void openSubredditInActivity(String subreddit) {
        Activity activity = (Activity)getContext();

        // TODO find a better way to do this (to not open the sub if we are in the sub before
        //  this works because we are always in MainActivity when we're at a place where it makes sense
        //  to be able to click on a subreddit to open it.
        //  Opening a post from PostActivity doesnt work
        // Don't open another activity if we are already in that subreddit (because honestly why would you)
        if (!(activity instanceof MainActivity)) {
            return;
        }

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(getContext(), SubredditActivity.class);
        intent.putExtra("subreddit", subreddit);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
