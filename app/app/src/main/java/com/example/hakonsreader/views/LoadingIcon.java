package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

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

    public synchronized void setItemsLoading(int itemsLoading) {
        this.itemsLoading = itemsLoading;
        if (itemsLoading == 0) {
            setVisibility(View.GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    /**
     * Called when something has started or finished loading
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
