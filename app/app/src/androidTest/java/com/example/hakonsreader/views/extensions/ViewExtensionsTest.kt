package com.example.hakonsreader.views.extensions

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.views.util.goneIf
import com.example.hakonsreader.views.util.invisibleIf
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*


/**
 * Tests for view extension functions
 */
class ViewExtensionsTest {
    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }


    /**
     * Tests that a view sets the visibility correct when [goneIf] is used
     */
    @Test
    fun test_goneIf() {
        val view = TextView(context)

        // Should be VISIBLE by default
        assertEquals(View.VISIBLE, view.visibility)

        view.goneIf(true)
        // Should now be GONE
        assertEquals(View.GONE, view.visibility)

        view.goneIf(false)
        // Should now be VISIBLE
        assertEquals(View.VISIBLE, view.visibility)
    }

    /**
     * Tests that a view sets the visibility correct when [invisibleIf] is used
     */
    @Test
    fun test_invisibleIf() {
        val view = TextView(context)

        // Should be VISIBLE by default
        assertEquals(View.VISIBLE, view.visibility)

        view.invisibleIf(true)
        // Should now be INVISIBLE
        assertEquals(View.INVISIBLE, view.visibility)

        view.invisibleIf(false)
        // Should now be VISIBLE
        assertEquals(View.VISIBLE, view.visibility)
    }
}