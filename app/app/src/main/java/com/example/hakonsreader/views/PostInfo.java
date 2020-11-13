package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostInfoBinding;
import com.example.hakonsreader.views.util.ViewUtil;


/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private static final String TAG = "PostInfo";

    private PostInfoBinding binding;

    public PostInfo(@NonNull Context context) {
        this(context, null, 0, 0);
    }
    public PostInfo(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public PostInfo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
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
