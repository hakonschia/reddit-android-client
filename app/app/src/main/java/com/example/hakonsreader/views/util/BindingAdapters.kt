package com.example.hakonsreader.views.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.createAgeText
import com.example.hakonsreader.misc.createAgeTextShortened
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Contains the static binding adapters used for data binding that aren't specific to one view
 */

/**
 * Binding adapter for setting the age text on the post. The text is formatted as "2 hours", "1 day" etc.
 * For a shortened text (5m etc.) use [setAgeTextShortened]
 *
 * @param textView The textView to set the text on
 * @param createdAt The timestamp the post was created at. If this is negative nothing is done
 */
@BindingAdapter("createdAt")
fun setAgeText(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        val created = Instant.ofEpochSecond(createdAt)
        val now = Instant.now()
        val between = Duration.between(created, now)
        textView.text = createAgeText(textView.resources, between)
    }
}

/**
 * Binding adapter for setting a text for when something was created. The text is formatted as
 * "Created 1. january 1970"
 *
 * @param textView The textView to set the text on
 * @param createdAt The timestamp (seconds) the post was created at. If this is negative nothing is done
 */
@BindingAdapter("createdAtFullText")
fun setCreatedAtFullText(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        val format = SimpleDateFormat("d. MMMM y", Locale.getDefault())
        // Seconds is given, milliseconds is expected
        val date = Date(createdAt * 1000L)
        val full = textView.resources.getString(R.string.subredditInfoCreatedAt, format.format(date))
        textView.text = full
    }
}

/**
 * Binding adapter for setting the age text on the post. The text is formatted as "*2 hours", "*1 day" etc.
 * For a shortened text (*5m etc.) use [setAgeTextEditedShortened]
 *
 * @param textView The textView to set the text on
 * @param createdAt The timestamp the post was created at. If this is negative nothing is done
 */
@BindingAdapter("editedAt")
fun setAgeTextEdited(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        val created = Instant.ofEpochSecond(createdAt)
        val now = Instant.now()
        val between = Duration.between(created, now)
        textView.text = "*" + createAgeText(textView.resources, between)
    }
}

/**
 * Binding adapter for setting the age text on the post. The text is formatted as "2h", "1d" etc..
 * For a longer text (5 minutes etc.) use [setAgeText]
 *
 * @param textView The textView to set the text on
 * @param createdAt The timestamp the post was created at. If this is negative nothing is done
 */
@BindingAdapter("createdAtShortened")
fun setAgeTextShortened(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        val created = Instant.ofEpochSecond(createdAt)
        val now = Instant.now()
        val between = Duration.between(created, now)
        textView.text = createAgeTextShortened(textView.resources, between)
    }
}

/**
 * Binding adapter for setting the time when a post/comment was edited. The text is formatted as "*2h", "*1d" etc..
 * For a longer text (5 minutes etc.) use [setAgeTextEdited] (TextView, long)}
 *
 * @param textView The textView to set the text on
 * @param createdAt The timestamp the post was created at. If this is negative nothing is done
 */
@BindingAdapter("editedAtShortened")
fun setAgeTextEditedShortened(textView: TextView, createdAt: Long) {
    if (createdAt >= 0) {
        val created = Instant.ofEpochSecond(createdAt)
        val now = Instant.now()
        val between = Duration.between(created, now)
        textView.text = "*" + createAgeTextShortened(textView.resources, between)
    }
}

/**
 * Sets the text color of a TextView based on a reddit distinguish. By default this will use "text_color"
 *
 * @param textView The text view to set the color on
 * @param distinguish A string defining how the thing is distinguished. If this is null the default color is used
 */
@BindingAdapter("textColorFromRedditDistinguish")
fun setTextColorFromRedditDistinguish(textView: TextView, distinguish: String?) {
    val color = when (distinguish) {
        "admin" -> R.color.commentByAdminBackground
        "moderator" ->  R.color.commentByModBackground
        else -> R.color.text_color
    }
    textView.setTextColor(ContextCompat.getColor(textView.context, color))
}
