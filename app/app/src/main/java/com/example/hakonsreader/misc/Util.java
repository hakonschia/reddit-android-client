package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

public class Util {
    private Util() { }


    /**
     * Handles generic errors that are common for all API responses and shows a snackbar to the user
     *
     * @param parent The view to attach the snackbar to
     * @param code The code for the request
     * @param t Throwable from the request
     */
    public static void handleGenericResponseErrors(View parent, int code, Throwable t) {
        if (t instanceof IOException) {
            Util.showNoInternetSnackbar(parent);
        } else if (t instanceof InvalidAccessTokenException) {
            Util.showNotLoggedInSnackbar(parent);
        } else if (code == 403) {
            Util.showForbiddenErrorSnackbar(parent);
        } else if (code == 429) {
            Util.showTooManyRequestsSnackbar(parent);
        } else if (code >= 500) {
            Util.showGenericServerErrorSnackbar(parent);
        } else {
            Util.showUnknownError(parent);
        }
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
     * Creates and shows a snackbar for when an action was attempted that requires the user to be logged in
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showNotLoggedInSnackbar(View parent) {
        Snackbar snackbar = Snackbar.make(parent, parent.getResources().getString(R.string.notLoggedInError), Snackbar.LENGTH_LONG);
        Context context = parent.getContext();

        snackbar.setAction(context.getString(R.string.log_in), v -> {
            // TODO Redirect to a login activity/fragment
        });
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.accent));
        snackbar.show();
    }

    /**
     * Creates and shows a snackbar for errors caused by no internet connection
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showForbiddenErrorSnackbar(View parent) {
        // 403 errors are generally when the access token is outdated and new functionality has been
        // added that requires more OAuth scopes
        Snackbar.make(parent, parent.getResources().getString(R.string.forbiddenError), Snackbar.LENGTH_SHORT).show();
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
    public static void showUnknownError(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.unknownError), Snackbar.LENGTH_SHORT).show();
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
     * Create a duration string in the format of "mm:ss" that can be used in videos
     *
     * @param seconds The amount of seconds to display
     * @return A string formatted as "mm:ss"
     */
    public static String createVideoDuration(int seconds) {
        return String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60);
    }
}
