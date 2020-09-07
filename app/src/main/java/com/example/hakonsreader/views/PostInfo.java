package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;

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

        String time;

        Duration between = Duration.between(created, now);

        // This is kinda bad but whatever
        if (between.toDays() > 0) {
            time = String.format(context.getString(R.string.post_age_days), between.toDays());
        } else if (between.toHours() > 0) {
            time = String.format(context.getString(R.string.post_age_hours), between.toHours());
        } else {
            time = String.format(context.getString(R.string.post_age_minutes), between.toMinutes());
        }

        String subredditText = String.format(context.getString(R.string.subredditPrefixed), post.getSubreddit());
        String authorText = String.format(context.getString(R.string.authorPrefixed), post.getAuthor());

        subreddit.setText(subredditText);
        author.setText(authorText);
        age.setText(time);
        title.setText(post.getTitle());
    }
}
