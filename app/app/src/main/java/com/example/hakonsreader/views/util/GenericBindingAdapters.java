package com.example.hakonsreader.views.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.BindingAdapter;

/**
 * Generic BindingAdapters used with data binding
 */
public class GenericBindingAdapters {

    private GenericBindingAdapters() {}

    // TODO these dont actually work because there a ClassCastException since which type of layoutparams
    //  to get (ViewGroup/ConstriantLayout etc) are based on the parent view

    /**
     * Adapter to set "layout_marginBottom" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginBottom")
    public static void marginBottom(View view, float margin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(layoutParams);
        marginParams.bottomMargin = (int)margin;

        view.setLayoutParams(marginParams);
    }

    /**
     * Adapter to set "layout_marginTop" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginTop")
    public static void marginTop(View view, float margin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(layoutParams);
        marginParams.topMargin = (int)margin;

        view.setLayoutParams(marginParams);
    }
    /**
     * Adapter to set "layout_marginStart" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginStart")
    public static void marginStart(View view, float margin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(layoutParams);
        marginParams.setMarginStart((int)margin);

        view.setLayoutParams(marginParams);
    }
    /**
     * Adapter to set "layout_marginEnd" with the use of data binding
     *
     * @param view The view to set the margin on
     * @param margin The margin to set
     */
    @BindingAdapter("marginEnd")
    public static void marginEnd(View view, float margin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(layoutParams);
        marginParams.setMarginEnd((int)margin);

        view.setLayoutParams(marginParams);
    }
}
