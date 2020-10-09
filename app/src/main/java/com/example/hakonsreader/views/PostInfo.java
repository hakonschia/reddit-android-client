package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Space;

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
        binding.domain.setText(post.getDomain());
        binding.age.setText(Util.createAgeText(getResources(), between));
        binding.author.setText(authorText);
        binding.title.setText(post.getTitle());

        if (post.isLocked()) {
            binding.lock.setVisibility(VISIBLE);
        }
        if (post.isStickied()) {
            binding.stickied.setVisibility(VISIBLE);
        }

        this.createAndAddTags();

        // When the subreddit is clicked, open the selected subreddit in a new activity
        binding.subreddit.setOnClickListener(view -> openSubredditInActivity(post.getSubreddit()));
    }

    /**
     * Creates and adds the relevant tags
     */
    private void createAndAddTags() {
        // If this view has been used in a RecyclerView make sure it is fresh
        binding.authorFlair.removeAllViews();
        binding.tags.removeAllViews();

        if (post.isSpoiler()) {
            this.addTag(ViewUtil.createSpoilerTag(getContext()));
        }
        if (post.isNsfw()) {
            this.addTag(ViewUtil.createNSFWTag(getContext()));
        }

        this.addAuthorFlair();
        this.addLinkFlair();
    }

    private void addAuthorFlair() {
        Tag tag = ViewUtil.createFlair(post.getAuthorRichtextFlairs(), post.getAuthorFlairText(), post.getAuthorFlairTextColor(), post.getAuthorFlairBackgroundColor(), getContext());
        if (tag != null) {
            binding.authorFlair.addView(tag);
        }
    }

    private void addLinkFlair() {
        Tag tag = ViewUtil.createFlair(post.getLinkRichtextFlairs(), post.getLinkFlairText(), post.getLinkFlairTextColor(), post.getLinkFlairBackgroundColor(), getContext());
        if (tag != null) {
            this.addTag(tag);
        }
    }


    /**
     * Adds a tag to {@link PostInfoBinding#tags} and adds space between
     *
     * @param tag The tag to add
     */
    private void addTag(Tag tag) {
        binding.tags.addView(tag);

        Space space = new Space(getContext());
        space.setMinimumWidth((int)getResources().getDimension(R.dimen.tagSpace));
        binding.tags.addView(space);
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

    /**
     * Formats the post as a mod post
     */
    public void asMod() {
        binding.author.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.comment_by_mod));
        binding.author.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
    }

    /**
     * Resets formatting
     */
    public void reset() {
        // Reset author text (from when comment is by a mod)
        binding.author.setBackground(null);
        binding.author.setTextColor(ContextCompat.getColor(getContext(), R.color.secondaryTextColor));

        binding.lock.setVisibility(GONE);
        binding.stickied.setVisibility(GONE);
    }
}
