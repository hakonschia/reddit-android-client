package com.example.hakonsreader.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Settings;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Generic superclass for a view displaying Reddit content
 *
 * This class is built around the idea of recycling views. Calling {@link #setRedditPost(RedditPost)}
 * will automatically update the content view. When the view is no longer needed you should call
 * {@link #recycle()} to ensure that any potential resources are freed up and is no longer connected
 * to a post
 */
@AndroidEntryPoint
public abstract class Content extends FrameLayout {

    /**
     * The key that should be used to send extras about posts across activities
     * The value stored with this key should be the {@link Bundle} retrieved with {@link Content#getExtras()}
     */
    public static final String EXTRAS = "extras";

    public static final String EXTRAS_POST_ID = "extras_postId";


    /**
     * The post the content is for. Children should use {@link #setRedditPost(RedditPost)} if the goal
     * is to update the view automatically.
     */
    protected RedditPost redditPost;

    /**
     * A list of the previous posts this view has displayed
     */
    private final List<RedditPost> previousPosts = new ArrayList<>();


    /**
     * If true, the content of {@link #redditPost} should be cached
     */
    protected boolean cache;

    /**
     * A bundle of extras. This will by default always be empty and it is up to subclasses
     * to provide data for this
     */
    @NonNull
    protected Bundle extras = new Bundle();

    @Nullable
    protected Bitmap bitmap;

    @Inject
    Settings settings;


    public Content(Context context) {
        super(context);
    }
    public Content(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public Content(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public Content(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    /**
     * Retrieves extra information for the content
     *
     * @return A Bundle of extras. This might be empty
     */
    @NonNull
    public Bundle getExtras() {
        if (redditPost != null) {
            extras.putString(EXTRAS_POST_ID, redditPost.getId());
        }
        return extras;
    }

    /**
     * Sets the extras for the content
     *
     * @param extras The extras to set
     */
    @CallSuper
    public void setExtras(@NonNull Bundle extras) {
        this.extras = extras;
    }


    /**
     * @return The post this view is displaying
     */
    @Nullable
    public RedditPost getRedditPost() {
        return redditPost;
    }

    /**
     * Sets the post this content is for.
     *
     * This will automatically update the view of the content, and any bitmap and extras must be set
     * before this is called to provide the expected behaviour.
     *
     * @param redditPost The post to use in the view
     */
    public void setRedditPost(@Nullable RedditPost redditPost) {
        if (this.redditPost != null) {
            this.recycle();
        }

        this.redditPost = redditPost;
        if (redditPost != null) {
            if (redditPost.isNsfw()) {
                cache = settings.cacheNsfw();
            } else {
                // Always cache non-NSFW posts
                cache = true;
            }
            this.updateView();
        }
    }

    /**
     * Updates the view
     */
    protected abstract void updateView();


    /**
     * Called when the view has been selected
     */
    public void viewSelected() {
        // Default implementation of this is empty
    }

    /**
     * Called when the view has been unselected
     */
    public void viewUnselected() {
        // Default implementation of this is empty
    }


    /**
     * Gets the list of mappings from views to transition names for this view
     *
     * @return A list of {@link Pair} mapping a view to its transition name (this might be empty)
     */
    @NonNull
    public List<Pair<View, String>> getTransitionViews() {
        return new ArrayList<>();
    }

    /**
     * Gets the contents wanted height. This is
     *
     * @return The height the view wants to use, or {@link ViewGroup.LayoutParams#WRAP_CONTENT} if
     * it is not known
     */
    public int getWantedHeight() {
        // Using View.measure and measureHeight with WRAP_CONTENT as height doesn't work with some
        // of the subclasses, so instead this is an easy way of fixing the issue as they
        // can directly say what the value would be (if it is known to the class)
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    /**
     * Sets a bitmap on the content
     *
     * @param bitmap The bitmap to set
     */
    public void setBitmap(@Nullable Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * @return A bitmap representation of the view, if applicable. If a child class overrides this
     * method it is up to the child to determine what makes sense to return. Otherwise, the bitmap
     * passed to {@link #setBitmap(Bitmap)} is returned
     */
    @Nullable
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * @return The list of posts this view has previously displayed. Does not include the current post
     */
    @NonNull
    public List<RedditPost> getPreviousPosts() {
        return previousPosts;
    }

    /**
     * Recycles the content so that it can be reused with a new reddit post
     */
    @CallSuper
    protected void recycle() {
        // We do not want to use clear() here, as the bundle might be stored somewhere for reuse later
        extras = new Bundle();

        if (redditPost != null) {
            previousPosts.add(redditPost);
        }

        redditPost = null;
    }
}
