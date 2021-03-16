package com.example.hakonsreader.views.util

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter

/**
 * Generic BindingAdapters used with data binding
 */
/**
 * Adapter to set "layout_marginBottom" with the use of data binding
 *
 * @param view The view to set the margin on
 * @param margin The margin to set
 */
@BindingAdapter("marginBottom")
fun marginBottom(view: View, margin: Float) {
    (view.layoutParams as MarginLayoutParams).run {
        bottomMargin = margin.toInt()
        view.layoutParams = this
    }
}

/**
 * Adapter to set "layout_marginTop" with the use of data binding
 *
 * @param view The view to set the margin on
 * @param margin The margin to set
 */
@BindingAdapter("marginTop")
fun marginTop(view: View, margin: Float) {
    (view.layoutParams as MarginLayoutParams).run {
        topMargin = margin.toInt()
        view.layoutParams = this
    }
}

/**
 * Adapter to set "layout_marginStart" with the use of data binding
 *
 * @param view The view to set the margin on
 * @param margin The margin to set
 */
@BindingAdapter("marginStart")
fun marginStart(view: View, margin: Float) {
    (view.layoutParams as MarginLayoutParams).run {
        marginStart = margin.toInt()
        view.layoutParams = this
    }
}

/**
 * Adapter to set "layout_marginEnd" with the use of data binding
 *
 * @param view The view to set the margin on
 * @param margin The margin to set
 */
@BindingAdapter("marginEnd")
fun marginEnd(view: View, margin: Float) {
    (view.layoutParams as MarginLayoutParams).run {
        marginEnd = margin.toInt()
        view.layoutParams = this
    }
}

@BindingAdapter("layout_height")
fun layoutHeight(view: View, height: Float) {
    view.updateLayoutParams {
        this.height = height.toInt()
    }
}