package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostInfoBinding;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.misc.ViewUtil;

import java.time.Duration;
import java.time.Instant;

/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private static final String TAG = "PostInfo";

    private PostInfoBinding binding;

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
        binding.setPost(post);
    }

    /**
     * Binding adapter for setting the age text on the post. The text is formatted as "2 hours", "1 day" etc.
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"createdAt"})
    public static void setAgeText(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            Instant created = Instant.ofEpochSecond(createdAt);
            Instant now = Instant.now();
            Duration between = Duration.between(created, now);

            textView.setText(Util.createAgeText(textView.getResources(), between));
        }
    }

    /**
     * Creates and adds the tags for the post
     */
    @BindingAdapter({"spoiler", "nsfw", "linkFlair"})
    public static void addTags(ViewGroup view, boolean isSpoiler, boolean isNsfw, RedditPost post) {
        Context context = view.getContext();

        // If this view has been used in a RecyclerView it might still have old views
        view.removeAllViews();

        if (isSpoiler) {
            ViewUtil.addTagWithSpace(view, ViewUtil.createSpoilerTag(context));
        }
        if (isNsfw) {
            ViewUtil.addTagWithSpace(view, ViewUtil.createNsfwTag(context));
        }

        addLinkFlair(view, post);
    }

    /**
     * Adds the link flair of the post
     *
     * @param view The view to add the flair to
     * @param post The post to create the flair for
     */
    public static void addLinkFlair(ViewGroup view, RedditPost post) {
        if (post == null) {
            return;
        }
        Tag tag = ViewUtil.createFlair(
                post.getLinkRichtextFlairs(),
                post.getLinkFlairText(),
                post.getLinkFlairTextColor(),
                post.getLinkFlairBackgroundColor(),
                view.getContext()
        );

        if (tag != null) {
            // The link flair is grouped with spoiler/nsfw as they all convey info about the post
            ViewUtil.addTagWithSpace(view, tag);
        }
    }

    /**
     * Adds the authors flair. If the author has no flair the view is set to {@link View#GONE}
     *
     * @param view The view that holds the author flair
     * @param post The post
     */
    @BindingAdapter("authorFlair")
    public static void addAuthorFlair(FrameLayout view, RedditPost post) {
        if (post == null) {
            return;
        }
        Tag tag = ViewUtil.createFlair(
                post.getAuthorRichtextFlairs(),
                post.getAuthorFlairText(),
                post.getAuthorFlairTextColor(),
                post.getAuthorFlairBackgroundColor(),
                view.getContext()
        );

        if (tag != null) {
            // If this view has been used in a RecyclerView it might still have old views
            view.removeAllViews();
            view.addView(tag);

            view.setVisibility(View.VISIBLE);
        } else {
            // No author flair, remove the view so it doesn't take up space
            view.setVisibility(View.GONE);
        }
    }
}
