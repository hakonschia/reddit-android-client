package com.example.hakonsreader.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

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
     * A bundle of extras. This will by default always be empty and it is up to subclasses
     * to provide data for this
     */
    protected Bundle extras;

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
    public Bundle getExtras() {
        if (extras == null) {
            extras = new Bundle();
        }
        return extras;
    }

    /**
     * Sets the extras for the content
     *
     * @param extras The extras to set
     */
    public void setExtras(Bundle extras) {
        this.extras = extras;
    }


    /**
     * @return The post this view is displaying
     */
    public RedditPost getRedditPost() {
        return redditPost;
    }

    /**
     * Sets the post this content is for and updates the view
     *
     * @param redditPost The post to use in the view
     */
    public void setRedditPost(RedditPost redditPost) {
        this.redditPost = redditPost;
        this.updateView();
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
}
