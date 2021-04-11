package com.example.hakonsreader

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.robinhood.ticker.TickerView
import org.hamcrest.Description
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
 * Matcher for verifying if a view has a given text color.
 *
 * This matcher allows for any [View] to be passed. This is to allow to verify a [TickerView] as well
 * as [TextView], but it is an error to pass anything else and will always cause a fail
 *
 * @param color The color resource to verify
 */
fun hasTextColorWithTicker(@ColorRes color: Int) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("verifies that a View has a given text color")
    }

    override fun matchesSafely(item: View): Boolean {
        val resolvedColor = ContextCompat.getColor(item.context, color)
        return when (item) {
            is TickerView -> resolvedColor == item.textColor
            is TextView -> resolvedColor == item.currentTextColor
            else -> false
        }
    }
}

/**
 * Matcher for verifying that a [TickerView] has a given text set.
 */
fun tickerViewHasText(text: String) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("verifies that a TickerView has a given text")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item !is TickerView) {
            return false
        }
        return item.text == text
    }
}

/**
 * Verifies that an [ImageView] has a given [ColorFilter] set.
 *
 * This assumes the default color filter used by [ImageView.setColorFilter] by passing only a color is set
 *
 * @param color The color resource on the color filter
 */
fun imageHasColorInColorFilter(@ColorRes color: Int) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("verifies that an ImageView has a given color filter set")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item !is ImageView) {
            return false
        }

        val resolvedColor = ContextCompat.getColor(item.context, color)

        // Kind of bad as this relies on implementation details of it being a PorterDuffColorFilter
        // but dunno how else to get the color
        return item.colorFilter == PorterDuffColorFilter(resolvedColor, PorterDuff.Mode.SRC_ATOP)
    }
}
