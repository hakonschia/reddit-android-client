package com.example.hakonsreader

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for testing custom views. Use [layout] to specify the layout file to inflate
 */
@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    companion object {
        @LayoutRes
        var layout: Int = 0
    }

    private lateinit var linearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (layout == 0) {
            linearLayout = LinearLayout(this)
            setContentView(linearLayout)
        } else {
            setContentView(layout)
        }
    }

    /**
     * Adds a view to the activity. The activity has a base layout of a [LinearLayout] which will only
     * be created if [layout] is not set. It is an error to call this if the layout was set with [layout]
     */
    fun <T : View> addView(clazz: Class<T>): T {
        // val view = TextView(context = this)
        val view: T = clazz.constructors.first().newInstance(this) as T
        linearLayout.addView(view)
        return view
    }

    /**
     * The same as [addView], but removes all other views first to ensure only this view is
     * allowed in the layout
     */
    fun <T : View> addOnlyOneView(clazz: Class<T>): T {
        linearLayout.removeAllViews()
        return addView(clazz)
    }
}