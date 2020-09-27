package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;
import android.view.View;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.ContentImage;
import com.example.hakonsreader.views.ContentLink;
import com.example.hakonsreader.views.ContentText;
import com.example.hakonsreader.views.ContentVideo;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

public class Util {
    private Util() { }


    /**
     * Handles generic errors that are common for all API responses.
     * <p>Handles too many requests, server, and no internet errors
     *
     * <p>Shows a snackbar with error information</p>
     *
     * @param parent The view to attach the snackbar to
     * @param code The code for the request
     * @param t Throwable from the request
     * @return True if the error is handled
     */
    public static boolean handleGenericResponseErrors(View parent, int code, Throwable t) {
        boolean handled = false;

        if (t instanceof IOException) {
            Util.showNoInternetSnackbar(parent);
            handled = true;
        } else if (code == 429) {
            Util.showTooManyRequestsSnackbar(parent);
            handled = true;
        } else if (code == 503) {
            Util.showGenericServerErrorSnackbar(parent);
            handled = true;
        }

        return handled;
    }

    /**
     * Creates and shows a snackbar for generic server errors
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showGenericServerErrorSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.genericServerError), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by too many requests sent
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showTooManyRequestsSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.tooManyRequestsError), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by too many requests sent
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showErrorLoggingInSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.errorLoggingIn), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by no internet connection
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showNoInternetSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.noInternetConnection), Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Creates the text for text age text fields
     * <p>Formats to make sure that it says 3 hours, 5 minutes etc. based on what makes sense</p>
     *
     * @param resources Resources to retrieve strings from
     * @param time The time to format as
     * @return The time formatted as a string
     */
    public static String createAgeText(Resources resources, Duration time) {
        String format;
        long t;

        if ((t = time.toDays()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeDays, (int) t);
        } else if ((t = time.toHours()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeHours, (int) t);
        } else {
            t = time.toMinutes();
            format = resources.getQuantityString(R.plurals.postAgeMinutes, (int) t);
        }

        return String.format(Locale.getDefault(), format, t);
    }

    /**
     * Removes whitespace from a {@link CharSequence}
     *
     * <p>Taken from: https://stackoverflow.com/a/16745540/7750841</p>
     *
     * @param s The sequence to trim
     * @param start Where in the sequence to start
     * @param end Where in the sequence to end
     * @return A trimmed {@link CharSequence}
     */
    public static CharSequence trim(CharSequence s, int start, int end) {
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }

        return s.subSequence(start, end);
    }

    /**
     * Converts an HTML string to a {@link CharSequence} that can be used in TextView's
     *
     * <p>The end of the CharSequence is trimmed so it doesn't include extra whitespace</p>
     *
     * @param html The HTML string to convert
     * @return A trimmed {@link CharSequence} of the input string
     */
    public static CharSequence fromHtml(String html) {
        Spanned s = Html.fromHtml(html, Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV | Html.FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE);
        return Util.trim(s, 0, s.length());
    }
}
