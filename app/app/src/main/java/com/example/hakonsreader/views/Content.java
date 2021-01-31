package com.example.hakonsreader.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.RedditPost;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic superclass for a view displaying Reddit content
 */
public abstract class Content extends FrameLayout {

    /**
     * The key that should be used to send extras about posts across activities
     * The value stored with this key should be the {@link Bundle} retrieved with {@link Content#getExtras()}
     */
    public static final String EXTRAS = "extras";


    /**
     * The post the content is for
     */
    protected RedditPost redditPost;

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
        return extras;
    }

    /**
     * Sets the extras for the content
     *
     * @param extras The extras to set
     */
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
     * Sets the post this content is for and updates the view
     *
     * @param redditPost The post to use in the view
     */
    public void setRedditPost(@Nullable RedditPost redditPost) {
        this.redditPost = redditPost;
        if (redditPost != null) {
            cache = !(redditPost.isNsfw() && App.Companion.get().dontCacheNSFW());
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
}
