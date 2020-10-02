package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

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
import com.squareup.picasso.Picasso;

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
        binding.tags.removeAllViews();

        if (post.isSpoiler()) {
            binding.tags.addView(ViewUtil.createSpoilerTag(getContext()));
        }
        if (post.isNSFW()) {
            binding.tags.addView(ViewUtil.createNSFWTag(getContext()));
        }

        Tag tag = new Tag(getContext());

        int textColor;
        if (post.getLinkFlairTextColor().equals("dark")) {
            textColor = ContextCompat.getColor(getContext(), R.color.flairTextDark);
            tag.setFillColor(ContextCompat.getColor(getContext(), R.color.flairBackgroundDark));
        } else {
            textColor = ContextCompat.getColor(getContext(), R.color.flairTextLight);
            tag.setFillColor(ContextCompat.getColor(getContext(), R.color.flairBackgroundLight));
        }

        String fillColor = post.getLinkFlairBackgroundColor();
        if (fillColor != null && !fillColor.isEmpty()) {
            tag.setFillColor(fillColor);
        }

        List<RichtextFlair> flairs = post.getLinkRichtextFlairs();

        // If no richtext flairs, try to see if there is a text flair
        // Apparently some subs set both text and richtext flairs *cough* GlobalOffensive *cough*
        // so make sure not both are added
        if (flairs.isEmpty()) {
            String flairText = post.getLinkFlairText();
            if (flairText != null && !flairText.isEmpty()) {
                TextView tv = new TextView(getContext());
                tv.setText(flairText);
                tv.setTextColor(textColor);
                tv.setTextSize(getContext().getResources().getDimension(R.dimen.tagTextSize));
                tag.add(tv);
            }
        } else {
            // Add all views the flair has
            flairs.forEach(flair -> {
                View view = null;

                if (flair.getType().equals("text")) {
                    TextView tv = new TextView(getContext());
                    tv.setText(flair.getText());
                    tv.setTextColor(textColor);
                    tv.setTextSize(getContext().getResources().getDimension(R.dimen.tagTextSize));
                    view = tv;
                } else if (flair.getType().equals("emoji")) {
                    int size = (int)getContext().getResources().getDimension(R.dimen.tagIconSize);
                    ImageView iv = new ImageView(getContext());
                    Picasso.get()
                            .load(flair.getUrl())
                            .resize(size, size)
                            .into(iv);
                    view = iv;
                }

                // Don't know if there are more than "text" and "emoji" types, so this will do for now
                if (view != null) {
                    tag.add(view);
                }
            });
        }

        binding.tags.addView(tag);
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
    }
}
