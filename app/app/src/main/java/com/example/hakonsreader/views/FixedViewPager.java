package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * ViewPager that "fixes" the issue with PhotoView crashing the app when zooming out.
 * Sometimes when zooming out on a PhotoView in a ViewPager and letting go off one of the fingers
 * pinching it causes an issue with pointer index out of range. This class simply catches that
 * exception so the app doesn't crash
 *
 * <p>This is a known issue of PhotoView: https://github.com/chrisbanes/PhotoView/issues/4</p>
 */
public class FixedViewPager extends ViewPager {
    public FixedViewPager(@NonNull Context context) {
        this(context, null);
    }
    public FixedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
