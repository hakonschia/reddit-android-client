package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Superclass of {@link ProgressBar} that will automatically show/hide the progress bar
 * when loading counts increase
 */
public class LoadingIcon extends ProgressBar {
    private static final String TAG = "LoadingIcon";
    
    private int itemsLoading;

    public LoadingIcon(Context context) {
        super(context);
    }
    public LoadingIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public LoadingIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public LoadingIcon(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Increases the load counter. This will always enable the icon
     */
    private synchronized void increaseLoadCount() {
        itemsLoading++;
        setVisibility(View.VISIBLE);
    }

    /**
     * Decreases the load counter. If the load counter is now 0 the icon is hidden
     */
    private synchronized void decreaseLoadCount() {
        itemsLoading--;

        // If this has been called too many times on accident it should not go below 0, as that would cause issues
        if (itemsLoading < 0) {
            itemsLoading = 0;
        }

        if (itemsLoading == 0) {
            setVisibility(View.GONE);
        }
    }

    /**
     * Sets the total items loading and shows/hides the progress bar accordingly
     *
     * @param itemsLoading The amount of items loading
     */
    public synchronized void setItemsLoading(int itemsLoading) {
        this.itemsLoading = itemsLoading;
        if (itemsLoading == 0) {
            setVisibility(View.GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    /**
     * Called when something has started or finished loading.
     *
     * Convenience method for either {@link #increaseLoadCount()} and {@link #decreaseLoadCount()}
     *
     * @param up True if something has started loading
     */
    public synchronized void onCountChange(boolean up) {
        if (up) {
            increaseLoadCount();
        } else {
            decreaseLoadCount();
        }
    }
}
