package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.activites.MainActivity;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.databinding.PostInfoBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.misc.ViewUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private static final String TAG = "PostInfo";
    
    private PostInfoBinding binding;
    private RedditPost post;

    
    public PostInfo(@NonNull Context context) {
        super(context);
        binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public PostInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public PostInfo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public PostInfo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true);
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

        binding.subreddit.setText(subredditText);
        binding.author.setText(authorText);
        binding.age.setText(Util.createAgeText(getResources(), between));
        binding.title.setText(post.getTitle());

        this.createAndAddTags();

        // When the subreddit is clicked, open the selected subreddit in a new activity
        binding.subreddit.setOnClickListener(view -> openSubredditInActivity(post.getSubreddit()));
    }

    /**
     * Creates and adds the relevant tags
     */
    private void createAndAddTags() {
        // If this view has been used in a RecyclerView make sure it is fresh
        binding.tags.removeAllViews();

        if (true) {
            binding.tags.addView(ViewUtil.createSpoilerTag(getContext()));
        }
        if (post.isNSFW()) {
            binding.tags.addView(ViewUtil.createNSFWTag(getContext()));
        }

        List<RichtextFlair> flairs = post.getLinkRichtextFlairs();
        flairs.forEach(flair -> {
            Tag tag = new Tag(getContext());
            tag.setText(flair.getText());
            // TODO this shouldn't be hardcoded like this
            // TODO each flair item is different types of items in the flair (such as an icon and text)

            if (post.getLinkFlairTextColor().equals("dark")) {
                tag.setTextColor(ContextCompat.getColor(getContext(), R.color.flairTextDark));
                tag.setFillColor(ContextCompat.getColor(getContext(), R.color.flairBackgroundDark));
            }
            // tag.setFillColor(post.getLinkFlairBackgroundColor());
            binding.tags.addView(tag);
        });
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
