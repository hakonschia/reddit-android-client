package com.example.hakonsreader.views.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.BindingAdapter;

/**
 * Generic BindingAdapters used with data binding
 */
public class GenericBindingAdapters {

    private GenericBindingAdapters() {}

    /**
     * Adapter to set "layout_marginBottom" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginBottom")
    public static void marginBottom(View view, float margin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.bottomMargin = (int)margin;

        view.setLayoutParams(layoutParams);
    }

    /**
     * Adapter to set "layout_marginTop" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginTop")
    public static void marginTop(View view, float margin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.topMargin = (int)margin;

        view.setLayoutParams(layoutParams);
    }
    /**
     * Adapter to set "layout_marginStart" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginStart")
    public static void marginStart(View view, float margin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMarginStart((int)margin);

        view.setLayoutParams(layoutParams);
    }
    /**
     * Adapter to set "layout_marginEnd" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginEnd")
    public static void marginEnd(View view, float margin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMarginEnd((int)margin);

        view.setLayoutParams(layoutParams);
    }
}
