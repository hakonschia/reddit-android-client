package com.example.hakonsreader

import android.view.View
import android.widget.EditText
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


/**
 * Verifies that a text matches with the text in an EditText
 *
 * @param textToCheck The text to verify against the EditText
 */
fun editTextEqualTo(textToCheck: String) : TypeSafeMatcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("verifies the text in an EditText")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item !is EditText) {
                return false
            }

            return item.text.toString() == textToCheck
        }
    }
}

/**
 * Verifies the cursor position (selection) of an EditText
 *
 * @param start The start position of the cursor/selection
 * @param end Optional: The end position of the cursor, when the text has a selection
 */
fun cursorPosition(start: Int, end: Int = -1) : TypeSafeMatcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("verifies a cursor position on an EditText")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item !is EditText) {
                return false
            }

            val actualStart = item.selectionStart
            val actualEnd = item.selectionEnd

            if (start != actualStart) {
                return false
            }

            // If end is not specified don't check it (actualEnd will be the same as actualStart in this case)
            if (end != -1 && end != actualEnd) {
                return false
            }

            return true
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
            return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
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
