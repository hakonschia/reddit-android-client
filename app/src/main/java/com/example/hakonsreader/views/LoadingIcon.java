package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

public class LoadingIcon extends ProgressBar {
    private int itemsLoading;

    public LoadingIcon(Context context) {
        super(context);
    }

    public LoadingIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Increases the load counter. This will always enable the icon
     */
    public synchronized void increaseLoadCount() {
        this.itemsLoading++;
        setVisibility(View.VISIBLE);
    }

    /**
     * Decreases the load counter. If the load counter is now 0 the icon is hidden
     */
    public synchronized void decreaseLoadCount() {
        this.itemsLoading--;
        if (this.itemsLoading == 0) {
            setVisibility(View.GONE);
        }
    }

    public synchronized void setItemsLoading(int itemsLoading) {
        this.itemsLoading = itemsLoading;
        if (this.itemsLoading == 0) {
            setVisibility(View.GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }
}
