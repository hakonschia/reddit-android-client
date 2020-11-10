package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Layout that fixes the issue of having a RecyclerView inside a SwipeRefreshLayout inside a
 * MotionLayout messing up the scrolling animation of the MotionLayout
 *
 * Taken from: https://stackoverflow.com/a/61249168/7750841
 */
public class SwipeRefreshMotionLayout extends MotionLayout {
    public SwipeRefreshMotionLayout(@NonNull Context context) {
        super(context);
    }

    public SwipeRefreshMotionLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeRefreshMotionLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        // TODO if you switch from the fragment and go back this doesn't work anymore (it will expand again)

        if (!isInteractionEnabled()) {
            return;
        }

        // Check that the nested scroll is the swipe layout
        if (!(target instanceof SwipeRefreshLayout)) {
            super.onNestedPreScroll(target, dx, dy, consumed, type);
            return;
        }

        // Check that the child of the swipe layout is a RecyclerView
        View recyclerView = ((SwipeRefreshLayout) target).getChildAt(0);
        if (!(recyclerView instanceof RecyclerView)) {
            super.onNestedPreScroll(target, dx, dy, consumed, type);
            return;
        }

        // Scrolling up (dy < 0) and we can still scroll up, do nothing
        boolean canScrollVertically = recyclerView.canScrollVertically(-1);
        if (dy < 0 && canScrollVertically) {
            return;
        }

        super.onNestedPreScroll(target, dx, dy, consumed, type);
    }
}
