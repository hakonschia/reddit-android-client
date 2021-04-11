package com.example.hakonsreader

import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.google.android.material.R
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

/**
 * Performs a delay. Note this must be called with `onView(isRoot())`
 *
 * @param delay The amount of time in milliseconds to delay
 */
// Taken from: https://stackoverflow.com/a/59026058/7750841
fun waitFor(delay: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription(): String = "wait for $delay milliseconds"
        override fun perform(uiController: UiController, v: View?) {
            uiController.loopMainThreadForAtLeast(delay)
        }
    }
}

/**
 * Sets the cursor position for an EditText
 *
 * @param start The start selection
 * @param end Optional: The end selection
 */
// Taken from (with some modification): https://stackoverflow.com/a/63660118/7750841
fun setCursorPosition(start: Int, end: Int = -1) : ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return CoreMatchers.allOf(ViewMatchers.isDisplayed(), ViewMatchers.isAssignableFrom(EditText::class.java))
        }

        override fun getDescription(): String {
            return "set selection to start=$start, end=$end"
        }

        override fun perform(uiController: UiController, view: View) {
            if (end == -1) {
                (view as EditText).setSelection(start)
            } else {
                (view as EditText).setSelection(start, end)
            }
        }
    }
}
