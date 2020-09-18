package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.activites.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.LayoutPostInfoBinding;
import com.example.hakonsreader.misc.Util;

import java.time.Duration;
import java.time.Instant;

/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private LayoutPostInfoBinding binding;

    private RedditPost post;


    public PostInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.binding = LayoutPostInfoBinding.inflate(inflater, this, true);
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

        this.binding.subreddit.setText(subredditText);
        this.binding.author.setText(authorText);
        this.binding.age.setText(Util.createAgeText(getResources(), between));
        this.binding.title.setText(post.getTitle());

        // When the subreddit is clicked, open the selected subreddit in a new activity
        this.binding.subreddit.setOnClickListener(view -> openSubredditInActivity(post.getSubreddit()));
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
