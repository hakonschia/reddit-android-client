package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.PostContentImage;
import com.example.hakonsreader.views.PostContentLink;
import com.example.hakonsreader.views.PostContentText;
import com.example.hakonsreader.views.PostContentVideo;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

public class Util {


    /**
     * Generates content view for a post
     *
     * @param post The post to generate for
     * @return A view with the content of the post
     */
    public static View generatePostContent(RedditPost post, Context context) {
        switch (post.getPostType()) {
            case IMAGE:
                return new PostContentImage(context, post);

            case VIDEO:
                return new PostContentVideo(context, post);

            case RICH_VIDEO:
                // Links such as youtube, gfycat etc are rich video posts
                return null;

            case LINK:
                return new PostContentLink(context, post);

            case TEXT:
                return new PostContentText(context, post);

            default:
                return null;
        }
    }


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
     * Cleans up post content by releasing any resources that might be used by the view
     *
     * @param postContent The view of the content
     */
    public static void cleanupPostContent(View postContent) {
        // Release the exo player from video posts
        if (postContent instanceof PostContentVideo) {
            ((PostContentVideo)postContent).release();
        }
    }


    /**
     * Taken from: https://stackoverflow.com/a/10187511/7750841
     *
     * Trims trailing whitespace. Removes any of these characters:
     * 0009, HORIZONTAL TABULATION
     * 000A, LINE FEED
     * 000B, VERTICAL TABULATION
     * 000C, FORM FEED
     * 000D, CARRIAGE RETURN
     * 001C, FILE SEPARATOR
     * 001D, GROUP SEPARATOR
     * 001E, RECORD SEPARATOR
     * 001F, UNIT SEPARATOR
     *
     * @return "" if source is null, otherwise string with all trailing whitespace removed
     */
    public static String trimTrailingWhitespace(CharSequence source) {

        if(source == null)
            return "";

        int i = source.length();

        // loop back to the first non-whitespace character
        while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {
        }

        return source.subSequence(0, i+1).toString();
    }
}
